package org.kgusarov.socketio.samples.chat.jetty;

import org.testatoo.container.ContainerRunner;

/**
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public final class Server {
    private Server() {
    }

    public static void main(final String[] args) throws Exception {
        ContainerRunner.main(
                "-container", "jetty9",
                "-webappRoot", "./samples/jetty/target/classes/webapp"
        );
    }
}
