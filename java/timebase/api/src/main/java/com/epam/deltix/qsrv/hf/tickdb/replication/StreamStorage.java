package com.epam.deltix.qsrv.hf.tickdb.replication;

import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;

/**
 *
 */
public class StreamStorage implements Storage {
    public DXTickDB     db; // source database
    public String       name; // source stream

    public StreamStorage(DXTickDB db, String stream) {
        this.db = db;
        this.name = stream;
    }

    public DXTickStream getSource() {
        return db.getStream(name);
    }
}
