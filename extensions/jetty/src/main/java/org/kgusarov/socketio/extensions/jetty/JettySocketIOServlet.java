package org.kgusarov.socketio.extensions.jetty;

import org.kgusarov.socketio.core.server.TransportProvider;
import org.kgusarov.socketio.core.server.impl.SocketIOServlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public abstract class JettySocketIOServlet extends SocketIOServlet {
    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);

        final TransportProvider transportProvider = new JettyTransportProvider();
        final ServletContext ctx = getServletContext();
        transportProvider.init(config, ctx);
        setTransportProvider(transportProvider);
    }
}
