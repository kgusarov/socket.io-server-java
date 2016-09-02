package org.kgusarov.socketio.core.server.transport.connection;

import org.kgusarov.socketio.core.common.ConnectionState;
import org.kgusarov.socketio.core.common.DisconnectReason;
import org.kgusarov.socketio.core.common.SocketIOException;
import org.kgusarov.socketio.core.protocol.SocketIOPacket;
import org.kgusarov.socketio.core.protocol.SocketIOProtocol;
import org.kgusarov.socketio.core.server.*;
import org.kgusarov.socketio.core.server.impl.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * @author Mathieu Carbou
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public abstract class AbstractTransportConnection implements TransportConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTransportConnection.class);

    private Config config;
    private Session session;
    private final Transport transport;
    private HttpServletRequest request;

    protected AbstractTransportConnection(final Transport transport) {
        this.transport = transport;
    }

    @Override
    public final void init(final Config config) {
        this.config = config;
        init();
    }

    @Override
    public Transport getTransport() {
        return transport;
    }

    @Override
    public void setSession(final Session session) {
        this.session = session;
    }

    protected final Config getConfig() {
        return config;
    }

    @Override
    public Session getSession() {
        return session;
    }

    protected void init() {
        final long timeout = getConfig().getTimeout(Config.DEFAULT_PING_TIMEOUT);
        getSession().setTimeout(timeout);
    }

    @Override
    public void disconnect(final String namespace, final boolean closeConnection) {
        try {
            send(SocketIOProtocol.createDisconnectPacket(namespace));
            getSession().setDisconnectReason(DisconnectReason.DISCONNECT);
        } catch (final SocketIOException e) {
            LOGGER.warn("Exception during disconnect", e);
            getSession().setDisconnectReason(DisconnectReason.CLOSE_FAILED);
        }

        if (closeConnection) {
            abort();
        }
    }

    @Override
    public void emit(final String namespace, final String name, Object... args) throws SocketIOException {
        if (getSession().getConnectionState() != ConnectionState.CONNECTED) {
            throw new SocketIOClosedException();
        }

        ACKListener ackListener = null;
        final int l = args.length;

        if ((l > 0) && (args[l - 1] instanceof ACKListener)) {
            ackListener = (ACKListener) args[l - 1];
            args = Arrays.copyOfRange(args, 0, l - 1);
        }

        int packetId = -1;
        if (ackListener != null) {
            packetId = getSession().getNewPacketId();
        }

        final SocketIOPacket packet = SocketIOProtocol.createEventPacket(packetId, namespace, name, args);
        if (packetId >= 0) {
            getSession().subscribeACK(packetId, ackListener);
        }

        send(packet);
    }

    @Override
    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(final HttpServletRequest request) {
        this.request = request;
    }
}
