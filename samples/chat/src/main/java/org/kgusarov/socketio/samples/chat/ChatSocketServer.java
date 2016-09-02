package org.kgusarov.socketio.samples.chat;

import com.google.common.io.ByteStreams;
import org.kgusarov.socketio.core.common.SocketIOException;
import org.kgusarov.socketio.core.server.ACKListener;
import org.kgusarov.socketio.core.server.impl.SocketIOServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@SuppressWarnings("SerializableStoresNonSerializable")
public final class ChatSocketServer {
    private static final String ANNOUNCEMENT = "announcement";       // server to all connected clients
    private static final String CHAT_MESSAGE = "chat message";       // broadcast to room
    private static final String WELCOME = "welcome";            // single event sent by server to specific client
    private static final String FORCE_DISCONNECT = "force disconnect";   // client requests server to disconnect
    private static final String SERVER_BINARY = "server binary";      // client requests server to send a binary
    private static final String CLIENT_BINARY = "client binary";      // client sends binary

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatSocketServer.class);

    @SuppressWarnings("NestedMethodCall")
    public void init(final SocketIOServlet servlet) {
        servlet.of("/chat").on(socket -> {
            try {
                socket.emit(WELCOME, "Welcome to Socket.IO Chat, " + socket.getRequest().getRemoteAddr() + '!');

                socket.join("room");
            } catch (final SocketIOException e) {
                LOGGER.error("Failed to join chat room, disconnecting", e);
                socket.disconnect(true);
            }

            socket.on((socket1, reason, errorMessage) -> {
                try {
                    servlet.of("/chat")
                            .emit(ANNOUNCEMENT, socket1.getSession().getSessionId() + " disconnected");
                } catch (final SocketIOException e) {
                    LOGGER.error("Fauled to emit message", e);
                }
            });

            socket.on(CHAT_MESSAGE, (name, args, ackRequested) -> {
                LOGGER.trace("Received chat message: " + args[0]);

                try {
                    socket.broadcast("room", CHAT_MESSAGE, socket.getId(), args[0]);
                } catch (final SocketIOException e) {
                    LOGGER.error("Failed to broadcast message", e);
                }

                //this object will be sent back to the client in ACK packet
                return "OK";
            });

            socket.on(FORCE_DISCONNECT, (name, args, ackRequested) -> {
                socket.disconnect(false);
                return null;
            });

            socket.on(CLIENT_BINARY, (name, args, ackRequested) -> {
                final Map<?, ?> map = (Map<?, ?>) args[0];
                final InputStream is = (InputStream) map.get("buffer");
                final ByteArrayOutputStream os = new ByteArrayOutputStream();

                try {
                    ByteStreams.copy(is, os);
                    final byte[] array = os.toByteArray();
                    final StringBuilder sb = new StringBuilder("[");

                    for (final byte b : array) {
                        sb.append(' ').append(b);
                    }

                    sb.append(" ]");
                    LOGGER.trace("Binary received: " + sb);
                } catch (final IOException e) {
                    LOGGER.error("Error processing client binary", e);
                }

                return "OK";
            });

            socket.on(SERVER_BINARY, (name, args, ackRequested) -> {
                try {
                    final ByteArrayInputStream bais = new ByteArrayInputStream(new byte[]{1, 2, 3, 4});
                    socket.emit(SERVER_BINARY, bais,
                            (ACKListener) args1 -> LOGGER.trace("ACK received: " + args1[0]));
                } catch (final SocketIOException e) {
                    LOGGER.error("Error processing server binary", e);
                    socket.disconnect(true);
                }

                return null;
            });
        });
    }
}
