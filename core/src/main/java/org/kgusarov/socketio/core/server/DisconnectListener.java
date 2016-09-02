package org.kgusarov.socketio.core.server;

import org.kgusarov.socketio.core.common.DisconnectReason;
import org.kgusarov.socketio.core.server.impl.Socket;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
@FunctionalInterface
public interface DisconnectListener {
    void onDisconnect(Socket socket, DisconnectReason reason, String errorMessage);
}
