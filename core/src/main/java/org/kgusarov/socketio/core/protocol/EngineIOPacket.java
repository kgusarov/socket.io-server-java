package org.kgusarov.socketio.core.protocol;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public class EngineIOPacket {
    @SuppressWarnings("NestedMethodCall")
    public enum Type {
        OPEN(0),
        CLOSE(1),
        PING(2),
        PONG(3),
        MESSAGE(4),
        UPGRADE(5),
        NOOP(6),
        UNKNOWN(-1),;

        private static final Map<Integer, Type> ID_TO_TYPE = new HashMap<>();

        static {
            Arrays.stream(Type.values())
                    .forEach(t -> ID_TO_TYPE.put(t.value, t));
        }

        private final int value;

        Type(final int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public static Type decode(final int i) {
            return ID_TO_TYPE.getOrDefault(i, UNKNOWN);
        }
    }

    private final Type type;
    private String textData;
    private InputStream binaryData;

    public Type getType() {
        return type;
    }

    public String getTextData() {
        return textData;
    }

    public InputStream getBinaryData() {
        return binaryData;
    }

    public EngineIOPacket(final Type type, final String data) {
        this.type = type;
        textData = data;
    }

    //TODO: support byte[] in addtion to InputStream
    public EngineIOPacket(final Type type, final InputStream binaryData) {
        this.type = type;
        this.binaryData = binaryData;
    }

    public EngineIOPacket(final Type type) {
        this(type, "");
    }

    @Override
    public String toString() {
        return "EngineIOPacket{" +
                "type=" + type +
                '}';
    }
}
