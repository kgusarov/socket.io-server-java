package org.kgusarov.socketio.core.protocol.packets;

import org.kgusarov.socketio.core.protocol.BinaryPacket;
import org.kgusarov.socketio.core.protocol.SocketIOProtocol;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public class BinaryACKPacket extends ACKPacket implements BinaryPacket {
    private final List<InputStream> attachments;
    private int expectedNumberOfAttachments;

    public BinaryACKPacket(final int id, final String ns, final Object[] args) {
        super(Type.BINARY_ACK, id, ns, null);

        attachments = new ArrayList<>();

        setArgs((Object[]) SocketIOProtocol.extractBinaryObjects(args, attachments));
    }

    public BinaryACKPacket(final int id, final String ns, final Object[] args, final int expectedNumberOfAttachments) {
        super(Type.BINARY_ACK, id, ns, args);

        this.expectedNumberOfAttachments = expectedNumberOfAttachments;
        attachments = new ArrayList<>(expectedNumberOfAttachments);
    }

    @Override
    protected String encodeAttachments() {
        final int attachmentCount = attachments.size();
        return SocketIOProtocol.encodeAttachments(attachmentCount);
    }

    @Override
    public Collection<InputStream> getAttachments() {
        return attachments;
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
