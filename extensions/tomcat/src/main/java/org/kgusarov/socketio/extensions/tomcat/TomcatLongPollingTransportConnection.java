package org.kgusarov.socketio.extensions.tomcat;

import org.kgusarov.socketio.core.server.Transport;
import org.kgusarov.socketio.core.server.transport.connection.XHRTransportConnection;

/**
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
@SuppressWarnings("NestedMethodCall")
public final class TomcatLongPollingTransportConnection extends XHRTransportConnection {
    public TomcatLongPollingTransportConnection(final Transport transport) {
        super(transport);
    }

    @Override
    public void abort() {
        getSession().onShutdown();
        setDone(true);
    }
}
