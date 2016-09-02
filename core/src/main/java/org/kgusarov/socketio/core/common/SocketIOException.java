package org.kgusarov.socketio.core.common;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public class SocketIOException extends Exception {
    private static final long serialVersionUID = 1L;

    public SocketIOException() {
    }

    public SocketIOException(final String message) {
        super(message);
    }

    public SocketIOException(final Throwable cause) {
        super(cause);
    }

    public SocketIOException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
