package org.kgusarov.socketio.core.common;

/**
 * @author Alexander Sova       (bird@codeminders.com)
 * @author Konstantin Gusarov   (konstantins.gusarovs@gmail.com)
 */
public enum DisconnectReason {
	UNKNOWN,
	CONNECT_FAILED,            // A connection attempt failed.
	DISCONNECT,                // Disconnect was called explicitly.
	TIMEOUT,                // A timeout occurred.
	CLOSE_FAILED,            // The connection dropped before an orderly close could complete.
	ERROR,                    // A GET or POST returned an error, or an internal error occurred.
	CLOSED_REMOTELY,        // Remote end point initiated a close.
	CLIENT_GONE,            // Remote end point gone away (browser closed or navigated away)
	CLOSED,                    // Locally initiated close succeeded.
	;
}