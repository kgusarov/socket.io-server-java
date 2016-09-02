package org.kgusarov.socketio.core.protocol.packets;

import org.kgusarov.socketio.core.common.SocketIOProtocolException;
import org.kgusarov.socketio.core.protocol.SocketIOPacket;
import org.kgusarov.socketio.core.protocol.SocketIOProtocol;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public class ErrorPacket extends SocketIOPacket {
    private final Object arg;

    public ErrorPacket(final String namespace, final Object arg) {
        super(Type.ERROR, namespace);
        this.arg = arg;
    }

    @Override
    protected String encodeArgs() throws SocketIOProtocolException {
        return SocketIOProtocol.toJSON(arg);
    }
}
