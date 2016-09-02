package org.kgusarov.socketio.core.server;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
@SuppressWarnings("NestedMethodCall")
public enum TransportType {
    WEB_SOCKET("websocket"),
    FLASH_SOCKET("flashsocket"),
    JSONP_POLLING("jsonp-polling"),
    XHR_POLLING("xhr-polling"),
    UNKNOWN(""),;

    private static final Map<String, TransportType> NAME_TO_TYPE = new HashMap<>();

    static {
        Arrays.stream(TransportType.values())
                .forEach(t -> NAME_TO_TYPE.put(t.name, t));
    }

    private final String name;

    TransportType(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static TransportType decode(final String name) {
        return NAME_TO_TYPE.getOrDefault(name, UNKNOWN);
    }
}
