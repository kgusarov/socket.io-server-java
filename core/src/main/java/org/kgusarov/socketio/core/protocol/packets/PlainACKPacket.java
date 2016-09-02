package org.kgusarov.socketio.core.protocol.packets;

import org.kgusarov.socketio.core.protocol.SocketIOPacket;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public class PlainACKPacket extends ACKPacket {
    public PlainACKPacket(final int id, final String namespace, final Object[] args) {
        super(SocketIOPacket.Type.ACK, id, namespace, args);
    }
}
