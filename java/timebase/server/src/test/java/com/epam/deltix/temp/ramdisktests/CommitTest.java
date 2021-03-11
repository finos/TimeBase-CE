package com.epam.deltix.temp.ramdisktests;

import com.epam.deltix.util.time.TimeKeeper;
import java.io.*;

public class CommitTest {
    private static final int    BLOCK_SIZE = 8192;
    private static final long   NUM_BLOCKS = 1000000;
    private static final double MPS = 500000;
    private static final double MPB = BLOCK_SIZE / 20.0;
    private static final double BPMS = MPS / MPB / 1000.0;
        
    
    static final Object         statsLock = new Object ();
    static long                 maxLatencyThisPeriod = 0;
    static long                 experimentStart;
    static long                 periodStart;
    static int                  numWritesThisPeriod = 0;
    static int                  totalNumWrites = 0;

    static class Committer extends Thread {
        private final RandomAccessFile      raf;

        public Committer (RandomAccessFile raf) {
            super ("Committer");
            this.raf = raf;
        }

        @Override
        public void                         run () {
            for (;;) {
                try {
                    raf.getChannel ().force (false);
                } catch (IOException x) {
                    throw new RuntimeException (x);
                }
                
                try {
                    Thread.sleep (50);
                } catch (InterruptedException x) {
                    break;
                }
            }
        }
    }

    static class StatsPrinter extends Thread {
        public StatsPrinter () {
            super ("StatsPrinter");
        }

        @Override
        public void                         run () {
            System.out.println (
                "Total Time  ,Total Rate  ,Period Time ,Period Rate ,Max Latency"
            );

            for (;;) {
                long            now = TimeKeeper.currentTime;
                int             nwr;
                int             tnwr;
                double          maxl;
                double          totalSeconds;
                double          periodSeconds;

                synchronized (statsLock) {
                    periodSeconds = (now - periodStart) * 0.001;

                    if (periodSeconds == 0)
                        continue;
                    
                    nwr = numWritesThisPeriod;

                    if (nwr == 0)
                        continue;

                    totalSeconds = 0.001 * (now - experimentStart);
                    tnwr = totalNumWrites;
                    maxl = maxLatencyThisPeriod;
                    
                    numWritesThisPeriod = 0;
                    maxLatencyThisPeriod = 0;
                    periodStart = now;                    
                }

                System.out.printf (
                    "%12.3f,%,12d,%12f,%,12d,%12.3f\n",
                    totalSeconds,
                    (long) (tnwr * MPB / totalSeconds),
                    periodSeconds,
                    (long) (nwr * MPB / periodSeconds),
                    maxl * 0.001
                );

                try {
                    Thread.sleep (1000);
                } catch (InterruptedException x) {
                    break;
                }
            }
        }
    }

    static class Writer extends Thread {
        private final RandomAccessFile      raf;

        public Writer (RandomAccessFile raf) {
            super ("Writer");
            this.raf = raf;
        }

        @Override
        public void                         run () {
            byte []                 buffer = new byte [8192];

            synchronized (statsLock) {
                periodStart = experimentStart = TimeKeeper.currentTime;
            }

            try {
                for (long ii = 0; ii < NUM_BLOCKS; ii++) {
                    long            expTime = experimentStart + (long) (ii / BPMS);
                    long            td = expTime - TimeKeeper.currentTime;

                    if (td > 32)
                        Thread.sleep (td);   

                    long            t = TimeKeeper.currentTime;

                    raf.seek (BLOCK_SIZE * ii);                    
                    raf.write (buffer);

                    long            t2 = TimeKeeper.currentTime;
                    long            latency = t2 - t;
                    
                    synchronized (statsLock) {
                        if (maxLatencyThisPeriod < latency)
                            maxLatencyThisPeriod = latency;

                        numWritesThisPeriod++;
                        totalNumWrites++;
                    }
                }

            } catch (InterruptedException x) {
                throw new RuntimeException (x);
            } catch (IOException x) {
                throw new RuntimeException (x);
            }
        }
    }

    public static void  main (String [] args) throws Exception {
        if (args.length == 0)
            args = new String [] { "d:/temp/committest.dat" };

        System.out.println ("Target rate: " + BPMS + " b/s");

        File                    f = new File (args [0]);
        RandomAccessFile        raf = new RandomAccessFile (f, "rw");

        raf.setLength (BLOCK_SIZE * NUM_BLOCKS);

        Writer                  writer = new Writer (raf);
        StatsPrinter            sp = new StatsPrinter ();
        Committer               c = new Committer (raf);

        c.start ();
        sp.start ();
        writer.start ();

        writer.join ();

        raf.close ();
        sp.interrupt ();

        f.delete ();
    }
}
