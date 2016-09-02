package org.kgusarov.socketio.core.server.impl;

import org.kgusarov.socketio.core.common.DisconnectReason;
import org.kgusarov.socketio.core.common.SocketIOException;
import org.kgusarov.socketio.core.server.DisconnectListener;
import org.kgusarov.socketio.core.server.EventListener;
import org.kgusarov.socketio.core.server.Outbound;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public class Socket implements Outbound, DisconnectListener, EventListener {
    private final List<DisconnectListener> disconnectListeners = new ArrayList<>();
    private final Map<String, EventListener> eventListeners = new LinkedHashMap<>();

    // Socket is Session + Namespace
    private final Session session;
    private final Namespace namespace;

    public Socket(final Session session, final Namespace namespace) {
        this.session = session;
        this.namespace = namespace;
    }

    public String getNamespace() {
        return namespace.getId();
    }

    /**
     * Set listener for a named event. Only one listener per event is allowed.
     *
     * @param eventName event name
     * @param listener  event listener
     */
    public void on(final String eventName, final EventListener listener) {
        eventListeners.put(eventName, listener);
    }

    /**
     * Closes socket.
     *
     * @param closeConnection closes underlying transport connection if true
     */
    public void disconnect(final boolean closeConnection) {
        final String ns = getNamespace();
        getSession().getConnection().disconnect(ns, closeConnection);
    }

    @Override
    public void emit(final String name, final Object... args) throws SocketIOException {
        final String ns = getNamespace();
        getSession().getConnection().emit(ns, name, args);
    }

    /**
     * Adds disconnect listener
     *
     * @param listener disconnect listener
     */
    public void on(final DisconnectListener listener) {
        disconnectListeners.add(listener);
    }

    public Session getSession() {
        return session;
    }

    @Override
    public void onDisconnect(final Socket socket, final DisconnectReason reason, final String errorMessage) {
        for (final DisconnectListener listener : disconnectListeners) {
            listener.onDisconnect(socket, reason, errorMessage);
        }
    }

    @Override
    public Object onEvent(final String name, final Object[] args, final boolean ackRequested) {
        final EventListener listener = eventListeners.get(name);
        if (listener == null) {
            return null;
        }

        return listener.onEvent(name, args, ackRequested);
    }

    public void join(final String room) {
        namespace.in(room).join(this);
    }

    public void leave(final String room) {
        namespace.in(room).leave(this);
    }

    public void leaveAll() {
        namespace.leaveAll(this);
    }

    public void broadcast(final String room, final String name, final Object... args) throws SocketIOException {
        namespace.in(room).broadcast(this, name, args);
    }

    public String getId() {
        return getSession().getSessionId() + getNamespace();
    }

    /**
     * @return current HTTP request from underlying connection, null if socket is disconnected
     */
    public HttpServletRequest getRequest() {
        return getSession().getConnection().getRequest();
    }
}
