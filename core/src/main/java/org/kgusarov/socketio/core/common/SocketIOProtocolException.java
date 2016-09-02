package org.kgusarov.socketio.core.common;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public class SocketIOProtocolException extends SocketIOException {

    public SocketIOProtocolException(final String message) {
        super(message);
    }

    public SocketIOProtocolException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
