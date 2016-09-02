package org.kgusarov.socketio.core.protocol.packets;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public class PlainEventPacket extends EventPacket {
    public PlainEventPacket(final int id, final String namespace, final String name, final Object[] args) {
        super(Type.EVENT, id, namespace, name, args);
    }
}
