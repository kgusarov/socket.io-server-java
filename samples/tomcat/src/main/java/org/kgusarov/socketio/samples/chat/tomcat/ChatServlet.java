package org.kgusarov.socketio.samples.chat.tomcat;

import org.kgusarov.socketio.extensions.tomcat.TomcatSocketIOServlet;
import org.kgusarov.socketio.samples.chat.ChatSocketServer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

/**
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public class ChatServlet extends TomcatSocketIOServlet {
    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);
        new ChatSocketServer().init(this);
    }
}
