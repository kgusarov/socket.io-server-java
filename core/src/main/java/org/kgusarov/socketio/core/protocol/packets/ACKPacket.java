package org.kgusarov.socketio.core.protocol.packets;

import org.kgusarov.socketio.core.common.SocketIOProtocolException;
import org.kgusarov.socketio.core.protocol.SocketIOPacket;
import org.kgusarov.socketio.core.protocol.SocketIOProtocol;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public abstract class ACKPacket extends SocketIOPacket {
    private Object[] args;

    protected ACKPacket(final Type type, final int id, final String ns, final Object[] args) {
        super(type, id, ns);
        this.args = args;
    }

    @Override
    protected String encodeArgs() throws SocketIOProtocolException {
        return SocketIOProtocol.toJSON(args);
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(final Object[] args) {
        this.args = args;
    }
}
