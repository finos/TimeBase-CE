package com.epam.deltix.temp.ramdisktests;

import java.io.*;
import com.epam.deltix.ramdisk.*;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.memory.DataExchangeUtils;
import com.epam.deltix.util.time.TimeKeeper;
import java.util.Arrays;

class FDCustom extends FD {
    static final int        HEADER_LENGTH = 64;
    static final int        LENGTH_OFFSET = 16;
    static final long       COMMIT_DELAY = 5000;

    private byte []     header = new byte [HEADER_LENGTH];
    private long        lastCommit = TimeKeeper.currentTime;

    public FDCustom (RAMDisk ramdisk, File file) {
        super (ramdisk, file);
    }

    @Override
    public boolean              getAutoCommit () {
        return (true);
    }

    private void                writeLength (long length) throws IOException {
        DataExchangeUtils.writeLong (header, LENGTH_OFFSET, length);
        directWrite (LENGTH_OFFSET, header, LENGTH_OFFSET, 8);
    }

    @Override
    protected void              onCleanCommit (long cleanLength) 
        throws IOException
    {
        long    now = TimeKeeper.currentTime;

        if (now - lastCommit < COMMIT_DELAY)
            return;

        lastCommit = now;
        
        System.out.println (this + " is clean up to " + cleanLength);
        
        //
        //  Flush all caches before storing new clean length.
        //
        directForce ();
        writeLength (cleanLength);

        System.out.println ("    commit complete.");
    }

    @Override
    protected void              onTruncate ()
        throws IOException
    {
        //
        //  Write new length and flush all caches before beginning to overwrite
        //  data in the middle of the file.
        //
        onCommitLength ();
        directForce ();
    }

    @Override
    public long                 getPrivateHeaderLength () {
        return HEADER_LENGTH;
    }

    @Override
    protected void              onCommitLength () throws IOException {
        writeLength (getLogicalLength ());
    }

    @Override
    protected void              onOpen () throws IOException {
        directRead (0, header, 0, HEADER_LENGTH);
        setLogicalLength (DataExchangeUtils.readLong (header, 0));
    }

    @Override
    protected void              onFormat () throws IOException {
        Arrays.fill (header, (byte) 0);
        DataExchangeUtils.writeLong (header, LENGTH_OFFSET, HEADER_LENGTH);

        directWrite (0, header, 0, HEADER_LENGTH);

        setLogicalLength (HEADER_LENGTH);
    }
}

public class TestWrite {
    private static final int    NS = 100;
    private static final int    NM = 10000000;

    public static void main (String [] args) throws Exception {
        RAMDisk     ramdisk = RAMDisk.createCacheByNumPages (Integer.MAX_VALUE, 10000, 0);

        ramdisk.start ();

        byte []     msg = new byte [23];

        Arrays.fill (msg, (byte) 111);

        FD []       fds = new FD [NS];
        long []     pos = new long [NS];

        for (int ii = 0; ii < NS; ii++) {
            File        f = Home.getFile ("temp/ramdisktest/test" + ii + ".dat");
            f.delete ();
            fds [ii] = new FDCustom (ramdisk, f);
            fds [ii].format ();
            pos [ii] = FDCustom.HEADER_LENGTH;
        }

        long        t0 = System.currentTimeMillis ();

        for (int ii = 0; ii < NM; ii++) {
            int     is = ii % NS;
            
            fds [is].write (pos [is], msg, 0, msg.length);
            pos [is] += msg.length;
        }                

        long        t1 = System.currentTimeMillis ();

        System.out.printf ("Load: %,d\n", t1 - t0);

        for (int ii = 0; ii < NS; ii++)
            fds [ii].close ();

        long        t2 = System.currentTimeMillis ();

        System.out.printf ("Total: %,d\n", t2 - t0);

        ramdisk.shutdownAndWait ();
    }
}
