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
package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell;

import com.epam.deltix.data.stream.DXChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.qsrv.hf.blocks.InstrumentSet;
import com.epam.deltix.qsrv.hf.blocks.InstrumentToObjectMap;
import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.qsrv.hf.pub.ChannelQualityOfService;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.NullValueException;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.timebase.messages.TimeStamp;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldInfo;
import com.epam.deltix.qsrv.hf.pub.codec.TimeCodec;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;
import com.epam.deltix.qsrv.hf.pub.md.ArrayDataType;
import com.epam.deltix.qsrv.hf.pub.md.BooleanDataType;
import com.epam.deltix.qsrv.hf.pub.md.CharDataType;
import com.epam.deltix.qsrv.hf.pub.md.ClassDataType;
import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.pub.md.DateTimeDataType;
import com.epam.deltix.qsrv.hf.pub.md.EnumDataType;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.qsrv.hf.pub.md.TimeOfDayDataType;
import com.epam.deltix.qsrv.hf.pub.md.VarcharDataType;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeConstants;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.IndexedUnboundDecoderMap;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.StreamMessageSource;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.Parameter;
import com.epam.deltix.util.collections.CharSequenceSet;
import com.epam.deltix.util.concurrent.CursorIsClosedException;
import com.epam.deltix.util.io.CSVWriter;
import com.epam.deltix.util.lang.MathUtil;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.util.time.Interval;
import com.epam.deltix.util.time.TimeKeeper;

import javax.annotation.Nonnull;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.epam.deltix.util.cmdline.DefaultApplication.printException;

/**
 *
 */
public class Selector {

    public static final long DEFAULT_TIME = Long.MIN_VALUE;
    public static final long DEFAULT_ENDTIME = TimeConstants.TIMESTAMP_UNKNOWN;

    private final TickDBShell               shell;
    
    private long                            time = DEFAULT_TIME;
    private Interval                        timeOffset = null;
    private Interval                        endTimeOffset = null;
    private long                            endtime = DEFAULT_ENDTIME;
    private int                             num = 10;
    private boolean                         rawSelection = false;
    private boolean                         reverse = false;
    private ChannelQualityOfService         qos = ChannelQualityOfService.MAX_THROUGHPUT;
    private boolean                         versionTracking = false;
    private final MemoryDataInput           mdi = new MemoryDataInput ();
    private boolean                         decodeRaw = true;
    private boolean                         useqqlts = false;
    private DBQueryRunner testRunner = null;

    private InstrumentSet                   include = null;
    private InstrumentSet                   exclude = null;

    private CharSequenceSet                 types = null;
    private boolean                         printJson = false;

    public Selector (TickDBShell shell) {
        this.shell = shell;
    }
    
    private DBQueryRunner getTestRunner () {
        if (testRunner == null)
            testRunner = new DBQueryRunner();
        
        return (testRunner);
    }

    public void                 setTime (long time) {
        this.time = time;
        timeOffset = null;
    }

    public void                 setEndtime (long endtime) {
        this.endtime = endtime;
        endTimeOffset = null;
    }

    public void                 setTimeOffset (Interval offset) {
        time = Long.MIN_VALUE;
        timeOffset = offset;
    }

    public void                 setEndTimeOffset (Interval offset) {
        endtime = Long.MIN_VALUE;
        endTimeOffset = offset;
    }

    public void                 setNum (int num) {
        this.num = num;
    }
    
    public long                 getTime () {
        return time;
    }

    public long                 getTime (long endTime) {
        if (timeOffset != null && endTime != TimeStamp.TIMESTAMP_UNKNOWN)
            return endTime - timeOffset.toMilliseconds();

        return time;
    }

    public long                 getEndtime () {
        return endtime == TimeStamp.TIMESTAMP_UNKNOWN ? Long.MAX_VALUE : endtime;
    }

    public long                 getEndtime (long startTime) {
        if (endTimeOffset != null && startTime != TimeStamp.TIMESTAMP_UNKNOWN)
            return startTime + endTimeOffset.toMilliseconds();

        return endtime == TimeConstants.TIMESTAMP_UNKNOWN ? Long.MAX_VALUE : endtime;
    }
    
    public void                 setDecodeRaw (boolean decodeRaw) {
        this.decodeRaw = decodeRaw;
    }

    protected void              doSet () {
        System.out.println ("max:           " + num);
        System.out.println ("time:          " + shell.formatTime (time));
        System.out.println ("longtime:      " + time);
        System.out.println ("endtime:       " + shell.formatTime (endtime));
        System.out.println ("longendtime:   " + endtime);
        System.out.println (reverse ? "reverse" : "forward");  
        System.out.println ("raw:           " + (rawSelection ? "on" : "off"));  
        System.out.println ("qos:           " + qos);        
        System.out.println ("decode:        " + (decodeRaw ? "on" : "off"));
        System.out.println ("qqltr:         " + (useqqlts ? "on" : "off"));
        System.out.println ("history:       " + (versionTracking ? "on" : "off"));
        System.out.println ("printjson      " + printJson);
    }
    
    boolean                     doSet (String option, String value) throws Exception {
        if (option.equalsIgnoreCase ("max")) {            
            num = Integer.parseInt (value);
            shell.confirm ("max: " + num);
            return (true);
        }

        if (option.equalsIgnoreCase ("time")) {
            setTime(shell.parseTime (value));
            shell.confirm ("Selection time: " + shell.formatTime (time));
            return (true);
        }

        if (option.equalsIgnoreCase ("longtime")) {            
            setTime(Long.parseLong (value));
            shell.confirm ("Selection time: " + shell.formatTime (time));
            return (true);
        }

        if (option.equalsIgnoreCase ("timeoffset")) {
            setTimeOffset(Interval.valueOf(value));
            shell.confirm ("Time offset: " + timeOffset.toHumanString());
            return (true);
        }

        if (option.equalsIgnoreCase ("endtime")) {
            setEndtime(shell.parseTime (value));
            shell.confirm ("Selection end time: " + shell.formatTime (endtime));
            return (true);
        }

        if (option.equalsIgnoreCase ("longendtime")) {
            setEndtime(Long.parseLong (value));
            shell.confirm ("Selection end time: " + shell.formatTime (endtime));
            return (true);
        }

        if (option.equalsIgnoreCase ("endtimeoffset")) {
            setEndTimeOffset(Interval.valueOf(value));
            shell.confirm ("Selection End Time offset interval: " + endTimeOffset.toHumanString());
            return (true);
        }
        
        if (option.equalsIgnoreCase ("raw")) {
            rawSelection = value.equalsIgnoreCase ("on");
            shell.confirm ("Raw selection: " + rawSelection);
            return (true);
        }

        if (option.equalsIgnoreCase ("forward")) {
            reverse = false;
            shell.confirm ("Direction: forward");
            return (true);
        }

        if (option.equalsIgnoreCase ("reverse")) {
            reverse = true;
            shell.confirm ("Direction: reverse");
            return (true);
        }

        if (option.equalsIgnoreCase ("qos")) {
            qos = ChannelQualityOfService.valueOf (value);
            shell.confirm ("qos: " + qos);
            return (true);
        }

        if (option.equalsIgnoreCase ("history")) {
            versionTracking = value.equalsIgnoreCase ("on");
            shell.confirm ("history: " + versionTracking);
            return (true);
        }
        
        if (option.equalsIgnoreCase ("decode")) {
            decodeRaw = value.equalsIgnoreCase ("on");
            shell.confirm ("Raw decoding: " + decodeRaw);
            return (true);
        }

        if (option.equalsIgnoreCase ("qqltr")) {
            useqqlts = value.equalsIgnoreCase ("on");
            shell.confirm ("Use Time Range in QQL: " + useqqlts);   
            return (true);
        }

        if (option.equalsIgnoreCase("printjson")) {
            printJson = Boolean.parseBoolean(value);
            shell.confirm("Print JSON: " + printJson);
            return true;
        }

        return (false);
    }
    
    private void                doSymbols (String args, String fileId, LineNumberReader reader) {
        if (args == null || args.equalsIgnoreCase ("show")) {
            if (include == null) {
                System.out.println ("<all>");
            }

            if (include != null) {
                System.out.print("<include>: ");
                if (include.size() == 0)
                    System.out.println("none");

                for (IdentityKey id : include)
                    System.out.println (id.getSymbol ());
            }
            
            if (exclude != null) {
                System.out.print("<exclude>: ");
                if (exclude.size() == 0)
                    System.out.println("none");

                for (IdentityKey id : exclude)
                    System.out.println (id.getSymbol ());
            }


            return;
        }
        
        if (args.equalsIgnoreCase ("all")) {
            exclude = include = null;
            return;
        }

        if (args.startsWith("filter")) {
            try {
                InstrumentMessageSource ims =
                        shell.dbmgr.getDB().executeQuery (args.substring("filter".length()), new SelectionOptions(true, false), null, null, getTime());

                while (ims.next()) {
                    InstrumentMessage msg = ims.getMessage();
                    changeSubscription(msg.getSymbol(), true);
                }
            } catch (CompilationException x) {
                printException (x, false);
            }

            return;
        }

        if (args.equalsIgnoreCase ("clear")) {
            if (include != null)
                include.clear();

            exclude = null;

        } else {
            String []       s = args.split ("\\s", 3);
            boolean         add = s[0].equalsIgnoreCase ("add");

            String          symbol = s[2].trim ();
            
            boolean         ok = changeSubscription(symbol, add);
            shell.confirm (
                symbol + " " +
                (add ? 
                    ok ? "added" : "already in list" : 
                    ok ? "removed" : " not in list")
            );
        }
    }
    
    private boolean        changeSubscription(CharSequence symbol, boolean add) {
        boolean ok;

        if (add) {
           if (include == null) {
               exclude = null;
               include = new InstrumentSet();
           }
           ok = include.add(symbol);
        }
        else {
            if (include == null) {
                if (exclude == null)
                    exclude = new InstrumentSet();
                ok = exclude.add(symbol);
            } else {
                ok = include.remove(symbol);
            }
        }

        if (exclude != null && exclude.isEmpty())
            exclude = null;

        return ok;
    }

    private void                doTypes (String args, String fileId, LineNumberReader reader) {
        if (args == null || args.equalsIgnoreCase ("show")) {
            if (types == null)
                System.out.println ("<all>");
            else if (types.size() == 0)
                System.out.println ("<none>");
            else {
                for (CharSequence name : types)
                    System.out.println (name);
            }

            return;
        }

        if (args.equalsIgnoreCase ("all")) {
            types = null;
            return;
        }

        if (types == null)
            types = new CharSequenceSet ();

        if (args.equalsIgnoreCase ("clear")) {
            types.clear();
        } else {
            String []       s = args.split ("\\s", 3);
            boolean         add = s[0].equalsIgnoreCase ("add");

            for (int i = 1; i < s.length; i++) {
                String type = s[i].trim();

                boolean ok;

                if (add)
                    ok = types.add(type);
                else
                    ok = types.remove(type);

                shell.confirm( type + " " +
                                (add ?
                                        ok ? "added" : "already in list" :
                                        ok ? "removed" : " not in list")
                );
            }
        }
    }
    
    public boolean              doCommand (String key, String args, String fileId, LineNumberReader reader) 
        throws Exception 
    {
        if (key.equalsIgnoreCase ("symbols")) {
            doSymbols (args, fileId, reader);
            return (true);
        }
        
        if (key.equalsIgnoreCase ("types")) {
            doTypes (args, fileId, reader);
            return (true);
        }
        
        if (key.equalsIgnoreCase ("select")) {                         
            if (args != null)
                runQuery ("select " + args);
            else {
                if (!shell.dbmgr.checkStream ())
                    return (true);
                
                InstrumentMessageSource   cur = select ();

                printCursor (cur);
            }
            
            return (true);
        }

        if (key.equalsIgnoreCase ("read")) {

            if (!shell.dbmgr.checkChannels ())
                return (true);

            MessageSource<InstrumentMessage> source = shell.dbmgr.getSingleChannel().createConsumer(new SelectionOptions());
            printCursor (source);
            return (true);
        }

        if (key.equalsIgnoreCase ("readrate")) {

            if (!shell.dbmgr.checkChannels ())
                return (true);

            SelectionOptions options = new SelectionOptions(rawSelection, true, qos);
            MessageSource<InstrumentMessage> source = shell.dbmgr.getSingleChannel().createConsumer(options);
            printCursorRate(source);
            return (true);
        }
        
        if (key.equalsIgnoreCase ("drop")) { 
            runQuery (key + " " + args);
            return (true);
        }
        
        if (key.equalsIgnoreCase ("create") || 
            key.equalsIgnoreCase ("modify") || 
            key.equalsIgnoreCase ("alter")) 
        { 
            runQuery (key + " " + args, fileId, reader);
            return (true);
        }
        
        if (key.equals ("??")) {            
            runQuery (args, fileId, reader);
            return (true);
        }        
        
        if (key.equals ("?")) {            
            runQuery (args);
            return (true);
        }        
        
        if (key.equalsIgnoreCase ("monitor")) {
            if (!shell.dbmgr.checkStream () && shell.dbmgr.getSingleChannel() == null)
                return (true);
            
            SelectionOptions                options = 
                new SelectionOptions (rawSelection, true, qos);

            MessageSource<InstrumentMessage> source;
            DXChannel channel = shell.dbmgr.getSingleChannel();
            if (channel != null) {
                source = shell.dbmgr.getSingleChannel().createConsumer(options);
            } else {
                source = select (options);
            }

            try  {
                IndexedUnboundDecoderMap    dmap = newDecoderMap ();
                
                System.out.println ("Monitoring " + shell.dbmgr.pstreams () + "; hit <Enter> to abort ...");
                
                for (;;) {
                    source.next ();
                    
                    print (dmap, source);
                    
                    if (TickDBShell.checkInterrupt ())
                        break;
                }
            } finally {
                Util.close(source);
            }
                        
            return (true);
        }
        
        if (key.equalsIgnoreCase ("tptime")) {
            SelectionOptions                opts = getSelectionOptions();
//            SelectionOptions                opts =
//                new SelectionOptions (rawSelection, false, qos);
            
            long                            limit;
            
            InstrumentMessageSource         cur;
            
            if (StringUtils.isEmpty(args)) {
                if (!shell.dbmgr.checkStream ())
                    return (true);
            
                cur = select (time, opts, shell.dbmgr.getStreams ());
                limit = endtime;
            }
            else {
                cur = shell.dbmgr.getDB ().executeQuery (args, opts, shell.dbmgr.getStreams (), null, getQQLStartTime ());
                limit = TimeConstants.TIMESTAMP_UNKNOWN;
            }

            if (cur == null)
                System.out.println ("NO DATA");
            else 
                tptime (cur, limit);
            
            return (true);
        }
        
        if (key.equalsIgnoreCase ("stats")) { 
            if (!shell.dbmgr.checkStream ())
                return (true);
            
            stats (args);            
            return (true);
        }

        if (key.equalsIgnoreCase ("latency")) {
            if (args != null) {
                String[] s = args.split(StringUtils.REGEXP_WHITESPACE);
                if (s.length > 0 && s[0].equalsIgnoreCase("test")) {
                    LatencyTestOptions options = new LatencyTestOptions();
                    DXTickStream[] streams = shell.dbmgr.getStreams();
                    if (streams == null || streams.length == 0)
                        throw new IllegalStateException("Please specify stream for test (set 'set stream' operation)");

                    options.stream = streams[0];
                    if (s.length > 1)
                        options.messageDataSize = Integer.valueOf(s[1]);
                    if (s.length > 2)
                        options.warmupSize = Integer.valueOf(s[2]);
                    if (s.length > 3)
                        options.messagesPerLaunch = Integer.valueOf(s[3]);
                    if (s.length > 4)
                        options.numConsumers = Integer.valueOf(s[4]);
                    if (s.length > 5)
                        options.throughput = Integer.valueOf(s[5]);
                    if (s.length > 6)
                        options.launches = Integer.valueOf((s[6]));

                    if (s.length <= 7 || !s[7].equalsIgnoreCase("force")) {
                        System.out.print("All data from stream '" + options.stream + "' will be truncated! Proceed (y/N)?\n");
                        Scanner ans = new Scanner(System.in);
                        String answer = ans.nextLine();
                        if (!answer.equalsIgnoreCase("y")) {
                            return true;
                        }
                    }

                    MessageLatencyTest.run(options);
                    return true;
                }
            }

            if (!shell.dbmgr.checkStream ())
                return (true);

            SelectionOptions                options = 
                new SelectionOptions (rawSelection, true, qos);
            
            try (InstrumentMessageSource cur = selectLiveOnly (options)) {
                System.out.println ("Monitoring latency from " + shell.dbmgr.pstreams () + " ...");
                
                long        maxLatency = Long.MIN_VALUE;
                long        minLatency = Long.MAX_VALUE;
                long        repInterval = 1000;
                long        nextReport = TimeKeeper.currentTime + repInterval;
                
                for (;;) {
                    cur.next ();
                    
                    if (TickDBShell.checkInterrupt ())
                        break;
                    
                    InstrumentMessage msg = cur.getMessage ();
                    long                now = TimeKeeper.currentTime;
                    long                latency = now - msg.getTimeStampMs();
                    
                    if (latency > maxLatency)
                        maxLatency = latency;
                    
                    if (latency < minLatency)
                        minLatency = latency;
                    
                    if (now > nextReport) {
                        nextReport = now + repInterval;
                        
                        System.out.println ("    Latency: " + minLatency + " .. " + maxLatency + "; hit <Enter> to abort");
                        maxLatency = Long.MIN_VALUE;
                        minLatency = Long.MAX_VALUE;
                    }
                }
            }

            return (true);
        }
                
        if (key.equalsIgnoreCase ("test")) {        
            if (args == null) 
                shell.printUsage ();
            else
                testQQL (args, fileId, reader);            
            
            return (true);
        }

        if (key.equalsIgnoreCase ("testqql")) {                    
            testQQL (
                TickDBShell.getMultiLineInput (args, fileId, reader), 
                fileId, 
                reader
            );
            
            return (true);
        }

        if (key.equalsIgnoreCase ("desc")) {
            describe (args, TickDBShell.outWriter);
            return (true);
        }
                
        if (key.equalsIgnoreCase ("testdesc")) {
            testDescribe (args, fileId, reader);
            return (true);
        }
                
        if (key.equalsIgnoreCase ("param")) {
            if (args == null) {
                shell.error ("Usage: param add|clear|set|show", 1);
                return (true);
            }

            DBQueryRunner tr = getTestRunner ();
            String []       s = args.split (StringUtils.REGEXP_WHITESPACE, 2);
            String          pkey = s [0].toLowerCase ();

            switch (pkey) {
                case "show":
                    tr.showParams (TickDBShell.outWriter);
                    break;
                    
                case "clear":
                    tr.clearParams ();
                    TickDBShell.outWriter.println ("Parameters cleared.");
                    break;
                    
                case "add": {
                    if (s.length < 2) {
                        shell.error ("Usage: param add <name> <type> [ <value> ]", 1);
                        return (true);
                    }       

                    s = s [1].split (StringUtils.REGEXP_WHITESPACE, 3);

                    if (s.length < 2) {
                        shell.error ("Usage: param add <name> <type> [ <value> ]", 1);
                        return (true);
                    }       

                    String      typename = s [1].toUpperCase ();
                    String      pname = s [0].toUpperCase ();
                    Parameter   p = tr.addParam (typename, pname);

                    if (s.length == 3)
                        tr.setParam (pname, s [2]);

                    tr.showParam (p, TickDBShell.outWriter);
                    break;
                }
                
                case "set": {
                    s = s [1].split (StringUtils.REGEXP_WHITESPACE, 2);
                    Parameter   p = tr.setParam (s [0].toUpperCase (), s [1]);
                    tr.showParam (p, TickDBShell.outWriter);
                    break;
                }
            }
            
            return (true);
        }
        return (false);
    }
    
    private void                    describe (
        String                          args,
        Writer                          out
    )
        throws IOException 
    {
        DXTickStream []   streamsToDescribe;
        
        if (args == null)
            streamsToDescribe = shell.dbmgr.getStreams ();
        else {
            DXTickStream  s = shell.dbmgr.getDB ().getStream (args);
            
            if (s == null) {
                TickDBShell.errWriter.println ("Stream '" + args + "' not found.");
                return;
            }
            
            streamsToDescribe = new DXTickStream [] { s };
        }
        
        for (DXTickStream s : streamsToDescribe)
            out.write(s.describe());
    }
        
    public void                     testDescribe (
        String                          args,
        String                          fileId,
        LineNumberReader                reader
    ) 
        throws IOException
    {
        if (!shell.dbmgr.checkDb ())
            return;
        
        TestChecker     checker = new TestChecker (fileId, reader);
        
        try {
            describe (args, checker.getResultWriter ());            
            checker.resultDone ();
            shell.confirm ("\tOK");
        } finally {
            checker.recoverUnlessDone ();
        }
    }
    
    public void                     testQQL (
        String                          query,
        String                          fileId,
        LineNumberReader                reader
    ) 
        throws IOException
    {
        if (!shell.dbmgr.checkDb ())
            return;

        DBQueryRunner tr = getTestRunner ();
        TestChecker     checker = new TestChecker (fileId, reader);

        try {
            tr.runQuery (
                checker.getResultWriter (), 
                shell.dbmgr.getDB (), 
                getSelectionOptions (), 
                query, 
                getQQLStartTime (),
                getQQLEndTime(),
                num,
                printJson
            );
            checker.resultDone ();
            shell.confirm ("\tOK");
        } finally {
            checker.recoverUnlessDone ();
        }
    }
    
    private long                    getQQLStartTime () {
        return (useqqlts ? time : TimeConstants.TIMESTAMP_UNKNOWN);
    }

    private long                    getQQLEndTime () {
        return (useqqlts ? endtime : Long.MAX_VALUE);
        }

    public IdentityKey[]            getSelectedEntities() {
        return include != null ? include.toArray(new IdentityKey[include.size()]) : null;
    }

    public CharSequence[]   getSelectedSymbols() {
        IdentityKey[] ids = getSelectedEntities();
        if (ids == null) {
            return null;
        }

        CharSequence[] symbols = new CharSequence[ids.length];
        for (int i = 0; i < ids.length; ++i) {
            symbols[i] = ids[i].getSymbol();
        }

        return symbols;
    }

    public String[]                 getSelectedTypes() {
        return types != null ? types.toArray(new String[types.size()]) : null;
    }

    public InstrumentMessageSource  select (boolean raw) {
        return (select (raw, false));
    }
    
    public InstrumentMessageSource  select (boolean raw, boolean live) {
        return (select (new SelectionOptions (raw, live)));
    }
    
    private InstrumentMessageSource selectLiveOnly (SelectionOptions options) {
        options.live = true;
        return select(System.currentTimeMillis (), options, shell.dbmgr.getStreams ());
    }

    public InstrumentMessageSource  select () {
        return (select (getSelectionOptions ()));        
    }
    
    public InstrumentMessageSource  select (SelectionOptions options) {
        return (select (time, options, shell.dbmgr.getStreams ()));
    }
    
    public InstrumentMessageSource select (long timestamp,
                                            SelectionOptions options,
                                            TickStream [] tickStreams) {
        CharSequence []         symbols = getSelectedSymbols();
        String []               types = getSelectedTypes();

        return (
            tickStreams.length == 1 ?
                tickStreams [0].select (timestamp, options, types, symbols) :
                shell.dbmgr.getDB ().select (timestamp, options, types, symbols, tickStreams)
        );
    }

//    public InstrumentMessageSource select (long timestamp,
//                                           SelectionOptions options,
//                                           DXChannel channel) {
//
//        if (channels.length > 1) {
//
//        }
//
//        return (
//                tickStreams.length == 1 ?
//                        tickStreams [0].select (timestamp, options, types, ids) :
//                        shell.dbmgr.getDB ().select (timestamp, options, types, ids, tickStreams)
//        );
//    }

    public boolean                  accept (IdentityKey id) {
        if (exclude != null && exclude.contains(id))
            return false;

        return (include == null || include.contains(id));
    }
    
    public boolean                  enough (InstrumentMessage msg) {
        return (
            endtime != TimeConstants.TIMESTAMP_UNKNOWN &&
            msg.getTimeStampMs() > endtime
        );
    }
    
    public int                      getNumLines () {
        return (num);
    }
    
    public boolean                  isRaw () {
        return (rawSelection);
    }
    
    public SelectionOptions         getSelectionOptions () {
        SelectionOptions    opts = new SelectionOptions ();
        
        opts.raw = rawSelection;
        opts.channelQOS = qos;
        opts.reversed = reverse;
        opts.versionTracking = versionTracking;
        
        return (opts);
    }    
    
    public void                     runQuery (String args, String fileId, LineNumberReader reader)
        throws IOException, InterruptedException 
    {
        runQuery (TickDBShell.getMultiLineInput (args, fileId, reader));
    }
    
    public void                     runQuery (String text)
        throws IOException, InterruptedException 
    {
        if (text == null) {
            shell.printUsage();
            return;
        }

        if (!shell.dbmgr.checkDb())
            return;

        DBQueryRunner tr = getTestRunner ();
        long            t0 = System.currentTimeMillis ();

        tr.runQuery (
            TickDBShell.outWriter, 
            shell.dbmgr.getDB (), 
            getSelectionOptions (), 
            text, 
            getQQLStartTime (), 
            getQQLEndTime(),
            num, printJson
        );  

        if (shell.isTiming ()) {
            long        t1 = System.currentTimeMillis ();
            System.out.println ("Response time: " + (t1 - t0) * 0.001 + "s");
        }        
    }

    private void                tptime (InstrumentMessageSource cur, long limit) {
        MemoryDataInput                 input = new MemoryDataInput ();
        long                            t0 = System.currentTimeMillis ();
        long                            count = 0;
        IndexedUnboundDecoderMap        dmap = newDecoderMap ();
        long                            messageSize = 0;

        try {
            while (cur.next ()) {
                InstrumentMessage msg = cur.getMessage ();

                // TODO: handle reverse
                if (limit != TimeConstants.TIMESTAMP_UNKNOWN && msg.getTimeStampMs() > limit)
                    break;

                if (dmap != null) {
                    RawMessage          rmsg = (RawMessage) msg;
                    UnboundDecoder      decoder = dmap.getDecoder (cur);

                    rmsg.setUpMemoryDataInput (input);
                    messageSize = Math.max(messageSize, input.getLength());
                    decoder.beginRead (input);

                    while (decoder.nextField ())
                        if (!decoder.isNull())
                            decodeOne (decoder);
                }

                count++;
            }
        } finally {
            cur.close ();
        }
        
        long                            t1 = System.currentTimeMillis ();
        double                          s = (t1 - t0) * 0.001;
        System.out.printf (
            "%,d messages in %,.3fs; speed: %,.0f msg/s\n",
            count,
            s,
            count / s
        );        
    }
    
    public void                     printCursor (MessageSource<InstrumentMessage> cur) {
        if (cur == null) {
            System.out.println ("NO DATA");
        }
        else {
            try {
                IndexedUnboundDecoderMap dmap = newDecoderMap ();

                for (int ii = 0; ii < num && cur.next (); ii++) {
                    InstrumentMessage msg = cur.getMessage ();

                    if (enough (msg))
                        break;

                    print (dmap, cur);
                }
            } finally {
                cur.close ();
            }
        }
    }

    public void                     printCursorRate(MessageSource<InstrumentMessage> cur) {
        if (cur == null) {
            System.out.println("NO DATA");
        } else {
            try {
                printCursorRateInteractively(cur);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Interrupted");
            }
        }
    }

    /**
     * Reads data from the cursor and prints message rate once each second.
     * Stops the process as soon as end of cursor reached or any key pressed.
     *
     * @param cur cursor to read from
     */
    private void printCursorRateInteractively(@Nonnull MessageSource<InstrumentMessage> cur) throws IOException, InterruptedException {
        // Cleanup input
        while (System.in.available() > 0) {
            System.in.read();
        }

        AtomicBoolean stopFlag = new AtomicBoolean(false);
        AtomicBoolean endOfCursorFlag = new AtomicBoolean(false);
        AtomicLong messageCount = new AtomicLong();
        Thread readerThread = new Thread(() -> {
            try {
                while (!stopFlag.get()) {
                    boolean next = cur.next();
                    if (next) {
                        messageCount.incrementAndGet();
                    } else {
                        endOfCursorFlag.set(true);
                        break;
                    }
                }
            } catch (CursorIsClosedException ignore){
            }
        });
        readerThread.setName("TBShell \"printRate\" thread");
        readerThread.start();
        long prevTimeMs = System.currentTimeMillis();
        long prevMsgCount = messageCount.get();

        System.out.println();
        System.out.println("Printing message rate...");
        System.out.println("Press ENTER to stop");
        System.out.println();
        boolean stoppedItself = false;
        while (System.in.available() == 0) {
            if (endOfCursorFlag.get()) {
                System.out.println("End of cursor reached");
                stoppedItself = true;
                break;
            }
            if (!readerThread.isAlive()) {
                System.err.println("Reader stopped due with unknown reason");
                stoppedItself = true;
                break;
            }

            // Print message rate while there is no new input
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("Aborted");
                Thread.currentThread().interrupt();
                return;
            }

            long curTimeMs = System.currentTimeMillis();
            long curMsgCount = messageCount.get();
            long ratePerSecond = (curMsgCount - prevMsgCount) * 1000 / (curTimeMs - prevTimeMs);
            System.out.format("Msg rate: %,10d msg/sec\n", ratePerSecond);

            prevTimeMs = curTimeMs;
            prevMsgCount = curMsgCount;
        }

        // Consume one symbol from input
        if (!stoppedItself) {
            System.in.read();
        }

        System.out.println("Stopping reader...");
        stopFlag.set(true);
        readerThread.join(1000);
        cur.close();
        readerThread.join(3000);
        if (readerThread.isAlive()) {
            System.err.println("Failed to stop reader thread");
        } else {
            System.out.println("Reader stopped");
        }
    }

    private IndexedUnboundDecoderMap    newDecoderMap () {
        return (
            rawSelection && decodeRaw ?
                new IndexedUnboundDecoderMap (getCodecFactory ()) :
                null
        );
    }
    
    private void                print (IndexedUnboundDecoderMap dmap, MessageSource<InstrumentMessage> cur) {
        InstrumentMessage msg = cur.getMessage ();

        RawMessage          rmsg =
            msg instanceof RawMessage ?
                (RawMessage) msg : null;

        System.out.printf (
            "%s,%s,%s,%d",
            rmsg == null ?
                msg.getClass ().getSimpleName () :
                rmsg.type.getName (),
                msg.getSymbol(),
            shell.formatTime (msg.getTimeStampMs()),
                msg.getTimeStampMs()
        );

        if (decodeRaw && rmsg != null && cur instanceof StreamMessageSource) {
            UnboundDecoder      decoder = dmap.getDecoder ((StreamMessageSource)cur);

            assert decoder.getClassInfo ().getDescriptor ().equals (rmsg.type) :
                "Decoder type mismatch: message is of type " + rmsg.type +
                " but decoder is for " + decoder.getClassInfo ().getDescriptor ();

            rmsg.setUpMemoryDataInput (mdi);
            decoder.beginRead (mdi);

            while (decoder.nextField ()) {
                System.out.print (",");
                System.out.print (decoder.getField ().getName ());
                System.out.print (":");

                if (decoder.isNull ())
                    System.out.print ("<null>");
                else
                    System.out.print (decoder.getString ());
            }
        } else {
            System.out.print(msg);
        }

        System.out.println ();
    }

    private CodecFactory        getCodecFactory () {
        return (
            (qos == ChannelQualityOfService.MAX_THROUGHPUT) ?
                CodecFactory.COMPILED : CodecFactory.INTERPRETED
        );
    }

    private static void decodeOne (UnboundDecoder decoder) throws RuntimeException {
        NonStaticFieldInfo f = decoder.getField ();
        DataType type = f.getType ();
        
        try {
            if (type instanceof FloatDataType)
                decoder.getDouble ();
            else if (type instanceof IntegerDataType || 
                     type instanceof DateTimeDataType ||
                     type instanceof EnumDataType)
                decoder.getLong ();
            else if (type instanceof VarcharDataType)
                decoder.getString ();                
            else if (type instanceof BooleanDataType)
                decoder.getBoolean ();
            else if (type instanceof TimeOfDayDataType)
                decoder.getInt ();
            else if (type instanceof CharDataType)
                decoder.getChar ();
            else if (type instanceof ClassDataType)

                ;
            else if (type instanceof ArrayDataType)
                //decoder.getObject etc. 
                ;
            else
                throw new RuntimeException (type.getClass ().getName ());
        } catch (NullValueException e) {
            // just skip
        }
    }

    private static class SymStats implements Comparable <SymStats> {
        final ConstantIdentityKey     id;
        long                            size;
        long                            numMessages;

        SymStats (IdentityKey id) {
            this.id = ConstantIdentityKey.makeImmutable (id);
        }

        @Override
        public int      compareTo (SymStats that) {
            return (MathUtil.compare (this.size, that.size));
        }                
    }
    
    private void        stats (String out) throws IOException {
        final SelectionOptions                  opts =
            new SelectionOptions (true, false);
            
        final InstrumentToObjectMap <SymStats>  stats = 
            new InstrumentToObjectMap <> ();
        
        DXTickStream[] streams = this.shell.dbmgr.getStreams();

        // if stream is poly - then additional byte used to store RecordClassDescriptor index
        int[] polymorphic = new int[streams.length];
        for (int i = 0; i < streams.length; i++)
            polymorphic[i] = (streams[i].isPolymorphic() ? 1 : 0);

        try (InstrumentMessageSource cur =
                select (time, opts, streams))
        {
            while (cur.next ()) {
                RawMessage          msg = (RawMessage) cur.getMessage ();

                if (endtime != TimeConstants.TIMESTAMP_UNKNOWN && msg.getTimeStampMs() > endtime)
                    break;

                SymStats            ss = stats.get (msg);
                
                if (ss == null) {
                    ss = new SymStats (msg);
                    stats.put (msg, ss);
                }
                
                int mSize = msg.length + TimeCodec.getFieldSize(msg.getTimeStampMs(), TimeStamp.getNanosComponent(msg.getNanoTime())) +
                        polymorphic[cur.getCurrentStreamIndex()];

                ss.size += mSize + MessageSizeCodec.fieldSize(mSize);
                ss.numMessages++;
            }           
        }
        
        SymStats []     sa = stats.values ().toArray (new SymStats [stats.size ()]);
        
        Arrays.sort (sa);
        
        OutputStream    os = out == null ? System.out : new FileOutputStream (out);
        
        try {
            CSVWriter   csv = new CSVWriter (os);
            
            csv.writeLine ("Symbol", "# Messages", "Total Size");
            
            long        totalNumMessages = 0;
            long        totalSize = 0;
            
            for (SymStats ss : sa) {
                totalNumMessages += ss.numMessages;
                totalSize += ss.size;
                
                csv.writeLine (ss.id.symbol, ss.numMessages, ss.size);
            }
            
            csv.writeLine ("<TOTAL>", "", totalNumMessages, totalSize);
            
            csv.flush ();
        } finally {
            if (os != System.out)
                Util.close (os);
        }
    }
}
