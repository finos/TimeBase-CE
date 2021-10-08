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
package com.epam.deltix.qsrv.testsetup;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.util.ZIPUtil;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.util.cmdline.DefaultApplication;
import com.epam.deltix.util.csvx.CSVXReader;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.Interval;
import com.epam.deltix.util.time.Periodicity;

import java.util.zip.*;
import java.util.TimeZone;
import java.io.*;
import java.text.*;

/**
 *
 */
public class TickDBCreator extends DefaultApplication {
    public static final String      LOCATION = Home.getPath ("temp/qstest/tickdb");
    public static final String      RO_MINUTES_LOCATION = Home.getPath ("testdata/tickdb/minutes");

    public static final String      BARS_STREAM_KEY = "bars";

    public static final long        TEST_BASE_TIMESTAMP = 1266600000000L; // 2010-02-19 17:20:00.0 GMT
    public static final int         NUM_TEST_STREAMS = 3;
    public static final String []   TEST_STREAM_KEYS = new String [NUM_TEST_STREAMS];
    public static final int         TEST_DF = 2;
    public static final int         NUM_SYMBOLS = 4;
    public static final String []   TEST_SYMBOLS = new String [NUM_SYMBOLS];
    public static final IdentityKey[]   TEST_IDS = new IdentityKey[NUM_SYMBOLS];
    public static final int         NUM_MESSAGES = 1000;
    public static final String []   TYPE_NAMES = {
        IntMessage.TYPE_NAME,
        FloatMessage.TYPE_NAME,
        StringMessage.TYPE_NAME
    };

    public static final int             NUM_TYPES = TYPE_NAMES.length;


    private static final RecordClassDescriptor   RCD_IntMessage;
    private static final RecordClassDescriptor   RCD_FloatMessage;
    private static final RecordClassDescriptor   RCD_StringMessage;
    
    static {
        Introspector            ix = Introspector.createEmptyMessageIntrospector ();

        try {
            RCD_IntMessage = ix.introspectRecordClass (IntMessage.class);
            RCD_StringMessage = ix.introspectRecordClass (StringMessage.class);
            RCD_FloatMessage = ix.introspectRecordClass (FloatMessage.class);
        } catch (Introspector.IntrospectionException x) {
            throw new RuntimeException (x);
        }

        for (int ii = 0; ii < NUM_TEST_STREAMS; ii++)
            TEST_STREAM_KEYS [ii] = "test_" + ii;

        for (int ii = 0; ii < NUM_SYMBOLS; ii++) {
            TEST_SYMBOLS [ii] = "S" + ii;
            TEST_IDS [ii] =
                new ConstantIdentityKey(TEST_SYMBOLS [ii]);
        }
    }
    //
    //  Open read-only standard databases
    //
    public static DXTickDB      openStdMinutesTestDB () {
        DXTickDB    db = TickDBFactory.create (RO_MINUTES_LOCATION);

        db.open (true);

        return (db);
    }

    public static DXTickDB      openStdTicksTestDB (String path) throws IOException, InterruptedException {
        File home = new File(path);
        IOUtil.removeRecursive(home);

        try (InputStream is = IOUtil.openResourceAsStream("com/epam/deltix/testticks.zip")) {
            ZIPUtil.extractZipStream(is, home);
        }
        
        DXTickDB    db = TickDBFactory.create (path);

        db.open (true);

        return (db);
    }

    //
    //  Minute bar stream
    //
    public static void          loadBars (
        String                      symbol, 
        InputStream                 is, 
        TickLoader                  loader
    ) 
        throws IOException 
    {
        DateFormat                  df = new SimpleDateFormat ("MM/dd/yyyy HH:mm");
        df.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        CSVXReader                  in =
            new CSVXReader (new InputStreamReader (is), ',', true, symbol);
        
        StringBuilder               sb = new StringBuilder ();
        BarMessage bar = new BarMessage();
        
        bar.setSymbol(symbol);
        //bar.barSize = BarMessage.BAR_MINUTE;
        bar.setCurrencyCode ((short) 840);
        
        int                         row = 0;
        
        while (in.nextLine ()) {
            sb.setLength (0);
            sb.append (in.getCell (0));
            sb.append (' ');
            sb.append (in.getCell (1));
            
            try {
                bar.setTimeStampMs(df.parse (sb.toString ()).getTime ());
            } catch (ParseException px) {
                throw new IOException (in.getDiagPrefixWithLineNumber (), px);
            }
            
            bar.setOpen(in.getDouble (2));
            bar.setHigh(in.getDouble (3));
            bar.setLow(in.getDouble (4));
            bar.setClose(in.getDouble (5));
            bar.setVolume(in.getDouble (6));
            
            loader.send (bar);
            row++;
        }
    }
    
    public static void          loadBarsFromZip (ZipInputStream zis, TickLoader loader)
        throws IOException 
    {
        for (;;) {
            ZipEntry        zentry = zis.getNextEntry ();

            if (zentry == null)
                break;

            String          name = zentry.getName ();            
            int             dot = name.indexOf ('.');
            
            if (dot > 0)
                name = name.substring (0, dot);
            
            if (!Util.QUIET)
                System.out.println ("    " + name + " ...");
            loadBars (name, zis, loader);
        }
    }
    
    public static void          loadBarsFromZipResource (String path, TickLoader loader)
        throws IOException 
    {
        if (!Util.QUIET)
            System.out.println ("Loading " + path + " ...");

        ZipInputStream      zis = 
            new ZipInputStream (TickDBCreator.class.getResourceAsStream (path));
        
        try {
            loadBarsFromZip (zis, loader);
        } finally {
            Util.close (zis);            
        }
    }
    //
    //  Test data stream
    //
    public static IdentityKey[]     testEntities (int ... ids) {
        int                     n = ids.length;
        IdentityKey[]   iids = new IdentityKey[n];

        for (int ii = 0; ii < n; ii++)
            iids [ii] = TEST_IDS [ids [ii]];

        return (iids);
    }

    public static long              getTestTimestamp (
        int                             streamIdx,
        int                             entityIdx,
        int                             sequenceIdx
    )
    {
        return (TEST_BASE_TIMESTAMP + sequenceIdx * 1000 + streamIdx * 100 + entityIdx);
    }

    public static void              generateTestData (int sidx, TickLoader loader) {
        IntMessage      imsg = new IntMessage ();
        FloatMessage    fmsg = new FloatMessage ();
        StringMessage   smsg = new StringMessage ();

        long            t = TEST_BASE_TIMESTAMP + sidx * 100;

        for (int n = 0; n < NUM_MESSAGES; n++) {
            for (int isym = 0; isym < NUM_SYMBOLS; isym++) {
                imsg.setTimeStampMs(t + isym);
                fmsg.setTimeStampMs(t + isym);
                smsg.setTimeStampMs(t + isym);
                imsg.setSymbol(TEST_SYMBOLS [isym]);
                fmsg.setSymbol(TEST_SYMBOLS [isym]);
                smsg.setSymbol(TEST_SYMBOLS [isym]);

                imsg.data = n;
                loader.send (imsg);

                fmsg.data = n;
                loader.send (fmsg);

                smsg.data = "Seq #" + n;
                loader.send (smsg);
            }

            t += 1000;
        }

    }

    public static void              createTestStreams (DXTickDB db) {
        for (int ii = 0; ii < NUM_TEST_STREAMS; ii++)
            createTestStream (db, ii);
    }

    public static DXTickStream      createTestStream (DXTickDB tdb, int idx) {
        String                  name = TEST_STREAM_KEYS [idx];

        StreamOptions           options = 
            new StreamOptions (StreamScope.DURABLE, name, name, TEST_DF);

        options.setPolymorphic (RCD_IntMessage, RCD_FloatMessage, RCD_StringMessage);

        DXTickStream    stream = tdb.createStream (name, options);

        TickLoader      loader = stream.createLoader ();

        try {
            generateTestData (idx, loader);
            loader.close ();
            loader = null;
        } finally {
            Util.close (loader);
        }

        if (!Util.QUIET)
            System.out.println (name + " is done.");

        return (stream);
    }

//    //
//    //  Database creation
//    //
//    public static DXTickDB              createTestTickDB () {
//        return (createTickDB (LOCATION, true));
//    }

    public static DXTickDB              createTickDB (String url, boolean format) {
        DXTickDB          tdb = TickDBFactory.createFromUrl (url);

        if (format) {
            tdb.format ();
            if (!Util.QUIET)
                System.out.println (tdb.getId () + " has been created.");
        }
        else {
            tdb.open (false);
            if (!Util.QUIET)
                System.out.println ("Connected to " + tdb.getId ());
        }

        createBarsStream (tdb);
        //createTestStreams (tdb);
        
        return (tdb);
    }

    public static DXTickStream        createBarsStream (DXTickDB db) {
        return createBarsStream(db, BARS_STREAM_KEY);
    }
    
    public static DXTickStream        createBarsStream (DXTickDB tdb, String name) {
        DXTickStream stream = tdb.createStream(name, name, name, 0);

        StreamConfigurationHelper.setBar (
            stream, "", null, Interval.MINUTE,
            "DECIMAL(4)",
            "DECIMAL(0)"
        );
        stream.setPeriodicity(Periodicity.mkRegular(Interval.MINUTE));

        LoadingOptions options = new LoadingOptions();
        //options.writeMode = LoadingOptions.WriteMode.INSERT;
        
        try (TickLoader      loader = stream.createLoader (options)) {
            loadBarsFromZipResource("TestBars.zip", loader);
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        }

        if (!Util.QUIET)
            System.out.println("Done.");
        
        return stream;
    }

    public TickDBCreator (String [] args) {
        super (args);
    }

    @Override
    protected void          run () throws Throwable {
        String          url = getArgValue ("-db", LOCATION);
        boolean         format = isArgSpecified ("-format");
        
        createTickDB (url, format).close ();
    }

    public static void      main (String [] args) throws Exception {
        new TickDBCreator (args).start ();
    }
}
