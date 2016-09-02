package org.kgusarov.socketio.extensions.jetty;

import org.kgusarov.socketio.core.server.Transport;
import org.kgusarov.socketio.core.server.transport.AbstractTransportProvider;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public class JettyTransportProvider extends AbstractTransportProvider {
    @Override
    protected Transport createWebSocketTransport() {
        return new JettyWebSocketTransport();
    }
}
