package org.kgusarov.socketio.core.server.transport;

import org.kgusarov.socketio.core.common.SocketIOProtocolException;
import org.kgusarov.socketio.core.protocol.EngineIOProtocol;
import org.kgusarov.socketio.core.server.Transport;
import org.kgusarov.socketio.core.server.TransportProvider;
import org.kgusarov.socketio.core.server.TransportType;
import org.kgusarov.socketio.core.server.UnsupportedTransportException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public abstract class AbstractTransportProvider implements TransportProvider {
    private final Map<TransportType, Transport> transports = new EnumMap<>(TransportType.class);

    /**
     * Creates and initializes all available transports
     */
    @Override
    @SuppressWarnings("NestedMethodCall")
    public void init(final ServletConfig config, final ServletContext context) throws ServletException {

        addIfNotNull(TransportType.XHR_POLLING, createXHRPollingTransport());
        addIfNotNull(TransportType.JSONP_POLLING, createJSONPPollingTransport());
        addIfNotNull(TransportType.WEB_SOCKET, createWebSocketTransport());

        for (final Transport t : transports.values()) {
            t.init(config, context);
        }
    }

    @Override
    public void destroy() {
        getTransports().forEach(Transport::destroy);
    }

    @Override
    public Transport getTransport(final ServletRequest request) throws UnsupportedTransportException, SocketIOProtocolException {
        final String transportName = request.getParameter(EngineIOProtocol.TRANSPORT);
        if (transportName == null) {
            throw new SocketIOProtocolException("Missing transport parameter");
        }

        TransportType type = TransportType.UNKNOWN;

        if ("websocket".equals(transportName)) {
            type = TransportType.decode(transportName);
        }

        if ("polling".equals(transportName)) {
            type = request.getParameter(EngineIOProtocol.JSONP_INDEX) != null ?
                    TransportType.JSONP_POLLING :
                    TransportType.XHR_POLLING;
        }

        final Transport t = transports.get(type);
        if (t == null) {
            throw new UnsupportedTransportException(transportName);
        }

        return t;
    }

    @Override
    public Transport getTransport(final TransportType type) {
        return transports.get(type);
    }

    @Override
    public Collection<Transport> getTransports() {
        return transports.values();
    }

    protected Transport createXHRPollingTransport() {
        return new XHRPollingTransport();
    }

    protected Transport createJSONPPollingTransport() {
        return null;
    }

    protected Transport createWebSocketTransport() {
        return null;
    }

    private void addIfNotNull(final TransportType type, final Transport transport) {
        if (transport != null) {
            transports.put(type, transport);
        }
    }
}
