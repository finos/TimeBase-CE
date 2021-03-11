package com.epam.deltix.temp.ramdisktests;

import java.io.*;
import java.util.logging.*;

/**
 *
 */
public class ManyFiles {
    static final int        MB = 1 << 20;
    static final long       GB = 1L << 30;
    static byte []          block = new byte [MB];
    
    static {
        for (int ii = 0; ii < MB; ii++)
            block [ii] = (byte) ii;
    }
    
    public static void      createFile (File f, long size) throws IOException {
        try (RandomAccessFile   rf = new RandomAccessFile (f, "rw")) {
            rf.setLength (size);
                
            long                n = size / MB;
            
            for (long ii = 0; ii < n; ii++)
                rf.write (block);
        }
    }
    
    public static void      readFile (File f, byte [] buffer) throws IOException {
        try (RandomAccessFile   rf = new RandomAccessFile (f, "rw")) {               
            for (;;) {
                int n = rf.read (buffer);
                
                if (n < 0)
                    break;
                
                n0 += n;
                n1 += n;
            }
        }
    }
    
    public static void      createOld (File location) throws IOException {
        location.mkdirs ();
        
        createFile (new File (location, "big.dat"), 100 * GB);
    }
    
    public static void      createNew (File location) throws IOException {
        final int       singleFileSize = 10 * MB;
        final int       numFiles = (int) (100 * GB / singleFileSize);
        
        location.mkdirs ();
        
        for (int ii = 0; ii < numFiles; ii++) {
            File    f = new File (location, "small-" + ii + ".dat");
            
            createFile (f, singleFileSize);
            
            System.out.println (f + " done");
        }        
    }
    
    static volatile long             n0;
    static volatile long             t0;
    static volatile long             n1;
    static volatile long             t1;
    
    private static long     rateMBPS (long n, long dt) {
        if (dt == 0)
            return (0);
        
        return ((n >> 20) * 1000 / dt);
    }
    
    public static void      readAll (File location, byte [] buffer) 
        throws IOException 
    {
        n0 = n1 = 0;
        t1 = t0 = System.currentTimeMillis ();        
            
        new Thread ("Stats printer") {
            @Override
            public void run () {
                for (;;) {
                    long    now = System.currentTimeMillis ();

                    System.out.print (
                        "Last: " + rateMBPS (n1, now - t1) + 
                        " MB/s; Cumulative: " + rateMBPS (n0, now - t0) + 
                        " MB/s           \r"
                    );

                    t1 = now;
                    n1 = 0;
                    try {
                        Thread.sleep (1000);
                    } catch (InterruptedException ex) {
                        break;
                    }
                }
            }
        }.start ();
        
        File []         list = location.listFiles ();
        
        for (File f : list) {
            readFile (f, buffer);            
        }        
    }
    
    public static void main (String [] args) throws IOException {
        //createOld (new File ("h:/ftest/old"));
        //createNew (new File ("h:/ftest/new"));
        
        byte [] buffer = new byte [16 << 10];
        
        readAll (new File (args [0]), buffer);
        
//        long    dt = System.currentTimeMillis () - t0;
//        
//        System.out.println (((100 << 10) * 1000) / dt + " MB/s");
    }
}
