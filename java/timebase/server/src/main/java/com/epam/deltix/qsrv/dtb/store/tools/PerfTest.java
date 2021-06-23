/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.qsrv.dtb.store.tools;

import com.epam.deltix.qsrv.dtb.fs.pub.*;
import com.epam.deltix.qsrv.dtb.store.impl.PDSFactory;
import com.epam.deltix.qsrv.dtb.store.pub.*;
import com.epam.deltix.util.collections.PrintVisitor;
import com.epam.deltix.util.collections.generated.*;
import com.epam.deltix.util.lang.Disposable;
import com.epam.deltix.util.memory.*;
import java.io.*;
import java.util.*;
import java.util.logging.*;

public class PerfTest implements Disposable {    
    public static final int                     NTSFS = 10 << 20;
    public static final long BASE_TIME = 1356998400000000000L;

    private final AbstractFileSystem            fs;
    private TSRoot                              root;
    private final PersistentDataStore           cache = PDSFactory.create ();
    
    public PerfTest (AbstractFileSystem fs) {
        this.fs = fs;
    }
    
    public void         prepare (String outPath) {
        cache.start ();
        
        root = cache.createRoot (null, fs, outPath);
    }
    
    @Override
    public void         close () {        
        cache.shutdown ();        
        cache.waitForShutdown (0);        
    }
    
    public void         mapTest () {
        root.open (true);
        
        System.out.println ("TS Refs:");
        
        ArrayList <TSRef>       tss = new ArrayList <> ();
        
        root.selectTimeSlices (null, null, tss);
        
        for (TSRef tsref : tss) {
            System.out.println ("    " + tsref.getPath ());
            
            testRead (tsref);
        }
        
        root.close ();
    }
    
    private void        testRead (TSRef tsref) {
        TSMessageConsumerImpl       consumer = new TSMessageConsumerImpl ();
        
        try (DataReader reader = cache.createReader (false)) {
            reader.associate (root);

            reader.open (tsref, Long.MIN_VALUE, false, EntityFilter.ALL);

            while (reader.readNext (consumer)) { 
                // go on
            }
        }
        
        System.out.println ("    Read " + consumer.numMsgs + " msgs from " + tsref);
    }
    
    public void         listSymbols () {
        root.open (true);
        
        IntegerArrayList    ids = new IntegerArrayList ();
        SymbolRegistry      sr = root.getSymbolRegistry ();

        sr.listIds (ids);
        
        int                 n = ids.size ();
            
        System.out.println ("Symbols:");
        
        TimeRange           tr = new TimeRange ();
        
        for (int ii= 0; ii < n; ii++) {
            if (ii == 10) {
                System.out.println ("... " + (n - ii) + " more ...");
                break;
            }
            
            int             id = ids.get (ii);
            
            root.getTimeRange (id, tr);
            
            System.out.println (
                "    " + sr.idToSymbol (id) + " #" + id + ": " + 
                tr.from + " .. " + tr.to + " Data: [" +
                sr.getEntityData (id) + "]"
            );                        
        }
        
        root.close ();
    }
    
    public void         generateMessages (
        int                 numEntities,
        int                 msgSize, 
        long                numMsgs,
        long                timestampBatchSize,
        String              compression
    ) 
    {
        root.setCompression (compression);
        root.format ();
        
        String []           symbols = new String [numEntities];
        int []              ids = new int [numEntities];
        
        for (int ii = 0; ii < numEntities; ii++) {
            symbols [ii] = "DLTX_" + ii;
            ids [ii] = 
                root.getSymbolRegistry ().registerSymbol (
                    symbols [ii], 
                    "data for " + symbols [ii]
                );
        }
        
        final byte []       body = new byte [msgSize];

        Arrays.fill (body, (byte) 0xFF);

        long            start = System.currentTimeMillis ();
        
        try (DataWriter writer = cache.createWriter ()) {        
            writer.associate (root);

            TSMessageProducer producer =
                new TSMessageProducer () {
                    @Override
                    public void     writeBody (MemoryDataOutput out) {
                        out.write (body);
                    }                
                };

            
            long            t = BASE_TIME;

            writer.open (t, null);

            for (long count = 0; count < numMsgs; count++) {
                int     entity = (int) (count % numEntities);
                long    timestamp = 
                    t + timestampBatchSize * (count / timestampBatchSize);
                        
                writer.insertMessage (
                    ids [entity], 
                    timestamp, 
                    0,
                    producer
                );
            }                                
        }
        
        cache.waitUntilDataStored (0);
        
        root.close ();
        
        long            end = System.currentTimeMillis ();
        double          s = (end - start) * 0.001;
        long            mb = (msgSize * numMsgs) >> 20;
        
        System.out.println ("Wrote       " + numMsgs + " msgs in " + s + " seconds");
        System.out.println ("Total Data: " + mb + "MB");
        System.out.println ("Msg Rate:   " + (numMsgs * 1E-6) / s + " M msgs/s");
        System.out.println ("Data Rate:  " + mb / s + " MB/s");
    }
    
    private static class TSMessageConsumerImpl implements TSMessageConsumer {
        int             numMsgs = 0;
        long            totalBytes = 0;
        
        public TSMessageConsumerImpl () {
        }

        @Override
        public void     process (int entity, long timestampNanos, int type, int bodyLength, MemoryDataInput mdi) {
            numMsgs++;
            totalBytes += bodyLength;            
        }

        @Override
        public boolean processRealTime(long timestampNanos) {
            // do nothing
            return false;
        }

        @Override
        public boolean isRealTime() {
            return false;
        }

        @Override
        public boolean realTimeAvailable() {
            return false;
        }
    }
    
    class Reader extends Thread {
        private final long              limit;
        private final EntityFilter      filter;
        TSMessageConsumerImpl           consumer = new TSMessageConsumerImpl ();

        public Reader (long limit, EntityFilter filter) {
            this.limit = limit;
            this.filter = filter;
        }
                
        @Override
        public void run () {
            try (DataReader reader = cache.createReader (false)) {
                reader.associate (root);

                reader.open (0, false, filter);

                while (reader.readNext (consumer)) {
                    if (consumer.numMsgs % 10000 == 0 && System.currentTimeMillis () > limit)
                        break;
                }
            }
        }
    }
    
    public void         readMessages (
        int                 numReaders, 
        int                 readTime,
        EntityFilter []     filters
    )
        throws InterruptedException 
    {
        root.open (true);
        
        Reader []           readers = new Reader [numReaders];
        
        long                start = System.currentTimeMillis ();
        long                limit = start + readTime;
            
        for (int ii = 0; ii < numReaders; ii++)             
            readers [ii] = 
                new Reader (
                    limit,
                    filters == null ? null : filters [ii % filters.length]
                );
                
        for (int ii = 0; ii < numReaders; ii++)
            readers [ii].start ();
        
        for (int ii = 0; ii < numReaders; ii++)
            readers [ii].join ();
        
        root.close ();
        
        long            end = System.currentTimeMillis ();
        double          s = (end - start) * 0.001;
        long            totalBytes = 0;
        int             numMsgs = 0;
        
        for (int ii = 0; ii < numReaders; ii++) {
            TSMessageConsumerImpl   consumer = readers [ii].consumer;
            
            totalBytes += consumer.totalBytes;
            numMsgs += consumer.numMsgs;
        }
                
        long            mb = totalBytes >> 20;
        
        System.out.println ("Read        " + numMsgs + " msgs in " + s + " seconds");
        System.out.println ("Total Data: " + mb + "MB");
        System.out.println ("Msg Rate:   " + (numMsgs * 1E-6) / s + " M msgs/s");
        System.out.println ("Data Rate:  " + mb / s + " MB/s");
    }
    
    public static void  main (String [] args) throws Exception {
        try (InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("config/conslogger.properties")) {
            LogManager.getLogManager ().readConfiguration (is);
        }
        
        String              path = "C:\\TEMP\\dtb";
        boolean             write = false;
        boolean             list = false;
        boolean             read = false;
        boolean             mapTest = false;
        String              compression = "";
        int                 numEntities = 100;
        int                 msgSize = 30;
        long                numMessages = 1000000;
        int                 numReaders = 1;
        int                 readTime = 20000;
        EntityFilter []     filters = null;
        long                tsbs = 1;
        AbstractFileSystem  fs = FSFactory.getLocalFS();
        
        for (int ii = 0; ii < args.length; ) {
            String          arg = args [ii++];
            
            switch (arg) {
                case "ls":      list = true; break;
                case "wr":      write = true; break;
                case "rd":      read = true; break;
                case "mt":      mapTest = true; break;
                case "-tb":     tsbs = Long.parseLong (args [ii++]); break;
                case "-e":      numEntities = Integer.parseInt (args [ii++]); break;
                case "-z":      compression = args [ii++]; break;
                case "-s":      msgSize = Integer.parseInt (args [ii++]); break;
                case "-n":      numMessages = Long.parseLong (args [ii++]); break;
                case "-d":      path = args [ii++]; break;
                case "-nr":     numReaders = Integer.parseInt (args [ii++]); break;
                case "-rt":     readTime = 1000 * Integer.parseInt (args [ii++]); break;
                case "-ef":     filters = parseFilters (args [ii++]); break;
                //case "-hadoop": fs = DistributedFS.createFromUrl (args [ii++]); break;
                    
                default: throw new IllegalArgumentException (arg);
            }
        }
        
        if (!write && !read && !list && !mapTest)
            write = read = list = mapTest = true;
        
        if (write)
            FSUtils.removeRecursive (
                fs.createPath (path), 
                false,
                new PrintVisitor (System.out, "rm ", "\n")
            );
        
        try (PerfTest              dbc = new PerfTest (fs)) {                
            dbc.prepare (path);

            if (write) {
                dbc.generateMessages(numEntities, msgSize, numMessages, tsbs, compression);
            }

            if (list)
                dbc.listSymbols ();

            if (mapTest)
                dbc.mapTest ();
            
            if (read)
                dbc.readMessages (numReaders, readTime, filters);                
        }       
    }   
    
    private static EntityFilter []  parseFilters (String arg) {
        String []           args = arg.split (",");
        int                 n = args.length;
        EntityFilter []     ret = new EntityFilter [n];
        
        for (int ii = 0; ii < n; ii++) {
            String          fs = args [ii];
            EntityFilter    filter;
            
            switch (fs) {
                case "all": 
                    filter = EntityFilter.ALL;  
                    break;
                    
                default:    
                    filter = new SingleEntityFilter (Integer.parseInt (fs)); 
                    break;
            }
               
            ret [ii] = filter;
        }
            
        return (ret);
    }
        
}
