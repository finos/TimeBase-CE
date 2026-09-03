package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.hf.spi.conn.DisconnectEventListener;
import com.epam.deltix.qsrv.hf.spi.conn.Disconnectable;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Same as {@link com.epam.deltix.qsrv.hf.spi.conn.DisconnectableEventHandler} but catches and logs exceptions from listeners
 * instead of propagating them.
 * <p>
 * Helps implement the {@link Disconnectable} interface.
 * <p> Doesn't maintain a connection status, so <code>isConnected</code> must be implemented by a client.</p>
 */
class SafeDisconnectableEventHandler implements Disconnectable {
    public static final Log LOGGER = LogFactory.getLog(SafeDisconnectableEventHandler.class);

    private final CopyOnWriteArrayList<DisconnectEventListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void addDisconnectEventListener(DisconnectEventListener listener) {
        listeners.addIfAbsent(listener);
    }

    @Override
    public void removeDisconnectEventListener(DisconnectEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public boolean isConnected() {
        throw new UnsupportedOperationException();
    }

    public void onReconnected() {
        for (DisconnectEventListener listener : listeners) {
            try {
                listener.onReconnected();
            } catch (Throwable t) {
                LOGGER.error("Error processing reconnect event: %s").with(t);
                if (!(t instanceof Exception)) {
                    throw t;
                }
            }
        }
    }

    public void onDisconnected() {
        for (DisconnectEventListener listener : listeners) {
            try {
                listener.onDisconnected();
            } catch (Throwable t) {
                LOGGER.error("Error processing reconnect event: %s").with(t);
                if (!(t instanceof Exception)) {
                    throw t;
                }
            }
        }
    }
}
