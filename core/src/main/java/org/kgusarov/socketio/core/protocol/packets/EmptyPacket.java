package org.kgusarov.socketio.core.protocol.packets;

import org.kgusarov.socketio.core.protocol.SocketIOPacket;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public class EmptyPacket extends SocketIOPacket {
    public EmptyPacket(final Type type, final String namespace) {
        super(type, namespace);
    }

    @Override
    protected String encodeArgs() {
        return "";
    }
}
