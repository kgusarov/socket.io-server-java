package org.kgusarov.socketio.extensions.tomcat;

import org.kgusarov.socketio.core.server.Transport;
import org.kgusarov.socketio.core.server.transport.AbstractTransportProvider;

/**
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public class TomcatTransportProvider extends AbstractTransportProvider {
    @Override
    protected Transport createXHRPollingTransport() {
        return new TomcatLongPollingTransport();
    }
}
