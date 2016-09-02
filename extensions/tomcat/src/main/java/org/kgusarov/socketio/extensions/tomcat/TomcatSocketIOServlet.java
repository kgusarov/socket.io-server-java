package org.kgusarov.socketio.extensions.tomcat;

import org.kgusarov.socketio.core.server.TransportProvider;
import org.kgusarov.socketio.core.server.impl.SocketIOServlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public abstract class TomcatSocketIOServlet extends SocketIOServlet {
    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);

        final TransportProvider transportProvider = new TomcatTransportProvider();
        final ServletContext ctx = getServletContext();
        transportProvider.init(config, ctx);
        setTransportProvider(transportProvider);
    }
}
