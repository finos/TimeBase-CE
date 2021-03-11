package com.epam.deltix.qsrv.hf.blocks;

import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.TimeStampedMessage;

public class TimeEntry implements TimeIdentity {
    public long                         timestamp;
    public String                       symbol;

    public  TimeEntry() {
    }

    public  TimeEntry(IdentityKey id, long timestamp) {
        this.symbol = id.getSymbol().toString();
        this.timestamp = timestamp;
    }

    @Override
    public TimeEntry            get(IdentityKey id) {
        return this;        
    }

    @Override
    public TimeIdentity         create(IdentityKey id) {
        return new TimeEntry(id, TimeStampedMessage.TIMESTAMP_UNKNOWN);
    }

    @Override
    public CharSequence         getSymbol() {
        return symbol;
    }

    @Override
    public long                 getTime() {
        return timestamp;
    }

    @Override
    public void                 setTime(long timestamp) {
        this.timestamp = timestamp;
    }
}