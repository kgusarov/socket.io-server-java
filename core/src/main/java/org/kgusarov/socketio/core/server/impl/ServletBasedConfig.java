package org.kgusarov.socketio.core.server.impl;

import org.kgusarov.socketio.core.server.Config;

import javax.servlet.ServletConfig;

/**
 * @author Mathieu Carbou
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public final class ServletBasedConfig implements Config {
    private final ServletConfig config;
    private final String namespace;

    public ServletBasedConfig(final ServletConfig config, final String namespace) {
        this.namespace = namespace;
        this.config = config;
    }

    @Override
    public long getPingInterval(final long def) {
        return getLong(PING_INTERVAL, def);
    }

    @Override
    public long getTimeout(final long def) {
        return getLong(TIMEOUT, def);
    }

    @Override
    public int getBufferSize() {
        return getInt(BUFFER_SIZE, DEFAULT_BUFFER_SIZE);
    }

    @Override
    public int getMaxIdle() {
        return getInt(MAX_IDLE, DEFAULT_MAX_IDLE);
    }

    @Override
    public int getInt(final String param, final int def) {
        final String v = getString(param);
        return v == null ? def : Integer.parseInt(v);
    }

    @Override
    public long getLong(final String param, final long def) {
        final String v = getString(param);
        return v == null ? def : Long.parseLong(v);
    }

    @Override
    public boolean getBoolean(final String key, final boolean def) {
        final String v = getString(key);
        return v == null ? def : Boolean.parseBoolean(v);
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public String getString(final String param) {
        String v = config.getInitParameter(namespace + '.' + param);
        if (v == null)
            v = config.getInitParameter(param);

        return v;
    }

    @Override
    public String getString(final String param, final String def) {
        final String v = getString(param);
        return v == null ? def : v;
    }

    @Override
    public int getAckTimeout() {
        return getInt(ACK_TIMEOUT, DEFAULT_ACK_TIMEOUT);
    }
}
