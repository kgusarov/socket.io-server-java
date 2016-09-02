package org.kgusarov.socketio.core.protocol.packets;

import org.kgusarov.socketio.core.protocol.BinaryPacket;
import org.kgusarov.socketio.core.protocol.SocketIOProtocol;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Binary event packet class
 *
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public class BinaryEventPacket extends EventPacket implements BinaryPacket {
    private final List<InputStream> attachments;
    private int expectedNumberOfAttachments;

    /**
     * This constructor suppose to be called by parser when new packet arrived
     *
     * @param id                          packet id. Used for ACK
     * @param name                        event name
     * @param args                        event arguments as array of POJOs to be converted to JSON.
     * @param expectedNumberOfAttachments number of binary attachment expected to be attached to this packed
     */
    public BinaryEventPacket(final int id, final String namespace, final String name, final Object[] args, final int expectedNumberOfAttachments) {
        super(Type.BINARY_EVENT, id, namespace, name, args);

        this.expectedNumberOfAttachments = expectedNumberOfAttachments;
        attachments = new ArrayList<>(expectedNumberOfAttachments);
    }

    /**
     * This constructor suppose to be called by user by emit() call
     *
     * @param id        packet id
     * @param namespace packet namespace
     * @param name      event name
     * @param args      event arguments as array of POJOs to be converted to JSON.
     *                  {@link java.io.InputStream} to be used for binary objects
     */
    public BinaryEventPacket(final int id, final String namespace, final String name, final Object[] args) {
        super(Type.BINARY_EVENT, id, namespace, name, null);

        attachments = new LinkedList<>();

        // We know that extractBinaryObjects does not change the structure of the object,
        // so we can safely case it to Object[]
        setArgs((Object[]) SocketIOProtocol.extractBinaryObjects(args, attachments));
    }

    @Override
    public Collection<InputStream> getAttachments() {
        return attachments;
    }

    @Override
    protected String encodeAttachments() {
        final int attachmentCount = attachments.size();
        return SocketIOProtocol.encodeAttachments(attachmentCount);
    }

    /**
     * @return true when all expected attachment arrived, false otherwise
     */
    @Override
    public boolean isComplete() {
        return expectedNumberOfAttachments == 0;
    }

    /**
     * This method to be called when new attachement arrives to the socket
     *
     * @param attachment new attachment
     */
    @Override
    public void addAttachment(final InputStream attachment) {
        attachments.add(attachment);
        expectedNumberOfAttachments -= 1;
    }
}
