package org.kgusarov.socketio.core.protocol;

import org.kgusarov.socketio.core.common.SocketIOProtocolException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.kgusarov.socketio.core.protocol.SocketIOProtocol.encodeNamespace;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public abstract class SocketIOPacket {
    @SuppressWarnings("NestedMethodCall")
    public enum Type {
        UNKNOWN(-1, false, false),
        CONNECT(0, false, false),
        DISCONNECT(1, false, false),
        EVENT(2, true, false),
        ACK(3, true, false),
        ERROR(4, false, false),
        BINARY_EVENT(5, true, true),
        BINARY_ACK(6, true, true),;

        private static final Map<Integer, Type> ID_TO_TYPE = new HashMap<>();

        static {
            Arrays.stream(Type.values())
                    .forEach(t -> ID_TO_TYPE.put(t.value, t));
        }

        private final int value;
        private final boolean argsExpected;
        private final boolean attachmentsExpected;

        Type(final int value, final boolean argsExpected, final boolean attachmentsExpected) {
            this.value = value;
            this.argsExpected = argsExpected;
            this.attachmentsExpected = attachmentsExpected;
        }

        public int value() {
            return value;
        }

        public boolean argsExpected() {
            return argsExpected;
        }

        public boolean attachmentsExpected() {
            return attachmentsExpected;
        }

        public boolean isEvent() {
            return (this == EVENT) || (this == BINARY_EVENT);
        }

        public static Type decode(final int typeId) {
            return ID_TO_TYPE.getOrDefault(typeId, UNKNOWN);
        }
    }

    private final int id;
    private final Type type;
    private final String namespace;

    public Type getType() {
        return type;
    }

    public String getNamespace() {
        return namespace;
    }

    public int getId() {
        return id;
    }

    protected abstract String encodeArgs() throws SocketIOProtocolException;

    protected String encodeAttachments() {
        return "";
    }

    private String encodePacketId() {
        if (id < 0)
            return "";

        return String.valueOf(id);
    }

    protected SocketIOPacket(final Type type) {
        this(type, SocketIOProtocol.DEFAULT_NAMESPACE);
    }

    protected SocketIOPacket(final Type type, final String namespace) {
        this(type, -1, namespace);
    }

    protected SocketIOPacket(final Type type, final int id, final String namespace) {
        this.type = type;
        this.namespace = namespace;
        this.id = id;
    }

    public String encode() throws SocketIOProtocolException {
        final int packetTypeCode = type.value();
        final String tail = encodePacketId() + encodeArgs();
        final boolean addDelimiter = !tail.isEmpty();

        return packetTypeCode
                + encodeAttachments()
                + encodeNamespace(namespace, addDelimiter)
                + tail;
    }
}
