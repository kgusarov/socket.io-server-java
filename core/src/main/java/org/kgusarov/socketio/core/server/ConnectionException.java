package org.kgusarov.socketio.core.server;

import org.kgusarov.socketio.core.common.SocketIOException;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
@SuppressWarnings("NonSerializableFieldInSerializableClass")
public class ConnectionException extends SocketIOException {
    private final Object args;

    public Object getArgs() {
        return args;
    }

    public ConnectionException(final String message, final Throwable cause, final Object args) {
        super(message, cause);
        this.args = args;
    }

    public ConnectionException(final Throwable cause, final Object args) {
        super(cause);
        this.args = args;
    }

    public ConnectionException(final Object args) {
        this.args = args;
    }
}
