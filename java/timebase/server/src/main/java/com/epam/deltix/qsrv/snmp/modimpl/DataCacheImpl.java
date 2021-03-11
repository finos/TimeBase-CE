package com.epam.deltix.qsrv.snmp.modimpl;

import com.epam.deltix.qsrv.hf.tickdb.pub.mon.PropertyMonitor;
import com.epam.deltix.qsrv.snmp.model.timebase.Failure;
import com.epam.deltix.qsrv.snmp.model.timebase.TBDataCache;
import com.epam.deltix.snmp.pub.SNMP;
import com.epam.deltix.snmp.pub.Table;

import static com.epam.deltix.ramdisk.RAMDisk.Properties;

/**
 *
 */
public class DataCacheImpl implements TBDataCache, PropertyMonitor {
    
    private volatile int sequence = 0;

    private final Table<Failure> errors = SNMP.createTable(Failure.class);
    private int cacheSize = 0;
    private int usedCacheSize;
    private int numPages;
    private int numOpenedFiles;
    private int bytesWritten;
    private int bytesRead;
    private int queueLength;
    private String writerState = "";

    @Override
    public int getCacheSize() {
        return cacheSize;
    }

    @Override
    public int getUsedCacheSize() {
        return usedCacheSize;
    }

    @Override
    public int getNumPages() {
        return numPages;
    }

    @Override
    public int getNumOpenFiles() {
        return numOpenedFiles;
    }

    @Override
    public int getNumWriteBytes() {
        return bytesWritten;
    }

    @Override
    public int getNumReadBytes() {
        return bytesRead;
    }

    @Override
    public int getWriteQueueLength() {
        return queueLength;
    }

    @Override
    public String getWriterState() {
        return writerState;
    }

    @Override
    public Table<Failure> getIOFailures() {
        return errors;
    }

    @Override
    public void propertyChanged(String owner, String property, Object newValue) {
        Properties name = Properties.valueOf(property);

        switch (name) {
            case bytesRead:
                bytesRead = (Integer) newValue;
                break;

            case bytesWritten:
                bytesWritten = (Integer) newValue;
                break;

            case cacheSize:
                cacheSize = ((Long) newValue).intValue();
                break;

            case usedCacheSize:
                usedCacheSize = ((Long) newValue).intValue();
                break;

            case numPages:
                numPages = ((Long) newValue).intValue();
                break;

            case numOpenedFiles:
                numOpenedFiles = (Integer) newValue;
                break;

            case queueLength:
                queueLength = (Integer) newValue;
                break;

            case writerState:
                writerState = (String) newValue;
                break;

            case failures:
                if (newValue != null)
                    errors.add(new FailureImpl(sequence++, (Exception)newValue));
                break;
        }

    }
}
