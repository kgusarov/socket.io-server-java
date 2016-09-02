package org.kgusarov.socketio.core.server;

import org.kgusarov.socketio.core.common.SocketIOException;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
@FunctionalInterface
public interface Outbound {
    /**
     * Emits an event to the socket identified by the string name.
     *
     * @param name event name
     * @param args list of arguments. Arguments can contain any type of field that can result of JSON decoding,
     *             including objects and arrays of arbitrary size. If last argument is {@code ACKListener}
     *             then this listener to be called upon ACK arriving
     * @throws SocketIOException if IO or protocol error happens
     */
    void emit(String name, Object... args) throws SocketIOException;
}
