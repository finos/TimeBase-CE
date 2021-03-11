package com.epam.deltix.qsrv.hf.tickdb.replication;

/**
 *
 */
public class ReplicationOptions extends CommonOptions {

    public boolean  live = false;

    public long     rollSize = 100;
    public long     threshold = 100000;
    public boolean  format = false;
    public int      flush = 0;      // flush loader every 'flush' sends
}
