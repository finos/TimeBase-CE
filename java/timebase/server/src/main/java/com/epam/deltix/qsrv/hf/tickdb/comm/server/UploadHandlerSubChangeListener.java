package com.epam.deltix.qsrv.hf.tickdb.comm.server;

import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingError;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingErrorListener;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.SubscriptionChangeListener;
import com.epam.deltix.util.vsocket.ChannelClosedException;
import com.epam.deltix.util.vsocket.ConnectionAbortedException;
import com.epam.deltix.util.vsocket.VSChannel;
import com.epam.deltix.util.vsocket.VSChannelState;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;

/**
 * @author Alexei Osipov
 */
public class UploadHandlerSubChangeListener implements LoadingErrorListener, SubscriptionChangeListener {
    private final VSChannel ds;
    private final DataOutputStream out;
    private final boolean binary;
    private final CloseEventCallback closeCommandListener;

    public UploadHandlerSubChangeListener(VSChannel ds, boolean binary, CloseEventCallback closeCommandListener) {
        this.ds = ds;
        this.binary = binary;
        this.out = ds.getDataOutputStream();
        this.closeCommandListener = closeCommandListener;
    }

    @Override
    public void entitiesAdded(Collection<IdentityKey> entities) {
        try {
            synchronized (out) {
                out.writeInt(TDBProtocol.LOADRESP_ENTITIES_CHANGE);
                out.writeBoolean(false); // all entities
                out.writeBoolean(true); // entities added
                out.writeInt(entities.size()); // entities size
                for (IdentityKey id : entities)
                    TDBProtocol.writeIdentityKey(id, out);
                out.flush();
            }
        } catch (IOException ex) {
            onException(ex);
        }
    }

    @Override
    public void entitiesRemoved(Collection<IdentityKey> entities) {
        try {
            synchronized (out) {
                out.writeInt(TDBProtocol.LOADRESP_ENTITIES_CHANGE);
                out.writeBoolean(false); // all entities
                out.writeBoolean(false); // entities added
                out.writeInt(entities.size()); // entities size
                for (IdentityKey id : entities)
                    TDBProtocol.writeIdentityKey(id, out);
                out.flush();
            }
        } catch (IOException ex) {
            onException(ex);
        }
    }

    @Override
    public void allEntitiesAdded() {
        try {
            synchronized (out) {
                out.writeInt(TDBProtocol.LOADRESP_ENTITIES_CHANGE);
                out.writeBoolean(true); // all entities
                out.writeBoolean(true); // added
                out.flush();
            }
        } catch (IOException ex) {
            onException(ex);
        }
    }

    @Override
    public void allEntitiesRemoved() {
        try {
            synchronized (out) {
                out.writeInt(TDBProtocol.LOADRESP_ENTITIES_CHANGE);
                out.writeBoolean(true); // all entities
                out.writeBoolean(false); // added
                out.flush();
            }
        } catch (IOException ex) {
            onException(ex);
        }
    }

    @Override
    public void typesAdded(Collection<String> types) {
        try {
            synchronized (out) {
                out.writeInt(TDBProtocol.LOADRESP_TYPES_CHANGE);
                out.writeBoolean(false); // all entities
                out.writeBoolean(true); // entities added
                out.writeInt(types.size()); // entities size
                for (String type : types)
                    out.writeUTF(type);
                out.flush();
            }
        } catch (IOException ex) {
            onException(ex);
        }
    }

    @Override
    public void typesRemoved(Collection<String> types) {
        try {
            synchronized (out) {
                out.writeInt(TDBProtocol.LOADRESP_TYPES_CHANGE);
                out.writeBoolean(false); // all entities
                out.writeBoolean(false); // entities added
                out.writeInt(types.size()); // entities size
                for (String type : types)
                    out.writeUTF(type);
                out.flush();
            }
        } catch (IOException ex) {
            onException(ex);
        }
    }

    @Override
    public void allTypesAdded() {
        try {
            synchronized (out) {
                out.writeInt(TDBProtocol.LOADRESP_TYPES_CHANGE);
                out.writeBoolean(true); // all entities
                out.writeBoolean(true); // added
                out.flush();
            }
        } catch (IOException ex) {
            onException(ex);
        }
    }

    @Override
    public void allTypesRemoved() {
        try {
            synchronized (out) {
                out.writeInt(TDBProtocol.LOADRESP_TYPES_CHANGE);
                out.writeBoolean(true); // all entities
                out.writeBoolean(false); // added
                out.flush();
            }
        } catch (IOException ex) {
            onException(ex);
        }
    }

    private void onException(IOException ex) {
        if (ex instanceof ConnectionAbortedException) {
            TickDBServer.LOGGER.log(Level.WARNING, "Client unexpectedly drop connection");
        } else {
            if (ds.getState() == VSChannelState.Connected)
                throw new com.epam.deltix.util.io.UncheckedIOException(ex);
        }
    }

    public void onError(LoadingError e) {
        try {
            synchronized (out) {
                out.writeInt(TDBProtocol.LOADRESP_ERROR);
                if (binary)
                    TDBProtocol.writeBinary(out, e);
                else
                    TDBProtocol.writeError(out, e);
            }
        } catch (ChannelClosedException ex) {
            TickDBServer.LOGGER.finest("Client disconnect");
            closeCommandListener.close();
        } catch (IOException ex) {
            if (ds.getState() == VSChannelState.Connected)
                throw new com.epam.deltix.util.io.UncheckedIOException(ex);
        }
    }

    public interface CloseEventCallback {
        void close();
    }
}
