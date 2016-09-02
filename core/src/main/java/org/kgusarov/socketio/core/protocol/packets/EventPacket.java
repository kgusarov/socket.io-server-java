package org.kgusarov.socketio.core.protocol.packets;

import org.kgusarov.socketio.core.common.SocketIOProtocolException;
import org.kgusarov.socketio.core.protocol.SocketIOPacket;
import org.kgusarov.socketio.core.protocol.SocketIOProtocol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public abstract class EventPacket extends SocketIOPacket {
    private final String name;
    private Object[] args;

    protected EventPacket(final Type type, final int id, final String namespace, final String name, final Object[] args) {
        super(type, id, namespace);

        this.name = name;
        this.args = args;
    }

    public String getName() {
        return name;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(final Object[] args) {
        this.args = args;
    }

    @Override
    protected String encodeArgs() throws SocketIOProtocolException {
        // adding name of the event as a first argument
        final List<Object> data = new ArrayList<>();
        data.add(getName());
        data.addAll(Arrays.asList(getArgs()));

        final Object[] objects = data.toArray();
        return SocketIOProtocol.toJSON(objects);
    }
}

