package org.kgusarov.socketio.core.protocol;

import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import org.kgusarov.socketio.core.common.SocketIOProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of Engine.IO Protocol version 3
 *
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public final class EngineIOProtocol {
    private static final Logger LOGGER = LoggerFactory.getLogger(EngineIOProtocol.class);

    private static final int LEN_FACTOR = 10;
    private static final int LARGEST_DIGIT = 9;
    private static final String UPGRADES = "upgrades";
    private static final String PING_INTERVAL = "pingInterval";
    private static final String PING_TIMEOUT = "pingTimeout";
    private static final int PAYLOAD_MARKER = 255;
    private static final int BINARY_PACKET_FLAG = 1;
    private static final int TEXT_PACKET_FLAG = 0;

    public static final String TRANSPORT = "transport";
    public static final String JSONP_INDEX = "j";
    public static final String VERSION = "EIO";
    public static final String SESSION_ID = "sid";
    public static final String UTF8_NAME = StandardCharsets.UTF_8.name();

    // TODO: public static final String BASE64_FLAG = "b64";

    private EngineIOProtocol() {
    }

    public static String encode(final EngineIOPacket packet) {
        return packet.getType().value() + packet.getTextData();
    }

    @SuppressWarnings("resource")
    public static void binaryEncode(final EngineIOPacket packet, final OutputStream os) throws IOException, SocketIOProtocolException {
        final InputStream binaryData = packet.getBinaryData();

        final int packetTypeValue = packet.getType().value();

        if (binaryData != null) {
            ByteArrayInputStream bytes = null;

            try {
                if (binaryData instanceof ByteArrayInputStream) {
                    bytes = (ByteArrayInputStream) binaryData;
                } else {
                    // Cannot avoid double copy. The protocol requires to send the length before the data
                    final byte[] buffer = ByteStreams.toByteArray(binaryData);
                    bytes = new ByteArrayInputStream(buffer);
                }

                final int length = bytes.available();

                os.write(BINARY_PACKET_FLAG);
                os.write(encodeLength(length + 1));
                os.write(PAYLOAD_MARKER);
                os.write(packetTypeValue);
                ByteStreams.copy(bytes, os);
            } finally {
                Closeables.closeQuietly(bytes);
            }
        } else {
            final String textData = packet.getTextData();
            if (textData == null) {
                throw new SocketIOProtocolException("No text nor binary data present in packet");
            }

            final int encodedPacketType = packetTypeValue + '0';
            final byte[] data = textData.getBytes(UTF8_NAME);
            final int length = data.length;

            os.write(TEXT_PACKET_FLAG);
            os.write(encodeLength(length + 1));
            os.write(PAYLOAD_MARKER);
            os.write(encodedPacketType);
            os.write(data);
        }
    }

    //this is most ridiculous encoding I ever seen
    private static byte[] encodeLength(final int len) {
        final byte[] bytes = String.valueOf(len).getBytes();
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] -= '0';
        }

        return bytes;
    }

    public static EngineIOPacket decode(final String raw) throws SocketIOProtocolException {
        if (raw == null) {
            throw new SocketIOProtocolException("Cannot decode null value");
        }

        if (raw.isEmpty()) {
            throw new SocketIOProtocolException("Empty EIO packet");
        }

        try {
            final String encodedPacketType = raw.substring(0, 1);
            final EngineIOPacket.Type packetType = EngineIOPacket.Type.decode(Integer.parseInt(encodedPacketType));
            final String data = raw.substring(1);

            return new EngineIOPacket(packetType, data);
        } catch (final NumberFormatException e) {
            throw new SocketIOProtocolException("Invalid EIO packet type: " + raw, e);
        }
    }

    public static EngineIOPacket decode(final InputStream raw) throws SocketIOProtocolException {
        if (raw == null) {
            throw new SocketIOProtocolException("Cannot decode null value");
        }

        try {
            final int type = raw.read();
            if (type == -1) {
                throw new SocketIOProtocolException("Empty binary object received");
            }

            return new EngineIOPacket(EngineIOPacket.Type.decode(type), raw);
        } catch (final IOException e) {
            throw new SocketIOProtocolException("Cannot read packet type from binary object", e);
        }
    }

    public static EngineIOPacket createHandshakePacket(final String sessionId, final String[] upgrades,
                                                       final long pingInterval, final long pingTimeout) {

        final Map<String, Object> map = new LinkedHashMap<>();
        map.put(SESSION_ID, sessionId);
        map.put(UPGRADES, upgrades);
        map.put(PING_INTERVAL, pingInterval);
        map.put(PING_TIMEOUT, pingTimeout);

        try {
            return new EngineIOPacket(EngineIOPacket.Type.OPEN, SocketIOProtocol.toJSON(map));
        } catch (final SocketIOProtocolException e) {
            LOGGER.warn("Failed to decode Engine.IO packet", e);
            return null;
        }
    }

    public static EngineIOPacket createOpenPacket() {
        return new EngineIOPacket(EngineIOPacket.Type.OPEN);
    }

    public static EngineIOPacket createClosePacket() {
        return new EngineIOPacket(EngineIOPacket.Type.CLOSE);
    }

    public static EngineIOPacket createPingPacket(final String data) {
        return new EngineIOPacket(EngineIOPacket.Type.PING, data);
    }

    public static EngineIOPacket createPongPacket(final String data) {
        return new EngineIOPacket(EngineIOPacket.Type.PONG, data);
    }

    public static EngineIOPacket createMessagePacket(final String data) {
        return new EngineIOPacket(EngineIOPacket.Type.MESSAGE, data);
    }

    public static EngineIOPacket createMessagePacket(final InputStream data) {
        return new EngineIOPacket(EngineIOPacket.Type.MESSAGE, data);
    }

    public static EngineIOPacket createUpgradePacket() {
        return new EngineIOPacket(EngineIOPacket.Type.UPGRADE);
    }

    public static EngineIOPacket createNoopPacket() {
        return new EngineIOPacket(EngineIOPacket.Type.NOOP);
    }

    public static List<EngineIOPacket> decodePayload(final String payload) throws SocketIOProtocolException {

        final List<EngineIOPacket> packets = new ArrayList<>();

        final ParsePosition pos = new ParsePosition(0);

        while (pos.getIndex() < payload.length()) {
            final int len = decodePacketLength(payload, pos);
            final EngineIOPacket.Type type = decodePacketType(payload, pos);
            final int idx = pos.getIndex();
            final String data = payload.substring(idx, idx + len - 1);

            pos.setIndex(idx + 1 + len);
            switch (type) {
                case CLOSE:
                    packets.add(createClosePacket());
                    break;
                case PING:
                    packets.add(createPingPacket(data));
                    break;
                case MESSAGE:
                    packets.add(createMessagePacket(data));
                    break;
                case UPGRADE:
                    packets.add(createUpgradePacket());
                    break;
                case NOOP:
                    packets.add(createNoopPacket());
                    break;
                default:
                    throw new SocketIOProtocolException("Unexpected EIO packet type: " + type);
            }
        }

        return packets;
    }

    static int decodePacketLength(final String data, final ParsePosition pos) throws SocketIOProtocolException {
        final Number len = new DecimalFormat("#").parse(data, pos);
        if (len == null) {
            throw new SocketIOProtocolException("No packet length defined");
        }

        final int idx = pos.getIndex();
        pos.setIndex(idx + 1);

        return len.intValue();
    }

    static EngineIOPacket.Type decodePacketType(final String data, final ParsePosition pos) {
        final int idx = pos.getIndex();
        final String substr = data.substring(idx, idx + 1);
        final EngineIOPacket.Type type = EngineIOPacket.Type.decode(Integer.parseInt(substr));

        pos.setIndex(idx + 1);

        return type;
    }

    static int decodePacketLength(final InputStream is) throws IOException {
        int len = 0;

        while (true) {
            final int b = is.read();
            if (b < 0) {
                // end of stream. time to go
                return -1;
            }

            if (b > LARGEST_DIGIT) {
                // end of encoded length
                break;
            }

            len = len * LEN_FACTOR + b;
        }

        return len;
    }

    static EngineIOPacket.Type decodePacketType(final InputStream is) throws SocketIOProtocolException, IOException {
        final int i = is.read();
        if (i < 0) {
            throw new SocketIOProtocolException("Unexpected end of stream");
        }

        return EngineIOPacket.Type.decode(i);
    }

    public static List<EngineIOPacket> binaryDecodePayload(final InputStream is) throws SocketIOProtocolException, IOException {
        final List<EngineIOPacket> packets = new ArrayList<>();
        if (is.read() != 1) {
            throw new SocketIOProtocolException("Expected binary marker in the payload");
        }

        while (true) {
            int len = decodePacketLength(is);
            if (len < 0) {
                break;
            }

            if (len == 0) {
                throw new SocketIOProtocolException("Empty binary attahcment");
            }

            final EngineIOPacket.Type type = decodePacketType(is);
            len -= 1;

            final byte[] data = new byte[len];
            ByteStreams.readFully(is, data);

            switch (type) {
                case MESSAGE:
                    packets.add(createMessagePacket(new ByteArrayInputStream(data)));
                    break;
                default:
                    throw new SocketIOProtocolException("Unexpected EIO packet type: " + type);
            }
        }

        return packets;
    }
}
