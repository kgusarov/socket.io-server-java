package org.kgusarov.socketio.extensions.jetty;

import com.google.common.io.ByteStreams;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.kgusarov.socketio.core.common.ConnectionState;
import org.kgusarov.socketio.core.common.DisconnectReason;
import org.kgusarov.socketio.core.common.SocketIOException;
import org.kgusarov.socketio.core.common.SocketIOProtocolException;
import org.kgusarov.socketio.core.protocol.BinaryPacket;
import org.kgusarov.socketio.core.protocol.EngineIOPacket;
import org.kgusarov.socketio.core.protocol.EngineIOProtocol;
import org.kgusarov.socketio.core.protocol.SocketIOPacket;
import org.kgusarov.socketio.core.server.Config;
import org.kgusarov.socketio.core.server.SocketIOClosedException;
import org.kgusarov.socketio.core.server.Transport;
import org.kgusarov.socketio.core.server.transport.connection.AbstractTransportConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;

import static org.kgusarov.socketio.core.protocol.EngineIOProtocol.createHandshakePacket;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
@WebSocket
@SuppressWarnings("NestedMethodCall")
public final class JettyWebSocketTransportConnection extends AbstractTransportConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(JettyWebSocketTransportConnection.class);

    private Session remoteEndpoint;

    public JettyWebSocketTransportConnection(final Transport transport) {
        super(transport);
    }

    @Override
    public void init() {
        LOGGER.trace(getConfig().getNamespace() + " WebSocket configuration:" + " timeout=" + getSession().getTimeout());
    }

    @OnWebSocketConnect
    public void onWebSocketConnect(final Session session) {
        remoteEndpoint = session;

        if (getSession().getConnectionState() == ConnectionState.CONNECTING) {
            try {
                final String[] upgrades = {};
                final long pingInterval = getConfig().getPingInterval(Config.DEFAULT_PING_INTERVAL);
                final long pingTimeout = getConfig().getTimeout(Config.DEFAULT_PING_TIMEOUT);
                final String sessionId = getSession().getSessionId();

                final EngineIOPacket packet = createHandshakePacket(sessionId, upgrades, pingInterval, pingTimeout);
                send(packet);

                getSession().onConnect(this);
            } catch (final SocketIOException e) {
                LOGGER.error("Connection failed", e);
                getSession().setDisconnectReason(DisconnectReason.CONNECT_FAILED);
                abort();
            }
        }
    }

    @OnWebSocketClose
    public void onWebSocketClose(final int closeCode, final String message) {
        LOGGER.trace("Session[" + getSession().getSessionId() + "]:" + " websocket closed. Close code: "
                + closeCode + " message: " + message);

        //If close is unexpected then try to guess the reason based on closeCode, otherwise the reason is already set
        if (getSession().getConnectionState() != ConnectionState.CLOSING) {
            getSession().setDisconnectReason(fromCloseCode(closeCode));
        }

        getSession().setDisconnectMessage(message);
        getSession().onShutdown();
    }

    @OnWebSocketMessage
    public void onWebSocketText(final String text) {
        LOGGER.trace("Session[" + getSession().getSessionId() + "]: text received: " + text);
        getSession().resetTimeout();

        try {
            getSession().onPacket(EngineIOProtocol.decode(text), this);
        } catch (final SocketIOProtocolException e) {
            LOGGER.warn("Invalid packet received", e);
        }
    }

    @OnWebSocketMessage
    public void onWebSocketBinary(final InputStream is) {
        LOGGER.trace("Session[" + getSession().getSessionId() + "]: binary received");
        getSession().resetTimeout();

        try {
            getSession().onPacket(EngineIOProtocol.decode(is), this);
        } catch (final SocketIOProtocolException e) {
            LOGGER.warn("Problem processing binary received", e);
        }
    }

    @Override
    public void handle(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unexpected request on upgraded WebSocket connection");
    }

    @Override
    public void abort() {
        getSession().clearTimeout();
        if (remoteEndpoint != null) {
            disconnectEndpoint();
            remoteEndpoint = null;
        }
    }

    @Override
    public void send(final EngineIOPacket packet) throws SocketIOException {
        sendString(EngineIOProtocol.encode(packet));
    }

    @Override
    public void send(final SocketIOPacket packet) throws SocketIOException {
        send(EngineIOProtocol.createMessagePacket(packet.encode()));
        if (packet instanceof BinaryPacket) {
            final Collection<InputStream> attachments = ((BinaryPacket) packet).getAttachments();
            for (final InputStream is : attachments) {

                final ByteArrayOutputStream os = new ByteArrayOutputStream();
                try {
                    os.write(EngineIOPacket.Type.MESSAGE.value());
                    ByteStreams.copy(is, os);
                } catch (final IOException e) {
                    LOGGER.warn("Cannot load binary object to send it to the socket", e);
                }

                sendBinary(os.toByteArray());
            }
        }
    }

    private void sendString(final String data) throws SocketIOException {
        if (!remoteEndpoint.isOpen()) {
            throw new SocketIOClosedException();
        }

        LOGGER.trace("Session[" + getSession().getSessionId() + "]: send text: " + data);

        try {
            remoteEndpoint.getRemote().sendString(data);
        } catch (final IOException e) {
            disconnectEndpoint();
            throw new SocketIOException(e);
        }
    }

    //TODO: implement streaming. right now it is all in memory.
    //TODO: read and send in chunks using sendPartialBytes()
    private void sendBinary(final byte[] data) throws SocketIOException {
        if (!remoteEndpoint.isOpen()) {
            throw new SocketIOClosedException();
        }

        LOGGER.trace("Session[" + getSession().getSessionId() + "]: send binary");

        try {
            remoteEndpoint.getRemote().sendBytes(ByteBuffer.wrap(data));
        } catch (final IOException e) {
            disconnectEndpoint();
            throw new SocketIOException(e);
        }
    }

    private void disconnectEndpoint() {
        try {
            remoteEndpoint.disconnect();
        } catch (final IOException ex) {
            LOGGER.warn("Endpoint disconnection failed", ex);
        }
    }

    private DisconnectReason fromCloseCode(final int code) {
        switch (code) {
            case StatusCode.SHUTDOWN:
                return DisconnectReason.CLIENT_GONE;
            default:
                return DisconnectReason.ERROR;
        }
    }
}
