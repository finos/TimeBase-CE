package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.ramdisk.FD;
import com.epam.deltix.util.concurrent.ThrottlingExecutor;
import com.epam.deltix.util.lang.GrowthPolicy;
import net.jcip.annotations.GuardedBy;

import java.io.*;
import java.util.logging.Level;

/**
 *
 */
class LinearFile extends FD {
    private final TickDBImpl        db;

    @GuardedBy("this")
    private volatile long           cleanLength;

    private class FlusherTask extends ThrottlingExecutor.Task {
        volatile boolean canceled = false;

        @Override
        public boolean run() throws InterruptedException {
            if (canceled || !isOpen())
                return false;

            try {
                long length = cleanLength;
                cleanCommit();
                return length != cleanLength;
            } catch (Throwable e) {
                if (isOpen())
                    TickDBImpl.LOGGER.log(Level.WARNING, "Error while committing length", e);

                return false;
            }
        }
    }

    private final FlusherTask flusher = new FlusherTask();

    LinearFile (TickDBImpl db, File file) {
        super (db.ramdisk, file);

        this.db = db;
    }

    @Override
    public String               toString () {
        return (getFile ().toString ());
    }

    @Override
    public GrowthPolicy         getGrowthPolicy () {
        return db.growthPolicy;
    }

    @Override
    public boolean              getAutoCommit () {
        return (true);
    }

    private boolean             cleanCommit() throws IOException {
        if (!isOpen())
            return false;

        directForce ();
        onCommitLength (cleanLength);

        if (TickDBImpl.LOGGER.isLoggable (Level.FINE))
            TickDBImpl.LOGGER.fine ("Committed " + this + "; length = " + cleanLength);

        return true;
    }

    protected void              onCommitLength (long length) throws IOException {
    }

    @Override
    protected void              onCleanCommit (long length)
            throws IOException
    {
        boolean update;

        // file may be truncated in parallel, so prevent to set clean length > logical length
        synchronized (flusher) {
            update = length <= getLogicalLength();
            if (update)
                cleanLength = length;
        }

        if (update)
            flusher.submit(db.saver);
    }

    @Override
    protected void              onTruncate ()
            throws IOException
    {
        synchronized (flusher) {
            cleanLength = getLogicalLength(); // set correct length for the flusher
        }

        //
        //  Write new length and flush all caches before beginning to overwrite
        //  data in the middle of the file.
        //
        onCommitLength ();
        directForce ();

        if (TickDBImpl.LOGGER.isLoggable (Level.FINE))
            TickDBImpl.LOGGER.fine ("Truncated " + this);
    }

    @Override
    public void                 close() {
        flusher.canceled = true;
        super.close();
    }
}
