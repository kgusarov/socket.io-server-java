package org.kgusarov.socketio.core.server;

import org.kgusarov.socketio.core.common.SocketIOException;
import org.kgusarov.socketio.core.protocol.EngineIOPacket;
import org.kgusarov.socketio.core.protocol.SocketIOPacket;
import org.kgusarov.socketio.core.server.impl.Session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Mathieu Carbou
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public interface TransportConnection {
    void init(Config config);

    void setSession(Session session);

    Session getSession();

    Transport getTransport();

    void handle(HttpServletRequest request, HttpServletResponse response) throws IOException;

    /**
     * Tears down the connection.
     */
    void abort();

    void send(EngineIOPacket packet) throws SocketIOException;

    void send(SocketIOPacket packet) throws SocketIOException;

    void disconnect(String namespace, boolean closeConnection);

    /**
     * Emits an event to the socket identified by the string name.
     *
     * @param namespace namespace
     * @param name      event name
     * @param args      list of arguments. Arguments can contain any type of field that can result of JSON decoding,
     *                  including objects and arrays of arbitrary size.
     * @throws SocketIOException if IO or protocol error happens
     */

    void emit(String namespace, String name, Object... args) throws SocketIOException;

    /**
     * @return current HTTP request, null if connection is disconnected
     */
    HttpServletRequest getRequest();
}
