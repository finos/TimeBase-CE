package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.timebase.messages.IdentityKey;

import java.util.Collection;

/**
 *
 */
public interface StreamStateListener {

    public void changed(DXTickStream stream, int property);

    public void writerCreated(DXTickStream stream, IdentityKey[] ids);

    public void writerClosed(DXTickStream stream, IdentityKey[] ids);

    public void created(DXTickStream stream);

    public void renamed(DXTickStream stream, String key);

    public void deleted(DXTickStream stream);
}