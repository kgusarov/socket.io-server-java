package org.kgusarov.socketio.core.server;

/**
 * @author Mathieu Carbou
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public interface Config {
    String MAX_TEXT_MESSAGE_SIZE = "maxTextMessageSize";
    String PING_INTERVAL = "pingInterval";
    String TIMEOUT = "timeout";
    String BUFFER_SIZE = "bufferSize";
    String MAX_IDLE = "maxIdleTime";
    String ACK_TIMEOUT = "ackTimeout";

    int DEFAULT_BUFFER_SIZE = 8192;
    int DEFAULT_MAX_IDLE = 300 * 1000;

    int DEFAULT_PING_INTERVAL = 25 * 1000;
    int DEFAULT_PING_TIMEOUT = 60 * 1000;

    int DEFAULT_ACK_TIMEOUT = 60 * 1000;

    long getPingInterval(long def);

    long getTimeout(long def);

    int getBufferSize();

    int getMaxIdle();

    String getString(String key);

    String getString(String key, String def);

    int getInt(String key, int def);

    long getLong(String key, long def);

    boolean getBoolean(String key, boolean def);

    String getNamespace();

    int getAckTimeout();
}
