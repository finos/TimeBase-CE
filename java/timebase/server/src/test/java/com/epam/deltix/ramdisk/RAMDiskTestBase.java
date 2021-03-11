package com.epam.deltix.ramdisk;

import com.epam.deltix.util.concurrent.UncheckedInterruptedException;
import com.epam.deltix.util.lang.Util;
import org.junit.*;
import com.epam.deltix.util.io.*;
import java.io.*;
import java.lang.management.ThreadInfo;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 *
 */
public class RAMDiskTestBase {
    protected static final boolean  VERBOSE = false; // || !Boolean.getBoolean ("quiet");

    protected static final int      HEADER_LENGTH = 1 << 10;
    protected static final byte     FILLER = '~';
    protected static final int      INIT_NUM_ROWS = 10000;
    protected static final int      ROW_LENGTH = 37; // must be a prime number
    protected static final int      MAX_NUM_PAGES =
        (INIT_NUM_ROWS * ROW_LENGTH + RAMDisk.PAGE_SIZE - 1) / RAMDisk.PAGE_SIZE;
    
    private final File              workFolder = Home.getFile("temp" + File.separator + new GUID().toString() + File.separator + "ramdisktest");

    private RAMDisk                 ramdisk;

    static class TestFD extends FD {
        private byte []     header = new byte [HEADER_LENGTH];

        public TestFD (RAMDisk ramdisk, File file) {
            super (ramdisk, file);
        }

        @Override
        public long                 getPrivateHeaderLength () {
            return HEADER_LENGTH;
        }
        
        @Override
        protected void              onCommitLength () throws IOException {
            byte []     b = String.format ("%d", getLogicalLength ()).getBytes ();
            int         n = b.length;

            System.arraycopy (b, 0, header, 0, n);

            for (int ii = n; ii < 22; ii++)
                header [ii] = FILLER;

            directWrite (0, header, 0, 22);
        }

        @Override
        protected void              onOpen () throws IOException {
            directRead (0, header, 0, HEADER_LENGTH);

            int         n = 0;
            long        length = 0;

            for (;;) {
                byte    b = header [n++];

                if (b == FILLER)
                    break;

                int     d = b - '0';

                assert d >= 0 && d <= 9 : "Illegal header byte " + b;

                length = length * 10 + d;
            }

            setLogicalLength (length);
        }

        @Override
        protected void              onFormat () throws IOException {
            Arrays.fill (header, FILLER);

            for (int ii = 59; ii < HEADER_LENGTH; ii += 60)
                header [ii] = '\n';
            
            header [HEADER_LENGTH - 1] = '\n';

            directWrite (0, header, 0, HEADER_LENGTH);

            setLogicalLength (HEADER_LENGTH);
        }
    }

    static class TestThread extends Thread {
        private final RAMDisk       ramdisk;
        private final File          file;
        private final int           id;
        private TestFD              fd;
        private final byte []       row = new byte [ROW_LENGTH];
        private final byte []       test = new byte [ROW_LENGTH];
        private Random              random;
        private int                 numRows = 0;
        private final CountDownLatch    ticker;

        public TestThread (int id, RAMDisk ramdisk, File file, CountDownLatch ticker) {
            super ("Test Thread for " + file);

            this.id = id;
            this.ramdisk = ramdisk;
            this.file = file;

            random = new Random (id);

            for (int ii = 0; ii < ROW_LENGTH; ii++)
                row [ii] = byteAt (ii);

            this.ticker = ticker;
        }

        private int                 expectedLength () {
            return (HEADER_LENGTH + ROW_LENGTH * numRows);
        }

        private void                open (boolean format) throws IOException {
            if (VERBOSE)
                System.out.println (id + ": open");

            fd = new TestFD (ramdisk, file);

            if (format)
                fd.format ();
            else {
                fd.open (false);
                
                try {
                    fd.warmUp ();
                } catch (NoFreePagesException x) {
                    // ignore.
                }
            }
        }

        private void                close () throws IOException, InterruptedException {
            if (VERBOSE)
                System.out.println (id + ": close");

            fd.close ();
            fd = null;
        }

        private byte                byteAt (long pos) {
            int     idx = (int) (pos % ROW_LENGTH);

            if (idx == (ROW_LENGTH - 1))
                return ('\n');
            else
                return ((byte) ('a' + (id + idx * 19) % 26));
        }

        private void                blockFill () throws IOException {
            if (VERBOSE)
                System.out.println (id + ": blockFill");

            for (int ii = 0; ii < INIT_NUM_ROWS; ii++)
                fd.write (HEADER_LENGTH + ROW_LENGTH * ii, row, 0, ROW_LENGTH);

            numRows = INIT_NUM_ROWS;
        }

        private void                checkLength () throws IOException {
            assertEquals (id + ": length", expectedLength (), fd.getLogicalLength ());
        }

        private void                randomCheckBytes () throws IOException {
            if (VERBOSE)
                System.out.println (id + ": randomCheckBytes");

            int        len = expectedLength () - HEADER_LENGTH;
            
            for (int ii = 0; ii < 1000; ii++) {
                long    pos = random.nextInt (len);
                long    address = HEADER_LENGTH + pos;
                int     b = fd.read (address);

                assertEquals (id + ": byte at " + address, byteAt (pos), b);
            }
        }

        private void                checkBlock (int idx) throws IOException {
            long    address = HEADER_LENGTH + idx * ROW_LENGTH;
            int     n = fd.read (address, test, 0, ROW_LENGTH);

            assertEquals (
                id + ": read (" + address + ", byte [], 0, " +
                    ROW_LENGTH + ") result [row #" + idx + "]",
                n,
                ROW_LENGTH
            );

            assertArrayEquals (id + ": block at " + address, row, test);
        }

        private void                sequentialCheckBlocks () throws IOException {
            if (VERBOSE)
                System.out.println (id + ": sequentialCheckBlocks");

            for (int ii = numRows - 1; ii >= 0; ii--)
                checkBlock (ii);
        }

        private void                randomCheckBlocks () throws IOException {
            if (VERBOSE)
                System.out.println (id + ": randomCheckBlocks");

            for (int ii = 0; ii < 1000; ii++) 
                checkBlock (random.nextInt (numRows));
        }

        private void                fullChecks () throws IOException {
            checkLength ();
            randomCheckBytes ();
            sequentialCheckBlocks ();
            randomCheckBlocks ();
        }

        private void                truncate (int newNumRows) throws IOException {
            fd.truncate (HEADER_LENGTH + newNumRows * ROW_LENGTH);
            numRows = newNumRows;
        }

        private void                randomWriteBytes () throws IOException {
            if (VERBOSE)
                System.out.println (id + ": randomWriteBytes");

            int        len = expectedLength () - HEADER_LENGTH;

            for (int ii = 0; ii < 1000; ii++) {
                long    pos = random.nextInt (len);
                long    address = HEADER_LENGTH + pos;

                fd.write (address, byteAt (pos));
            }
        }

        @Override
        public void                 run () {
            try {
                open (true);

                assertEquals (id + ": length", expectedLength (), fd.getLogicalLength ());
                
                blockFill ();
                fullChecks ();                
                                
                truncate (numRows / 3);
                fullChecks ();

                randomWriteBytes ();
                sequentialCheckBlocks ();
                //
                //  Make sure we survive re-open
                //
                close ();
                open (false);

                fullChecks ();

                fd.trimToSize ();

                fullChecks ();

                close ();
            } catch (IOException iox) {
                throw new com.epam.deltix.util.io.UncheckedIOException(iox);
            } catch (InterruptedException ix) {
                throw new UncheckedInterruptedException (ix);
            } finally {
                ticker.countDown();
                //if (fd != null)
                //    fd.closeNoSave ();
            }
        }
    }

    @Before
    public void         setup () throws IOException {
        workFolder.mkdirs ();
        IOUtil.removeRecursive (workFolder, null, false);
    }
    
    @After
    public void         teardown () throws InterruptedException {
        System.out.println("Shutting down RAM disk"); // gross attempt to pinpoint timeout source
        if (ramdisk != null) {
            ramdisk.shutdownAndWait ();
            ramdisk = null;
        }
    }

    protected void      runTest (
            int                 numThreads,
            int                 maxNumOpenFiles,
            long                numPages
    ) throws InterruptedException
    {
        runTest(numThreads, maxNumOpenFiles, numPages, -1);
    }

    protected void      runTest (
        int                 numThreads,
        int                 maxNumOpenFiles,
        long                numPages,
        long                timeout
    )
        throws InterruptedException
    {
        ramdisk =
            numPages == 0 ?
                RAMDisk.createNonCached (maxNumOpenFiles) :
                RAMDisk.createCacheByNumPages (maxNumOpenFiles, numPages, 0);

        ramdisk.start ();

        final CountDownLatch counter = new CountDownLatch(numThreads);

        TestThread []   tts = new TestThread [numThreads];

        for (int ii = 0; ii < tts.length; ii++) {
            tts [ii] = new TestThread (ii, ramdisk, new File (workFolder, ii + ".txt"), counter);
            tts [ii].start ();
        }

        boolean success = true;
        if (timeout > 0)
            success = counter.await(timeout, TimeUnit.MILLISECONDS);
        else
            counter.await();

        if (!success) {
            System.out.println("Grabbing Thread Dump - timeout exceeded");

            Map<Thread, ThreadInfo> threads = Util.getAllStackTraces();
            for (ThreadInfo info : threads.values())
                System.out.println(info);
        }
    }
}
