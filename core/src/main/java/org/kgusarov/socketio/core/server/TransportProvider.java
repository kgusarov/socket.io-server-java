package org.kgusarov.socketio.core.server;

import org.kgusarov.socketio.core.common.SocketIOProtocolException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import java.util.Collection;

/**
 * Transport factory
 *
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public interface TransportProvider {

    /**
     * Creates all the transports
     *
     * @param config  servlet configuration
     * @param context servlet context
     * @throws ServletException if init failed
     */
    void init(ServletConfig config, ServletContext context) throws ServletException;

    void destroy();

    /**
     * Finds appropriate Transport class based on the rules defined at
     * https://github.com/socketio/engine.io-protocol#transports
     *
     * @param request incoming servlet request
     * @return appropriate Transport object
     * @throws UnsupportedTransportException no transport was found
     * @throws org.kgusarov.socketio.core.common.SocketIOProtocolException   invalid request was sent
     */
    Transport getTransport(ServletRequest request) throws UnsupportedTransportException, SocketIOProtocolException;

    Transport getTransport(TransportType type);

    Collection<Transport> getTransports();
}
