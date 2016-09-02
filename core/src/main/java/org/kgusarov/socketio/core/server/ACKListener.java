package org.kgusarov.socketio.core.server;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
@FunctionalInterface
public interface ACKListener {
    void onACK(Object[] args);
}
