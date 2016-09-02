package org.kgusarov.socketio.core.server.transport;

import org.kgusarov.socketio.core.protocol.EngineIOProtocol;
import org.kgusarov.socketio.core.server.Config;
import org.kgusarov.socketio.core.server.Transport;
import org.kgusarov.socketio.core.server.TransportConnection;
import org.kgusarov.socketio.core.server.impl.ServletBasedConfig;
import org.kgusarov.socketio.core.server.impl.Session;
import org.kgusarov.socketio.core.server.impl.SocketIOManager;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 * @author Mathieu Carbou
 */
public abstract class AbstractTransport implements Transport {
    private Config config;

    @Override
    public void destroy() {
    }

    @Override
    public void init(final ServletConfig config, final ServletContext context) throws ServletException {
        final String namespace = getType().toString();
        this.config = new ServletBasedConfig(config, namespace);
    }

    protected final Config getConfig() {
        return config;
    }

    protected final TransportConnection createConnection(final Session session) {
        final TransportConnection connection = createConnection();
        connection.setSession(session);
        connection.init(getConfig());
        return connection;
    }

    protected TransportConnection getConnection(final HttpServletRequest request, final SocketIOManager sessionManager) {
        final String sessionId = request.getParameter(EngineIOProtocol.SESSION_ID);
        Session session = null;

        if ((sessionId != null) && !sessionId.isEmpty()) {
            session = sessionManager.getSession(sessionId);
        }

        if (session == null) {
            final Session s = sessionManager.createSession(config);
            return createConnection(s);
        }

        final TransportConnection activeConnection = session.getConnection();
        final Transport transport = activeConnection.getTransport();
        if (Objects.equals(transport, this)) {
            return activeConnection;
        }

        // this is new connection considered for an upgrade
        return createConnection(session);
    }

    @Override
    public String toString() {
        return getType().toString();
    }
}
