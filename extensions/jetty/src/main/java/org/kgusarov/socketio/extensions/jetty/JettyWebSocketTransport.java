package org.kgusarov.socketio.extensions.jetty;

import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.kgusarov.socketio.core.server.Config;
import org.kgusarov.socketio.core.server.TransportConnection;
import org.kgusarov.socketio.core.server.TransportType;
import org.kgusarov.socketio.core.server.impl.SocketIOManager;
import org.kgusarov.socketio.core.server.transport.AbstractTransport;
import org.kgusarov.socketio.core.server.transport.connection.AbstractTransportConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
@SuppressWarnings({"SerializableStoresNonSerializable", "NestedMethodCall"})
public final class JettyWebSocketTransport extends AbstractTransport {
    private static final Logger LOGGER = LoggerFactory.getLogger(JettyWebSocketTransport.class);

    private final WebSocketServerFactory wsFactory = new WebSocketServerFactory();

    @Override
    public void init(final ServletConfig config, final ServletContext context) throws ServletException {
        super.init(config, context);

        try {
            wsFactory.init();
        } catch (final Exception e) {
            throw new ServletException(e);
        }

        wsFactory.getPolicy().setMaxTextMessageSize(getConfig().getInt(Config.MAX_TEXT_MESSAGE_SIZE, 32000));
        wsFactory.getPolicy().setInputBufferSize(getConfig().getBufferSize());
        wsFactory.getPolicy().setIdleTimeout(getConfig().getMaxIdle());

        LOGGER.trace(getType() + " configuration:" + System.lineSeparator() +
                " - bufferSize=" + wsFactory.getPolicy().getInputBufferSize() + System.lineSeparator() +
                " - maxIdle=" + wsFactory.getPolicy().getIdleTimeout());
    }

    @Override
    public TransportType getType() {
        return TransportType.WEB_SOCKET;
    }

    @Override
    public void handle(final HttpServletRequest request,
                       final HttpServletResponse response,
                       final SocketIOManager sessionManager) throws IOException {

        if (!"GET".equals(request.getMethod())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    "Only GET method is allowed for websocket transport");
            return;
        }

        final TransportConnection connection = getConnection(request, sessionManager);

        // a bit hacky but safe since we know the type of TransportConnection here
        ((AbstractTransportConnection) connection).setRequest(request);
        wsFactory.acceptWebSocket((servletUpgradeRequest, servletUpgradeResponse) -> connection, request, response);
    }

    @Override
    public TransportConnection createConnection() {
        return new JettyWebSocketTransportConnection(this);
    }
}
