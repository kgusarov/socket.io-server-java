package org.kgusarov.socketio.core.server;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
@FunctionalInterface
public interface EventListener {
    /**
     * Called upon incoming event
     *
     * @param ackRequested true if client requested an acknowledgement
     * @param name         event name
     * @param args         event arguments
     * @return Object to send back to the caller as an acknowledgement, null if no ack to be sent
     */
    Object onEvent(String name, Object[] args, boolean ackRequested);
}
