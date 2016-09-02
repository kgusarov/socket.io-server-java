package org.kgusarov.socketio.core.server.transport;

import org.kgusarov.socketio.core.server.TransportConnection;
import org.kgusarov.socketio.core.server.TransportType;
import org.kgusarov.socketio.core.server.transport.connection.XHRTransportConnection;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public class XHRPollingTransport extends AbstractHttpTransport {
    @Override
    public TransportType getType() {
        return TransportType.XHR_POLLING;
    }

    @Override
    public TransportConnection createConnection() {
        return new XHRTransportConnection(this);
    }
}
