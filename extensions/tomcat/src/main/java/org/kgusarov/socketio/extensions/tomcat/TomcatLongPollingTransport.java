package org.kgusarov.socketio.extensions.tomcat;

import org.kgusarov.socketio.core.server.TransportConnection;
import org.kgusarov.socketio.core.server.transport.XHRPollingTransport;

/**
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public final class TomcatLongPollingTransport extends XHRPollingTransport {
    @Override
    public TransportConnection createConnection() {
        return new TomcatLongPollingTransportConnection(this);
    }
}
