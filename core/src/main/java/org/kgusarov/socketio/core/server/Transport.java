package org.kgusarov.socketio.core.server;

import org.kgusarov.socketio.core.server.impl.SocketIOManager;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public interface Transport {
    /**
     * @return The name of the transport.
     */
    TransportType getType();

    /**
     * Initializes the transport
     *
     * @param config  Servlet config
     * @param context Servlet context
     * @throws javax.servlet.ServletException if init fails
     */
    void init(ServletConfig config, ServletContext context) throws ServletException;

    void destroy();

    /**
     * Handles incoming HTTP request
     *
     * @param request         object that contains the request the client made of the servlet
     * @param response        object that contains the response the servlet returns to the client
     * @param socketIOManager session manager
     * @throws java.io.IOException if an input or output error occurs while the servlet is handling the request
     */
    void handle(HttpServletRequest request, HttpServletResponse response, SocketIOManager socketIOManager) throws IOException;

    /**
     * Creates new connection
     *
     * @return new transport connection
     */
    TransportConnection createConnection();
}
