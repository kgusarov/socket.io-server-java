package org.kgusarov.socketio.core.server.transport;

import org.kgusarov.socketio.core.protocol.EngineIOProtocol;
import org.kgusarov.socketio.core.server.TransportType;
import org.kgusarov.socketio.core.server.impl.Session;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public abstract class JSONPPollingTransport extends AbstractHttpTransport {
    private static final String EIO_PREFIX = "___eio";
    private static final String FRAME_ID = JSONPPollingTransport.class.getName() + ".FRAME_ID";

    protected JSONPPollingTransport() {
    }

    @Override
    public TransportType getType() {
        return TransportType.JSONP_POLLING;
    }

    public void startSend(final Session session, final ServletResponse response) throws IOException {
        response.setContentType("text/javascript; charset=UTF-8");
    }

    @SuppressWarnings("NestedMethodCall")
    public void writeData(final Session session, final ServletResponse response, final String data) throws IOException {
        response.getOutputStream().print(EIO_PREFIX);
        response.getOutputStream().print("[" + session.getAttribute(FRAME_ID) + "]('");
        response.getOutputStream().print(data);
        response.getOutputStream().print("');");
    }

    public void finishSend(final Session session, final ServletResponse response) throws IOException {
        response.flushBuffer();
    }

    @SuppressWarnings("ProhibitedExceptionCaught")
    public void onConnect(final Session session, final ServletRequest request, final ServletResponse response) throws IOException {
        try {
            final String parameter = request.getParameter(EngineIOProtocol.JSONP_INDEX);
            session.setAttribute(FRAME_ID, Integer.parseInt(parameter));
        } catch (final NullPointerException | NumberFormatException e) {
            throw new IOException("Missing or invalid 'j' parameter. It suppose to be integer", e);
        }

        startSend(session, response);
    }
}
