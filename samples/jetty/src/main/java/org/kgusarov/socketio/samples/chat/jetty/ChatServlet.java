package org.kgusarov.socketio.samples.chat.jetty;

import org.kgusarov.socketio.extensions.jetty.JettySocketIOServlet;
import org.kgusarov.socketio.samples.chat.ChatSocketServer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

/**
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public class ChatServlet extends JettySocketIOServlet {
    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);
        new ChatSocketServer().init(this);
    }
}
