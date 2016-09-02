package org.kgusarov.socketio.core.server.transport.connection;

import org.apache.commons.io.IOUtils;
import org.kgusarov.socketio.core.common.SocketIOException;
import org.kgusarov.socketio.core.common.SocketIOProtocolException;
import org.kgusarov.socketio.core.protocol.BinaryPacket;
import org.kgusarov.socketio.core.protocol.EngineIOPacket;
import org.kgusarov.socketio.core.protocol.SocketIOPacket;
import org.kgusarov.socketio.core.server.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import static org.kgusarov.socketio.core.protocol.EngineIOProtocol.*;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public class XHRTransportConnection extends AbstractTransportConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(XHRTransportConnection.class);
    private static final String ALLOWED_ORIGINS = "allowedOrigins";
    private static final String ALLOW_ALL_ORIGINS = "allowAllOrigins";

    private final BlockingQueue<EngineIOPacket> packets = new LinkedBlockingDeque<>();

    private volatile boolean done;

    public XHRTransportConnection(final Transport transport) {
        super(transport);
    }

    @Override
    public void handle(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        if (done) {
            return;
        }

        // store request for end-user to check for cookies, or user-agent or something else
        setRequest(request);

        if (getConfig().getBoolean(ALLOW_ALL_ORIGINS, false)) {
            final String origin = request.getHeader("Origin");
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        } else {
            final String origins = getConfig().getString(ALLOWED_ORIGINS);
            if (origins != null) {
                response.setHeader("Access-Control-Allow-Origin", origins);
                response.setHeader("Access-Control-Allow-Credentials", "true");
            }
        }

        final String requestMethod = request.getMethod();
        if ("POST".equals(requestMethod)) {
            try {
                processInbound(request, response);
            } catch (final SocketIOProtocolException e) {
                throw new IOException("Failed to process inbound message", e);
            }
        } else if ("GET".equals(requestMethod)) {
            try {
                processOutbound(response);
            } catch (final SocketIOProtocolException e) {
                throw new IOException("Failed to process outbound request", e);
            }
        } else if (!"OPTIONS".equals(requestMethod)) {
            // OPTIONS is CORS pre-flight request
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    protected void processOutbound(final HttpServletResponse response) throws IOException, SocketIOProtocolException {
        response.setContentType("application/octet-stream");

        try {

            final OutputStream os = response.getOutputStream();
            for (EngineIOPacket packet = packets.take(); packet != null; packet = packets.poll()) {
                if (done) {
                    break;
                }

                binaryEncode(packet, os);
            }

            response.flushBuffer();
        } catch (final InterruptedException e) {
            LOGGER.warn("Polling connection interrupted", e);
        }
    }

    private void processInbound(final HttpServletRequest request, final HttpServletResponse response) throws IOException, SocketIOProtocolException {
        response.setContentType("text/plain");

        final String contentType = request.getContentType();
        final ServletInputStream inputStream = request.getInputStream();

        if (contentType.startsWith("text/")) {
            // text encoding
            //IOUtils.toString(new ByteArrayInputStream(payload.getBytes(StandardCharsets.ISO_8859_1)))
            final String characterEncoding = request.getCharacterEncoding();
            final String payload = IOUtils.toString(inputStream, characterEncoding);

            for (final EngineIOPacket packet : decodePayload(payload)) {
                getSession().onPacket(packet, this);
            }
        } else if (contentType.startsWith("application/octet-stream")) {
            // binary encoding
            for (final EngineIOPacket packet : binaryDecodePayload(inputStream)) {
                getSession().onPacket(packet, this);
            }
        } else {
            throw new SocketIOProtocolException("Unsupported request content type for incoming polling request: " + contentType);
        }

        response.getWriter().print("ok");
    }

    @Override
    public void abort() {
        try {
            done = true;
            send(createNoopPacket());
        } catch (final SocketIOException e) {
            LOGGER.warn("Failed to abort connection", e);
        }
    }

    @Override
    public void send(final EngineIOPacket packet) throws SocketIOException {
        packets.add(packet);
    }

    @Override
    public void send(final SocketIOPacket packet) throws SocketIOException {
        final String encodedPacket = packet.encode();
        send(createMessagePacket(encodedPacket));

        if (packet instanceof BinaryPacket) {
            for (final InputStream is : ((BinaryPacket) packet).getAttachments()) {
                send(createMessagePacket(is));
            }
        }
    }

    protected void setDone(final boolean done) {
        this.done = done;
    }
}
