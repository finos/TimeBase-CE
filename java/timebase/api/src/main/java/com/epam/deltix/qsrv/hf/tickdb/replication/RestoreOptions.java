package com.epam.deltix.qsrv.hf.tickdb.replication;

import com.epam.deltix.timebase.messages.IdentityKey;

/**
 *
 */
public class RestoreOptions extends CommonOptions {

    public RestoreOptions() {
    }

    public RestoreOptions(String name) {
        this.name = name;
    }

    public String name; // stream name
}
