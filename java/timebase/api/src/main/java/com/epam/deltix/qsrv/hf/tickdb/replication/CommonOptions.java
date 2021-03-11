package com.epam.deltix.qsrv.hf.tickdb.replication;

import com.epam.deltix.timebase.messages.IdentityKey;

/**
 *
 */
public class CommonOptions {

    public IdentityKey[] entities; // selected entities

    public String[]             types; // selected entities

    public long[]               range; // selected time range

    public ReloadMode           mode = ReloadMode.allow;

    public int                  retries = 0;    // number of reconnect attempts

    public long                 retryTimeout = 5000; // period of time in milliseconds between reconnect attempts

    public boolean              async = false; // run replication process asynchronously in separate thread
}
