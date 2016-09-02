package org.kgusarov.socketio.core.server;

import org.kgusarov.socketio.core.common.SocketIOException;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public class SocketIOClosedException extends SocketIOException {
    private static final long serialVersionUID = 1L;

    public SocketIOClosedException() {
    }

    public SocketIOClosedException(final String message) {
        super(message);
    }

    public SocketIOClosedException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public SocketIOClosedException(final Throwable cause) {
        super(cause);
    }
}
