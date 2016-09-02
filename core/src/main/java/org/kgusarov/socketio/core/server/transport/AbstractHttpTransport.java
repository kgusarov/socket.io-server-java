package org.kgusarov.socketio.core.server.transport;

import org.kgusarov.socketio.core.common.ConnectionState;
import org.kgusarov.socketio.core.common.SocketIOException;
import org.kgusarov.socketio.core.server.Config;
import org.kgusarov.socketio.core.server.TransportConnection;
import org.kgusarov.socketio.core.server.TransportType;
import org.kgusarov.socketio.core.server.impl.Session;
import org.kgusarov.socketio.core.server.impl.SocketIOManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.kgusarov.socketio.core.protocol.EngineIOProtocol.createHandshakePacket;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public abstract class AbstractHttpTransport extends AbstractTransport {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractHttpTransport.class);

    @Override
    public void handle(final HttpServletRequest request, final HttpServletResponse response,
                       final SocketIOManager socketIOManager) throws IOException {

        final String requestMethod = request.getMethod();
        final String name = getClass().getName();
        LOGGER.trace("Handling {} request by {}", requestMethod, name);

        final TransportConnection connection = getConnection(request, socketIOManager);
        final Session session = connection.getSession();

        if (session.getConnectionState() == ConnectionState.CONNECTING) {
            final List<String> upgrades = new ArrayList<>();
            if (socketIOManager.getTransportProvider().getTransport(TransportType.WEB_SOCKET) != null) {
                final String upgrade = TransportType.WEB_SOCKET.toString();
                upgrades.add(upgrade);
            }

            try {
                handleConnect(request, response, connection, session, upgrades);
            } catch (final SocketIOException e) {
                LOGGER.warn("Failed to handle connect", e);
            }
        } else if (session.getConnectionState() == ConnectionState.CONNECTED) {
            connection.handle(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_GONE, "Socket.IO session is closed");
        }
    }

    private void handleConnect(final HttpServletRequest request, final HttpServletResponse response, final TransportConnection connection,
                               final Session session, final List<String> upgrades) throws SocketIOException, IOException {

        final int upgradeCount = upgrades.size();
        final String[] upgradeList = upgrades.toArray(new String[upgradeCount]);
        final long pingInterval = getConfig().getPingInterval(Config.DEFAULT_PING_INTERVAL);
        final long pingTimeout = getConfig().getTimeout(Config.DEFAULT_PING_TIMEOUT);

        connection.send(createHandshakePacket(session.getSessionId(), upgradeList, pingInterval, pingTimeout));

        // called to send the handshake packet
        connection.handle(request, response);
        session.onConnect(connection);
    }
}
