package org.kgusarov.socketio.core.server.impl;

import org.kgusarov.socketio.core.common.ConnectionState;
import org.kgusarov.socketio.core.common.DisconnectReason;
import org.kgusarov.socketio.core.common.SocketIOException;
import org.kgusarov.socketio.core.common.SocketIOProtocolException;
import org.kgusarov.socketio.core.protocol.BinaryPacket;
import org.kgusarov.socketio.core.protocol.EngineIOPacket;
import org.kgusarov.socketio.core.protocol.SocketIOPacket;
import org.kgusarov.socketio.core.protocol.packets.ACKPacket;
import org.kgusarov.socketio.core.protocol.packets.EventPacket;
import org.kgusarov.socketio.core.server.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.kgusarov.socketio.core.protocol.EngineIOProtocol.createNoopPacket;
import static org.kgusarov.socketio.core.protocol.EngineIOProtocol.createPongPacket;
import static org.kgusarov.socketio.core.protocol.SocketIOProtocol.*;

/**
 * SocketIO session.
 * <p/>
 *
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
@SuppressWarnings("NestedMethodCall")
public class Session implements DisconnectListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(Session.class);

    private final SocketIOManager socketIOManager;
    private final String sessionId;
    private final int ackTimeout;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    // Namespace -> Socket
    private final Map<String, Socket> sockets = new ConcurrentHashMap<>();

    private final AtomicReference<TransportConnection> activeConnection = new AtomicReference<>(null);
    private final AtomicReference<ConnectionState> state = new AtomicReference<>(ConnectionState.CONNECTING);

    private final AtomicReference<DisconnectReason> disconnectReason = new AtomicReference<>(DisconnectReason.UNKNOWN);
    private final AtomicReference<String> disconnectMessage = new AtomicReference<>(null);

    private final AtomicLong timeout = new AtomicLong(0L);
    private final AtomicReference<Future<?>> timeoutTask = new AtomicReference<>(null);
    private final AtomicBoolean timedOut = new AtomicBoolean(false);

    private final AtomicReference<BinaryPacket> binaryPacket = new AtomicReference<>(null);
    private final AtomicInteger packetId = new AtomicInteger(0);

    // Packet ID -> listener
    private final Map<Integer, ACKListener> ackListeners = new ConcurrentHashMap<>();

    Session(final SocketIOManager socketIOManager, final String sessionId, final int ackTimeout) {
        this.socketIOManager = socketIOManager;
        this.sessionId = sessionId;
        this.ackTimeout = ackTimeout;
    }

    public Socket createSocket(final String ns) {
        final Namespace namespace = socketIOManager.getNamespace(ns);
        if (namespace == null) {
            throw new IllegalArgumentException("Namespace does not exist");
        }

        final Socket socket = namespace.createSocket(this);
        socket.on(this);
        sockets.put(ns, socket);

        return socket;
    }

    public void setAttribute(final String key, final Object val) {
        attributes.put(key, val);
    }

    public Object getAttribute(final String key) {
        return attributes.get(key);
    }

    public String getSessionId() {
        return sessionId;
    }

    public ConnectionState getConnectionState() {
        return state.get();
    }

    public TransportConnection getConnection() {
        return activeConnection.get();
    }

    public void resetTimeout() {
        clearTimeout();
        final long toValue = timeout.get();

        if (timedOut.get() || (toValue == 0L)) {
            return;
        }

        final ScheduledFuture<?> future = socketIOManager.executor.schedule(this::onTimeout, toValue, TimeUnit.MILLISECONDS);
        timeoutTask.set(future);
    }

    public void clearTimeout() {
        final Future<?> future = timeoutTask.get();
        if (future != null) {
            future.cancel(false);
        }

        timeoutTask.set(null);
    }

    public void setTimeout(final long timeout) {
        this.timeout.set(timeout);
    }

    public long getTimeout() {
        return timeout.get();
    }

    private void onBinary(final InputStream is) throws SocketIOProtocolException {
        final BinaryPacket bp = binaryPacket.get();
        if (bp == null) {
            throw new SocketIOProtocolException("Unexpected binary object");
        }

        insertBinaryObject(bp, is);

        //keeping copy of all attachments in attachments list
        bp.addAttachment(is);

        if (bp.isComplete()) {
            if (bp.getType() == SocketIOPacket.Type.BINARY_EVENT) {
                onEvent((EventPacket) bp);
            } else if (bp.getType() == SocketIOPacket.Type.BINARY_ACK) {
                onACK((ACKPacket) bp);
            }

            binaryPacket.set(null);
        }
    }

    public void onConnect(final TransportConnection connection) throws SocketIOException {
        final boolean success = activeConnection.compareAndSet(null, connection);
        if (!success) {
            throw new SocketIOException("Connection is already set for this session");
        }

        final Socket socket = createSocket(DEFAULT_NAMESPACE);

        try {
            connection.send(createConnectPacket(DEFAULT_NAMESPACE));
            state.set(ConnectionState.CONNECTED);
            socketIOManager.getNamespace(DEFAULT_NAMESPACE).onConnect(socket);
        } catch (final ConnectionException e) {
            LOGGER.warn("Connection failed", e);
            connection.send(createErrorPacket(DEFAULT_NAMESPACE, e.getArgs()));
            closeConnection(DisconnectReason.CONNECT_FAILED, connection);
        }
    }

    /**
     * Optional. if transport knows detailed error message it could be set before calling onShutdown()
     *
     * @param message detailed explanation of the disconnect reason
     */
    public void setDisconnectMessage(final String message) {
        disconnectMessage.set(message);
    }

    /**
     * Calling this method will change activeConnection status to CLOSING
     *
     * @param reason session disconnect reason
     */
    public void setDisconnectReason(final DisconnectReason reason) {
        state.set(ConnectionState.CLOSING);
        disconnectReason.set(reason);
    }

    /**
     * callback to be called by transport activeConnection socket is closed.
     */
    public void onShutdown() {
        final ConnectionState connectionState = getConnectionState();
        if (connectionState == ConnectionState.CLOSING) {
            final DisconnectReason reason = disconnectReason.get();
            onDisconnect(reason);
        } else {
            onDisconnect(DisconnectReason.ERROR);
        }
    }

    /**
     * Disconnect callback. to be called by session itself. Transport activeConnection should always call onShutdown()
     */
    private void onDisconnect(final DisconnectReason reason) {
        final String errorMessage = disconnectMessage.get();
        final Object[] args = {sessionId, reason, errorMessage};
        LOGGER.trace("Session[{}]: onDisconnect: {} mesage: []", args);

        final ConnectionState connectionState = getConnectionState();
        if (connectionState == ConnectionState.CLOSED) {
            // to prevent calling it twice
            return;
        }

        state.set(ConnectionState.CLOSED);

        clearTimeout();

        // taking copy of sockets because
        // session will be modifying the collection while iterating
        for (final Object o : sockets.values().toArray()) {
            final Socket socket = (Socket) o;
            socket.onDisconnect(socket, reason, errorMessage);
        }

        socketIOManager.deleteSession(sessionId);
    }

    private void onTimeout() {
        LOGGER.trace("Session[{}]: onTimeout", sessionId);
        if (!timedOut.get()) {
            timedOut.set(true);

            final TransportConnection connection = activeConnection.get();
            closeConnection(DisconnectReason.TIMEOUT, connection);
        }
    }

    public void onPacket(final EngineIOPacket packet, final TransportConnection connection) {
        switch (packet.getType()) {
            case OPEN:
            case PONG:
                // ignore. OPEN and PONG are server -> client only
                return;

            case MESSAGE:
                resetTimeout();

                try {
                    if (packet.getTextData() != null) {
                        onPacket(decode(packet.getTextData()));
                    } else if (packet.getBinaryData() != null) {
                        onBinary(packet.getBinaryData());
                    }
                } catch (final SocketIOProtocolException e) {
                    LOGGER.warn("Invalid SIO packet: " + packet.getTextData(), e);
                }
                return;

            case PING:
                resetTimeout();
                onPing(packet.getTextData(), connection);

                // ugly hack to replicate current sio client behaviour
                final TransportConnection currentConnection = getConnection();
                if (!Objects.equals(connection, currentConnection)) {
                    forcePollingCycle();
                }

                return;

            case CLOSE:
                closeConnection(DisconnectReason.CLOSED_REMOTELY, connection);
                return;

            case UPGRADE:
                upgradeConnection(connection);
                return;

            default:
                throw new UnsupportedOperationException("EIO Packet " + packet + " is not implemented yet");

        }
    }

    private void onPacket(final SocketIOPacket packet) {
        switch (packet.getType()) {
            case CONNECT:
                connect(packet);
                return;

            case DISCONNECT:
                closeConnection(DisconnectReason.CLOSED_REMOTELY, activeConnection.get());
                return;

            case EVENT:
                onEvent((EventPacket) packet);
                return;

            case ACK:
                onACK((ACKPacket) packet);
                return;

            case BINARY_ACK:
            case BINARY_EVENT:
                binaryPacket.set((BinaryPacket) packet);
                return;

            default:
                throw new UnsupportedOperationException("SocketIO packet " + packet.getType() + " is not implemented yet");
        }
    }

    private void connect(final SocketIOPacket packet) {
        try {
            if (socketIOManager.getNamespace(packet.getNamespace()) == null) {
                getConnection().send(createErrorPacket(packet.getNamespace(), "Invalid namespace"));
                return;
            }

            final Socket socket = createSocket(packet.getNamespace());
            getConnection().send(createConnectPacket(packet.getNamespace()));
            invokeOnConnectOnNamespace(socket);
        } catch (final SocketIOException e) {
            LOGGER.warn("Cannot send packet to the client", e);
            closeConnection(DisconnectReason.CONNECT_FAILED, activeConnection.get());
        }
    }

    private void invokeOnConnectOnNamespace(final Socket socket) throws SocketIOException {
        try {
            socketIOManager.getNamespace(socket.getNamespace()).onConnect(socket);
        } catch (final ConnectionException e) {
            getConnection().send(createErrorPacket(socket.getNamespace(), e.getArgs()));
            socket.disconnect(false);
        }
    }

    private void onPing(final String data, final TransportConnection connection) {
        try {
            connection.send(createPongPacket(data));
        } catch (final SocketIOException e) {
            LOGGER.warn("connection.send failed", e);
            closeConnection(DisconnectReason.ERROR, connection);
        }
    }

    private void onEvent(final EventPacket packet) {
        final ConnectionState connectionState = state.get();
        if (connectionState != ConnectionState.CONNECTED) {
            return;
        }

        try {
            final TransportConnection connection = activeConnection.get();
            final Namespace ns = socketIOManager.getNamespace(packet.getNamespace());
            if (ns == null) {
                getConnection().send(createErrorPacket(packet.getNamespace(), "Invalid namespace"));
                return;
            }

            final Socket socket = sockets.get(ns.getId());
            if (socket == null) {
                connection.send(createErrorPacket(packet.getNamespace(), "No socket is connected to the namespace"));
                return;
            }

            final Object ack = socket.onEvent(packet.getName(), packet.getArgs(), packet.getId() != -1);
            if ((packet.getId() != -1) && (ack != null)) {
                final Object[] args = ack instanceof Object[] ? (Object[]) ack : new Object[]{ack};
                connection.send(createACKPacket(packet.getId(), packet.getNamespace(), args));
            }
        } catch (final Throwable e) {
            LOGGER.warn("Session[" + sessionId + "]: Exception thrown by one of the event listeners", e);
        }
    }

    private void onACK(final ACKPacket packet) {
        final ConnectionState connectionState = state.get();
        if (connectionState != ConnectionState.CONNECTED) {
            return;
        }

        try {
            final ACKListener listener = ackListeners.get(packet.getId());

            unsubscribeACK(packet.getId());
            if (listener != null) {
                listener.onACK(packet.getArgs());
            }
        } catch (final Throwable e) {
            LOGGER.warn("Session[" + sessionId + "]: Exception thrown by ACK listener", e);
        }
    }

    private void upgradeConnection(final TransportConnection connection) {
        final Transport oldTransport = activeConnection.get().getTransport();
        final Transport newTransport = connection.getTransport();

        LOGGER.trace("Upgrading from {} to {}", oldTransport, newTransport);
        activeConnection.set(connection);
    }

    /**
     * Remembers the disconnect reason and closes underlying transport activeConnection
     */
    private void closeConnection(final DisconnectReason reason, final TransportConnection connection) {
        if (Objects.equals(activeConnection, connection)) {
            setDisconnectReason(reason);
        }

        //this call should trigger onShutdown() eventually
        connection.abort();
    }

    public int getNewPacketId() {
        return packetId.incrementAndGet();
    }

    public void subscribeACK(final int packetId, final ACKListener ackListener) {
        ackListeners.put(packetId, ackListener);
        socketIOManager.executor.schedule(() -> {
            LOGGER.warn("No ACK received for packet {} (session {})", packetId, sessionId);
            ackListeners.remove(packetId);
        }, ackTimeout, TimeUnit.MILLISECONDS);
    }

    public void unsubscribeACK(final int packetId) {
        ackListeners.remove(packetId);
    }

    @Override
    public void onDisconnect(final Socket socket, final DisconnectReason reason, final String errorMessage) {
        final String namespace = socket.getNamespace();
        sockets.remove(namespace);
    }

    // hack to replicate current Socket.IO client behaviour
    private void forcePollingCycle() {
        try {
            getConnection().send(createNoopPacket());
        } catch (final SocketIOException e) {
            LOGGER.warn("Cannot send NOOP packet while upgrading the transport", e);
        }
    }
}
