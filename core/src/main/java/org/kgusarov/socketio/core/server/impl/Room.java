package org.kgusarov.socketio.core.server.impl;

import org.kgusarov.socketio.core.common.SocketIOException;
import org.kgusarov.socketio.core.server.Outbound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.newSetFromMap;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public class Room implements Outbound {
    private static final Logger LOGGER = LoggerFactory.getLogger(Room.class);

    private final String id;
    private final Set<Socket> sockets = newSetFromMap(new ConcurrentHashMap<>());

    Room(final String id) {
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
                LOGGER.error("Failed to emit event in room", e);
            }
        }
    }

    public void join(final Socket socket) {
        sockets.add(socket);
    }

    public void leave(final Socket socket) {
        sockets.remove(socket);
    }

    public boolean contains(final Socket socket) {
        return sockets.contains(socket);
    }

    public void broadcast(final Socket sender, final String name, final Object... args) throws SocketIOException {
        for (final Socket socket : sockets) {
            if (!Objects.equals(socket, sender)) {
                socket.emit(name, args);
            }
        }
    }
}
