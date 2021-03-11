package com.epam.deltix.qsrv.snmp.model.timebase;

import com.epam.deltix.snmp.pub.*;

/**
 *
 */
public interface TBDataCache {
    @Id(1)
    @Description ("Total (maximum) data cache size, in MB")
    public int      getCacheSize();

    @Id(2)
    @Description ("Used data cache size, in MB")
    public int      getUsedCacheSize();

    @Id(3)
    @Description ("Number of allocated pages")
    public int      getNumPages();

    @Id(4)
    @Description ("Number of opened files")
    public int      getNumOpenFiles();

    @Id(5)
    @Description ("Number of IO write bytes")
    public int      getNumWriteBytes();

    @Id(6)
    @Description ("Number of IO read bytes")
    public int      getNumReadBytes();

    @Id(7)
    @Description ("Write queue length")
    public int      getWriteQueueLength();

    @Id(8)
    @Description ("Writer Thread state")
    public String   getWriterState();

    @Id(9)
    @Description ("Number of IO failures")
    public Table<Failure> getIOFailures();
}
