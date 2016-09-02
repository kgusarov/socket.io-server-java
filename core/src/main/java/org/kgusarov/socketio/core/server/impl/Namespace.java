/**
 * The MIT License
 * Copyright (c) 2015 Alexander Sova (bird@codeminders.com)
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.kgusarov.socketio.core.server.impl;

import org.kgusarov.socketio.core.common.DisconnectReason;
import org.kgusarov.socketio.core.common.SocketIOException;
import org.kgusarov.socketio.core.server.ConnectionException;
import org.kgusarov.socketio.core.server.ConnectionListener;
import org.kgusarov.socketio.core.server.DisconnectListener;
import org.kgusarov.socketio.core.server.Outbound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.newSetFromMap;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public class Namespace implements Outbound, ConnectionListener, DisconnectListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(Namespace.class);

    private final String id;

    private final Set<Socket> sockets = newSetFromMap(new ConcurrentHashMap<>());
    private final Set<ConnectionListener> connectionListeners = newSetFromMap(new ConcurrentHashMap<>());
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    Namespace(final String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public void emit(final String name, final Object... args) throws SocketIOException {
        for (final Socket s : sockets) {
            try {
                s.emit(name, args);
            } catch (final SocketIOException e) {
                LOGGER.error("Failed to emit event in namespace", e);
            }
        }
    }

    public void on(final ConnectionListener listener) {
        connectionListeners.add(listener);
    }

    @Override
    public void onConnect(final Socket socket) throws ConnectionException {
        for (final ConnectionListener listener : connectionListeners) {
            listener.onConnect(socket);
        }
    }

    public Socket createSocket(final Session session) {
        final Socket socket = new Socket(session, this);
        socket.on(this);
        sockets.add(socket);

        return socket;
    }

    @Override
    public void onDisconnect(final Socket socket, final DisconnectReason reason, final String errorMessage) {
        leaveAll(socket);
        sockets.remove(socket);
    }

    /**
     * Finds or creates a room.
     *
     * @param roomId room id
     * @return Room object
     */
    public Room room(final String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            room = new Room(roomId);

            final Room prev = rooms.putIfAbsent(roomId, room);
            room = prev == null ? room : prev;
        }

        return room;
    }

    /**
     * Finds or creates a room.
     *
     * @param roomId room id
     * @return Room object
     */
    public Room in(final String roomId) {
        return room(roomId);
    }

    @SuppressWarnings({"NestedMethodCall", "SerializableStoresNonSerializable"})
    void leaveAll(final Socket socket) {
        rooms.values().stream()
                .filter(room -> room.contains(socket))
                .forEach(room -> room.leave(socket));
    }
}
