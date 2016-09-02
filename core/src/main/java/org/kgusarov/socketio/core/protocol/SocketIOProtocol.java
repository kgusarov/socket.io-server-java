package org.kgusarov.socketio.core.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.kgusarov.socketio.core.common.SocketIOProtocolException;
import org.kgusarov.socketio.core.protocol.packets.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Integer.parseInt;

/**
 * Implementation of Socket.IO Protocol version 4
 *
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public final class SocketIOProtocol {
    private static final Logger LOGGER = LoggerFactory.getLogger(SocketIOProtocol.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ATTACHMENTS_DELIMITER = "-";
    private static final String NAMESPACE_PREFIX = "/";
    private static final String PLACEHOLDER = "_placeholder";
    private static final String NUM = "num";

    static final String NAMESPACE_DELIMITER = ",";

    public static final String DEFAULT_NAMESPACE = "/";

    static {
        MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    private SocketIOProtocol() {
    }

    @SuppressWarnings("NestedMethodCall")
    public static SocketIOPacket decode(final String data) throws SocketIOProtocolException {
        if (data == null) {
            throw new SocketIOProtocolException("Cannot decode null");
        }

        if (data.isEmpty()) {
            throw new SocketIOProtocolException("Empty SIO packet");
        }

        try {
            final ParsePosition pos = new ParsePosition(0);
            final SocketIOPacket.Type type = decodePacketType(data, pos);

            int attachments = 0;
            if (type.attachmentsExpected()) {
                attachments = decodeAttachments(data, pos);
            }

            final String namespace = decodeNamespace(data, pos);
            final int packetId = decodePacketId(data, pos);

            final List<?> args;
            String eventName = "";

            if (type.argsExpected()) {
                args = decodeArgs(data, pos, List.class);

                if (type.isEvent()) {
                    if (args.isEmpty()) {
                        throw new SocketIOProtocolException("Missing event name");
                    }
                    
                    eventName = args.get(0).toString();
                    args.remove(0);
                }
            } else {
                args = Collections.emptyList();
            }

            switch (type) {
                case CONNECT:
                    return createConnectPacket(namespace);

                case DISCONNECT:
                    return createDisconnectPacket(namespace);

                case EVENT:
                    return new PlainEventPacket(packetId, namespace, eventName, args.toArray());

                case ACK:
                    return new PlainACKPacket(packetId, namespace, args.toArray());

                case ERROR:
                    return createErrorPacket(namespace, decodeArgs(data, pos, Object.class));

                case BINARY_EVENT:
                    return new BinaryEventPacket(packetId, namespace, eventName, args.toArray(), attachments);

                case BINARY_ACK:
                    return new BinaryACKPacket(packetId, namespace, args.toArray(), attachments);

                default:
                    throw new SocketIOProtocolException("Unsupported packet type " + type);
            }
        } catch (final NumberFormatException e) {
            final String message = "Invalid SIO packet: " + data;

            LOGGER.warn(message, e);
            throw new SocketIOProtocolException(message, e);
        }
    }

    public static SocketIOPacket createErrorPacket(final String namespace, final Object args) {
        return new ErrorPacket(namespace, args);
    }

    /*
     * This method could create either EventPacket or BinaryEventPacket based
     * on the content of args parameter.
     * If args has any InputStream inamespaceide then SockeIOBinaryEventPacket will be created
     */
    public static SocketIOPacket createEventPacket(final int packetId, final String namespace, final String name, final Object[] args) {
        return hasBinary(args) ?
                new BinaryEventPacket(packetId, namespace, name, args) :
                new PlainEventPacket(packetId, namespace, name, args);
    }

    public static SocketIOPacket createACKPacket(final int id, final String namespace, final Object[] args) {
        return hasBinary(args) ?
                new BinaryACKPacket(id, namespace, args) :
                new PlainACKPacket(id, namespace, args);
    }

    public static SocketIOPacket createDisconnectPacket(final String namespace) {
        return new EmptyPacket(SocketIOPacket.Type.DISCONNECT, namespace);
    }

    public static SocketIOPacket createConnectPacket(final String namespace) {
        return new EmptyPacket(SocketIOPacket.Type.CONNECT, namespace);
    }

    public static String toJSON(final Object o) throws SocketIOProtocolException {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (final IOException e) {
            throw new SocketIOProtocolException("Cannot convert object to JSON", e);
        }
    }

    static <T> T fromJSON(final String s, final Class<? extends T> clazz) throws SocketIOProtocolException {
        try {
            if (s == null || s.isEmpty()) {
                return null;
            }

            return MAPPER.readValue(s, clazz);
        } catch (final IOException e) {
            throw new SocketIOProtocolException("Cannot parse JSON", e);
        }
    }

    static String decodeNamespace(final String data, final ParsePosition pos) {
        String namespace = DEFAULT_NAMESPACE;
        final int index = pos.getIndex();

        if (data.startsWith(NAMESPACE_PREFIX, index)) {
            final int idx = data.indexOf(NAMESPACE_DELIMITER, index);

            if (idx < 0) {
                final int length = data.length();

                namespace = data.substring(index);
                pos.setIndex(length);
            } else {
                namespace = data.substring(index, idx);
                pos.setIndex(idx + 1);
            }
        }

        return namespace;
    }

    static int decodeAttachments(final String data, final ParsePosition pos) throws SocketIOProtocolException {
        final Number n = new DecimalFormat("#").parse(data, pos);
        if ((n == null) || (n.intValue() == 0)) {
            throw new SocketIOProtocolException("No attachments defined in BINARY packet: " + data);
        }

        final int idx = pos.getIndex();

        //skipping '-' delimiter
        pos.setIndex(idx + 1);

        return n.intValue();
    }

    static int decodePacketId(final String data, final ParsePosition pos) {
        final Number id = new DecimalFormat("#").parse(data, pos);
        if (id == null)
            return -1;

        return id.intValue();
    }

    static SocketIOPacket.Type decodePacketType(final String data, final ParsePosition pos) {
        final int idx = pos.getIndex();
        final String substr = data.substring(idx, idx + 1);
        final SocketIOPacket.Type type = SocketIOPacket.Type.decode(parseInt(substr));
        
        pos.setIndex(idx + 1);

        return type;
    }

    static <T> T decodeArgs(final String data, final ParsePosition pos, final Class<? extends T> clazz) throws SocketIOProtocolException {
        final int idx = pos.getIndex();
        final int length = data.length();
        final String substr = data.substring(idx);
        final T json = fromJSON(substr, clazz);

        pos.setIndex(length);
        return json;
    }

    static String encodeNamespace(final String namespace, final boolean addDelimiter) {
        if (namespace.equals(DEFAULT_NAMESPACE)) {
            return "";
        }

        return namespace + (addDelimiter ? NAMESPACE_DELIMITER : "");
    }

    public static String encodeAttachments(final int size) {
        return size + ATTACHMENTS_DELIMITER;
    }

    private static boolean hasBinary(final Object args) {
        if (args.getClass().isArray()) {
            final Object[] values = (Object[]) args;
            final Stream<Object> stream = Arrays.stream(values);

            return hasBinaryData(stream);
        }

        if (args instanceof Map) {
            final Collection<?> values = ((Map<?, ?>) args).values();
            final Stream<?> stream = values.stream();

            return hasBinaryData(stream);
        }

        return args instanceof InputStream;
    }

    private static boolean hasBinaryData(final Stream<?> stream) {
        return stream.filter(SocketIOProtocol::hasBinary)
                .findAny()
                .isPresent();
    }

    /**
     * Extracts binary objects (InputStream) from JSON and replaces it with
     * placeholder objects {@code {"_placeholder":true,"num":1} }
     * This method to be used before sending the packet
     *
     * @param json        JSON object
     * @param attachments container for extracted binary object
     * @return modified JSON object
     */
    @SuppressWarnings({"unchecked", "NestedMethodCall", "SerializableStoresNonSerializable"})
    public static Object extractBinaryObjects(final Object json, final List<InputStream> attachments) {
        if (json instanceof Collection) {
            final Collection<?> collection = (Collection<?>) json;
            final int size = collection.size();
            final List<Object> copy = new ArrayList<>(size);

            copy.addAll(collection.stream()
                    .map(o -> extractBinaryObjects(o, attachments))
                    .collect(Collectors.toList()));

            return copy;
        }

        if (json.getClass().isArray()) {
            final List<Object> array = new ArrayList<>(((Object[]) json).length);
            for (final Object o : (Object[]) json) {
                array.add(extractBinaryObjects(o, attachments));
            }

            return array.toArray();
        }

        if (json instanceof Map) {
            final Map<?, ?> map = (Map<?, ?>) json;
            final Set<? extends Map.Entry<?, ?>> entries = map.entrySet();
            final Map<Object, Object> result = new LinkedHashMap<>();

            for (final Map.Entry<?, ?> entry : entries) {
                final Object key = entry.getKey();
                final Object value = extractBinaryObjects(entry, attachments);

                result.put(key, value);
            }

            return result;
        }

        if (json instanceof InputStream) {
            final LinkedHashMap<String, Object> map = new LinkedHashMap<>();

            map.put(PLACEHOLDER, true);
            map.put(NUM, attachments.size());
            attachments.add((InputStream) json);

            return map;
        }

        return json;
    }

    /**
     * Looks for the placeholder objects in {@code json.getArgs() } {@code {"_placeholder":true,"num":1}} and
     * replaces it with {@code attachment}
     * This method to be used when binary object are received from the client
     *
     * @param packet     packet to add a binary object
     * @param attachment binary object to inamespaceert
     * @throws SocketIOProtocolException if no placeholder object is found
     */
    public static void insertBinaryObject(final BinaryPacket packet, final InputStream attachment) throws SocketIOProtocolException {
        final AtomicBoolean found = new AtomicBoolean(false);
        final Object[] args = packet.getArgs();
        final int attachmentCount = packet.getAttachments().size();
        final Object copy = insertBinaryObject(args, attachment, attachmentCount, found);

        if (!found.get()) {
            throw new SocketIOProtocolException("No placeholder found for a binary object");
        }

        packet.setArgs((Object[]) copy);
    }

    /*
     * This method makes a copy of {@code json} replacing placeholder entry with {@code attachment}
     *
     * @param json       JSON object
     * @param attachment InputStream object to inamespaceert
     * @return copy of JSON object
     */
    @SuppressWarnings("unchecked")
    private static Object insertBinaryObject(final Object json, final InputStream attachment, final int index,
                                             final AtomicBoolean found) throws SocketIOProtocolException {

        if (json instanceof Collection) {
            final Collection<?> collection = (Collection<?>) json;
            final int size = collection.size();
            final List<Object> copy = new ArrayList<>(size);

            for (final Object o : collection) {
                copy.add(insertBinaryObject(o, attachment, index, found));
            }

            return copy;
        }

        if (json.getClass().isArray()) {
            final Object[] values = (Object[]) json;
            final List<Object> copy = new ArrayList<>(values.length);

            for (final Object o : values) {
                copy.add(insertBinaryObject(o, attachment, index, found));
            }

            return copy.toArray();
        }

        if (json instanceof Map) {
            final Map<?, ?> map = (Map<?, ?>) json;

            if (isPlaceholder(map, index)) {
                found.set(true);
                return attachment;
            }

            final Map<Object, Object> copy = new LinkedHashMap<>();
            final Set<? extends Map.Entry<?, ?>> entries = map.entrySet();

            for (final Map.Entry<?, ?> e : entries) {
                final Object key = e.getKey();
                final Object value = e.getValue();

                copy.put(key, insertBinaryObject(value, attachment, index, found));
            }

            return copy;
        }

        return json;
    }

    private static boolean isPlaceholder(final Map<?, ?> map, final int index) throws SocketIOProtocolException {
        final Object val = map.get(PLACEHOLDER);

        if (Boolean.TRUE.equals(val)) {
            final Object o = map.get(NUM);

            if (o == null) {
                return false;
            }

            if (o instanceof Number) {
                return index == ((Number) o).intValue();
            }

            if (o instanceof String) {
                try {
                    final String str = o.toString();
                    return index == parseInt(str);
                } catch (final NumberFormatException e) {
                    throw new SocketIOProtocolException("Invalid placeholder object", e);
                }
            }
        }

        return false;
    }
}
