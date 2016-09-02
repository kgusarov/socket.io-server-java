package org.kgusarov.socketio.core.server;

import org.kgusarov.socketio.core.server.impl.Socket;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
@FunctionalInterface
public interface ConnectionListener {
    /**
     * Callback to be called on new connection
     *
     * @param socket new socket
     * @throws org.kgusarov.socketio.core.server.ConnectionException thrown if caller want to fail the connection
     *                                                               for some reason (e.g. authentication error).
     *                                                               The error message will be sent to the client
     */
    void onConnect(Socket socket) throws ConnectionException;
}
