package org.kgusarov.socketio.core.server.impl;

import org.kgusarov.socketio.core.common.SocketIOProtocolException;
import org.kgusarov.socketio.core.protocol.EngineIOProtocol;
import org.kgusarov.socketio.core.protocol.SocketIOProtocol;
import org.kgusarov.socketio.core.server.TransportProvider;
import org.kgusarov.socketio.core.server.UnsupportedTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
@SuppressWarnings("NonSerializableFieldInSerializableClass")
public abstract class SocketIOServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(SocketIOServlet.class);

    private final SocketIOManager socketIOManager = new SocketIOManager();

    /**
     * Initializes and retrieves the given Namespace by its pathname identifier {@code id}.
     * <p/>
     * If the namespace was already initialized it returns it right away.
     *
     * @param id namespace id
     * @return namespace object
     */
    public Namespace of(final String id) {
        return namespace(id);
    }

    /**
     * Initializes and retrieves the given Namespace by its pathname identifier {@code id}.
     * <p/>
     * If the namespace was already initialized it returns it right away.
     *
     * @param id namespace id
     * @return namespace object
     */
    public Namespace namespace(final String id) {
        Namespace ns = socketIOManager.getNamespace(id);
        if (ns == null) {
            ns = socketIOManager.createNamespace(id);
        }

        return ns;
    }

    public void setTransportProvider(final TransportProvider transportProvider) {
        socketIOManager.setTransportProvider(transportProvider);
    }

    @Override
    public void init() throws ServletException {
        of(SocketIOProtocol.DEFAULT_NAMESPACE);
        LOGGER.info("Socket.IO server stated.");
    }

    @Override
    public void destroy() {
        socketIOManager.getTransportProvider().destroy();
        super.destroy();
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        serve(req, resp);
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        serve(req, resp);
    }

    @Override
    protected void doOptions(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        serve(req, resp);
    }

    @SuppressWarnings("NestedMethodCall")
    private void serve(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        assert socketIOManager.getTransportProvider() != null;

        try {
            LOGGER.trace("Request from " +
                    request.getRemoteHost() + ':' + request.getRemotePort() +
                    ", transport: " + request.getParameter(EngineIOProtocol.TRANSPORT) +
                    ", EIO protocol version:" + request.getParameter(EngineIOProtocol.VERSION));

            socketIOManager.getTransportProvider()
                    .getTransport(request)
                    .handle(request, response, socketIOManager);
        } catch (UnsupportedTransportException | SocketIOProtocolException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);

            LOGGER.warn("Socket IO error", e);
        }
    }
}
