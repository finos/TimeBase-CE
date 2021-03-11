package com.epam.deltix.qsrv.hf.blocks;

import com.epam.deltix.timebase.messages.IdentityKey;

public interface TimeIdentity extends IdentityKey {

    public TimeIdentity         get(IdentityKey id);
    
    public TimeIdentity         create(IdentityKey id);

    public long                 getTime();
    
    public void                 setTime(long timestamp);
}
