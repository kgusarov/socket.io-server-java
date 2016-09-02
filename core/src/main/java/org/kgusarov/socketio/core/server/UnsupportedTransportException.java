package org.kgusarov.socketio.core.server;

import org.kgusarov.socketio.core.common.SocketIOException;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public class UnsupportedTransportException extends SocketIOException {
    public UnsupportedTransportException(final String name) {
        super("Unsupported transport " + name);
    }
}
