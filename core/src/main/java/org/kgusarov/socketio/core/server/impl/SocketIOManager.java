package org.kgusarov.socketio.core.server.impl;

import org.kgusarov.socketio.core.server.Config;
import org.kgusarov.socketio.core.server.TransportProvider;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Class to manage Socket.IO sessions and namespaces.
 *
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public final class SocketIOManager {
    private final Map<String, Namespace> namespaces = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<>();
    private TransportProvider transportProvider;

    final ScheduledExecutorService executor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    private String generateSessionId() {
        while (true) {
            final String id = UUID.randomUUID().toString();
            if (sessions.get(id) == null) {
                return id;
            }
        }
    }

    /**
     * Creates new session
     *
     * @return new session
     */
    public Session createSession(final Config config) {
        final String sessionId = generateSessionId();
        final int ackTimeout = config.getAckTimeout();
        final Session session = new Session(this, sessionId, ackTimeout);

        sessions.put(session.getSessionId(), session);

        return session;
    }

    /**
     * Finds existing session
     *
     * @param sessionId session id
     * @return session object or null if not found
     */
    public Session getSession(final String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * Deletes the session
     *
     * @param sessionId session id
     */
    public void deleteSession(final String sessionId) {
        sessions.remove(sessionId);
    }

    /**
     * Creates new namespace
     *
     * @param id namespace in. Should always start with '/'
     * @return new namespace
     */
    public Namespace createNamespace(final String id) {
        final Namespace ns = new Namespace(id);
        namespaces.put(ns.getId(), ns);
        return ns;
    }

    public Namespace getNamespace(final String id) {
        return namespaces.get(id);
    }

    public TransportProvider getTransportProvider() {
        return transportProvider;
    }

    public void setTransportProvider(final TransportProvider transportProvider) {
        this.transportProvider = transportProvider;
    }
}
