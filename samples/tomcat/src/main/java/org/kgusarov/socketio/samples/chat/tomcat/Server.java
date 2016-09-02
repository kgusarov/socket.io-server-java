package org.kgusarov.socketio.samples.chat.tomcat;

import org.testatoo.container.ContainerRunner;

/**
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public final class Server {
    private Server() {
    }

    public static void main(final String[] args) throws Exception {
        ContainerRunner.main(
                "-container", "tomcat7",
                "-webappRoot", "./samples/tomcat/target/classes/webapp"
        );
    }
}
