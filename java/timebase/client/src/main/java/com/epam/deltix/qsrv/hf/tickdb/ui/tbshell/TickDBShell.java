package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell;

import com.epam.deltix.data.stream.ConsumableMessageSource;
import com.epam.deltix.data.stream.DXChannel;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.stream.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.DBLock;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockType;
import com.epam.deltix.qsrv.hf.tickdb.schema.MetaDataChange;
import com.epam.deltix.qsrv.hf.tickdb.schema.SchemaAnalyzer;
import com.epam.deltix.qsrv.hf.tickdb.schema.SchemaConverter;
import com.epam.deltix.qsrv.hf.tickdb.schema.SimpleClassSet;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.benchmark.BenchmarkCommandProcessor;
import com.epam.deltix.qsrv.util.json.JSONHelper;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.TimeStamp;
import com.epam.deltix.util.cmdline.AbstractShell;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.progress.ConsoleProgressIndicator;
import com.epam.deltix.util.time.TimeKeeper;
import com.epam.deltix.util.time.Interval;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Timebase command line shell. See help for info.
 */
public class TickDBShell extends AbstractShell {

    public static final String SECURITIES_STREAM = "securities";

    public final DBMgr              dbmgr = new DBMgr (this);
    public final Selector           selector = new Selector (this);
//    public final L2Processor        l2processor = new L2Processor (this);
    public final Replicator         replicator = new Replicator (this);
    public final PlayerCommandProcessor player = new PlayerCommandProcessor(this);
    public final BenchmarkCommandProcessor benchmark = new BenchmarkCommandProcessor(this);
    
    private boolean                 spaceSep = true;
    private boolean                 timing = false;
    private TimeZone                tz = TimeZone.getTimeZone ("America/New_York");
    private SimpleDateFormat        df = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss.SSS");
    private File                    srcMsgFile = null;

    private List<IdentityKey> fromEntities = new ArrayList<>();
    private List<IdentityKey> toEntities = new ArrayList<>();
    
    private final ArrayList<Job>    jobs = new ArrayList<Job>();
        
    private TickDBShell (String [] args) {
        super (args);
        df.setTimeZone (tz);

        TickDBFactory.setApplicationName("Timebase Shell");
    }
    
    public TickDBShell () {
        this (new String [0]);
    }

    public static void      main (String args []) throws Throwable {
        new TickDBShell (args).start ();
    }
       
    public long            parseTime (String value) throws ParseException {
        if (value.equalsIgnoreCase ("max"))
            return (Long.MAX_VALUE);
        
        if (value.equalsIgnoreCase ("min"))
            return (Long.MIN_VALUE);
                        
        return (df.parse (value).getTime ());
    }
    
    public String          formatTime (long time) {
        if (time == Long.MIN_VALUE)
            return ("min");
        
        if (time == Long.MAX_VALUE)
            return ("max");
        
        return (df.format (new Date (time)));
    }

    public void             startJob(Job job) {
        jobs.add(job);
        job.start();
    }

    @Override
    public String    expandPath (String path) {
        return (path.replaceAll ("\\$\\{home\\}", Matcher.quoteReplacement (Home.get ())));
    }
    
    @Override
    protected boolean       doSet (String option, String value) throws Exception {
        if (dbmgr.doSet (option, value))
            return (true);
        
        if (selector.doSet (option, value))
            return (true);
        
//        if (l2processor.doSet (option, value))
//            return (true);

        if (player.doSet(option, value))
            return (true);

        if (replicator.doSet(option, value))
            return (true);

        if (benchmark.doSet(option, value))
            return (true);
        
        if (option.equalsIgnoreCase ("tz")) {            
            tz = TimeZone.getTimeZone (value);
            df.setTimeZone (tz);
            confirm ("tz: " + tz.getDisplayName ());
            return (true);
        } 
        
        if (option.equalsIgnoreCase ("timeformat")) {            
            df = new SimpleDateFormat (value);
            df.setTimeZone (tz);
            confirm ("Time will now be displayed like this: " + df.format (new Date ()));
            return (true);
        } 
        
        if (option.equalsIgnoreCase ("space")) {
            spaceSep = value.equalsIgnoreCase ("on");
            confirm ("Space is a separator: " + spaceSep);
            return (true);
        }

        if (option.equalsIgnoreCase ("timing")) {
            timing = value.equalsIgnoreCase ("on");
            confirm ("Timing: " + timing);
            return (true);
        }

        if (option.equalsIgnoreCase ("src")) {            
            srcMsgFile = new File (value);

            if (srcMsgFile.canRead()) {
                if (!Util.QUIET)
                    confirm("Source message file: " + srcMsgFile.getCanonicalPath());
            } else
                confirm("Cannot read " + srcMsgFile.getPath());

            return (true);
        }

        return (super.doSet (option, value));
    }

    public static String    formatOnOff (boolean f) {
        return (f ? "on" : "off");
    }
    
    public static boolean   parseOnOff (String s) {
        return (s.equalsIgnoreCase ("on"));
    }

    @Override
    protected void          doSet () {
        dbmgr.doSet ();
        selector.doSet ();
        replicator.doSet();
        player.doSet ();
        benchmark.doSet ();
        
        System.out.println ("tz:            " + tz.getDisplayName ());
        System.out.println ("timeformat:    " + df.toPattern ());
        System.out.println ("src:           " + (srcMsgFile == null ? "<null>" : srcMsgFile.getPath ()));
        System.out.println ("timing:        " + formatOnOff (timing));
        System.out.println ("space:         " + formatOnOff (spaceSep));
                
        super.doSet ();
    }
    
    @Override
    protected boolean       doCommand (String key, final String args, final String fileId, final LineNumberReader reader)
        throws Exception 
    {
        if ("start".equalsIgnoreCase(key)) {
            System.out.println ("Starting command '" + args + "' in background.");
            final String k = getKey(args);

            Job job = new Job(k) {
                @Override
                public void run() {
                    try {
                        doCommand(k, args.substring(k.length()).trim(), fileId, reader);
                    } catch (Throwable x) {
                        printException (x, true);
                        error (2);
                    }
                }
            };

            startJob(job);
            return true;
        }

        if ("jobs".equalsIgnoreCase(key)) {
            for (Job job : jobs)
                System.out.println(job.getName() + ": " + job.getId());
            return (true);
        }

        if ("kill".equalsIgnoreCase(key)) {
            Job toKill = null;
            for (Job j : jobs) {
                if (String.valueOf(j.getId()).equals(args.trim()))
                    toKill = j;
            }

            if (toKill != null)
                toKill.interrupt();

            return (true);
        }

        if (dbmgr.doCommand (key, args))
            return (true);
                
        if (selector.doCommand (key, args, fileId, reader))
            return (true);

        if (replicator.doCommand (key, args, fileId, reader))
            return (true);

        if (player.doCommand (key, args))
            return (true);

        if (benchmark.doCommand (key, args))
            return (true);

        if (key.equalsIgnoreCase ("msgfilter")) {
            if (srcMsgFile == null)
                System.out.println ("Source file not set. Use set src first.");
            else
                filterMessageFile (srcMsgFile, args);
            
            return (true);
        }
        
        if (key.equalsIgnoreCase ("entities")) {
            if (!dbmgr.checkStream ())
                return (true);
            
            for (TickStream s : dbmgr.getStreams ()) {
                System.out.println ("Stream " + s.getKey () + ":");
                IdentityKey[]   ids = s.listEntities ();

                Arrays.sort (ids);

                for (IdentityKey id : ids) {
                    long []             tr = s.getTimeRange (id);
                    String              idstr = id.toString ();

                    if (tr == null)
                        System.out.println (" " + idstr);
                    else
                        System.out.printf (
                            " %-16s %s .. %s\n",
                            idstr,
                            df.format (new Date (tr [0])),
                            df.format (new Date (tr [1]))
                        );
                }           
            }
            
            return (true);
        }

        if (key.equalsIgnoreCase("entity")) {
            if (!dbmgr.checkStream())
                return (true);

            if (args == null) {
                System.out.println ("Usage: entity change <SRC_SYMBOL:SRC_TYPE> <TRG_SYMBOL:TRG_TYPE>\n" +
                    "   or: entity rename");
                return (true);
            }

            List<String> list = new ArrayList<String>();
            Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(args);
            while (m.find())
                list.add(m.group(1).replace("\"", ""));
            String[] instruments = list.toArray(new String[list.size()]);

            if (instruments[0].equalsIgnoreCase("rename"))
                doRenameInstruments();
            else if (instruments[0].equalsIgnoreCase("change"))
                doChangeInstruments(instruments);
            else if (instruments[0].equalsIgnoreCase("list"))
                doChangeInstrumentsList();
            else if (instruments[0].equalsIgnoreCase("clear"))
                doChangeInstrumentsClear();
            else
                System.out.println("Usage: entity change <SRC_SYMBOL:SRC_TYPE> <TRG_SYMBOL:TRG_TYPE>\n" +
                    "   or: entity rename");

            return (true);
        }
        
        if (key.equalsIgnoreCase ("copyfrom")) {
            if (!dbmgr.checkSingleStream ())
                return (true);
            
            DXTickStream          dest = dbmgr.getStreams () [0];
            DXTickStream []       src = dbmgr.getStreams (args);

            if (src == null)
                return (true);

            for (TickStream s : src)
                if (s == dest) {
                    System.out.println ("Destination stream " + dest.getKey () + " may not be one of the sources.");
                    return (true);
                }

            final TickLoader loader = dest.createLoader (new LoadingOptions (true));
            final SchemaConverter   converter = createConverter (dest, src);

            if (converter == null)
                return (true);

            MessageChannel<InstrumentMessage> channel = new MessageChannel<InstrumentMessage>() {
                @Override
                public void close () {
                    Util.close (loader);
                }

                @Override
                public void send (InstrumentMessage msg) {
                    RawMessage converted = converter.convert ((RawMessage) msg);
                    if (converted != null)
                        loader.send (converted);
                }
            };

            try {
                // use raw mode
                SelectionOptions options = selector.getSelectionOptions();
                options.raw = true;
                options.versionTracking = false;
                options.live = false;

                export(src, channel, options);
            } finally {
                loader.close ();
            }

            return (true);
        }
                
        if (key.equalsIgnoreCase ("import")) {

            if (srcMsgFile == null) {
                System.out.println("Source file not set. Use set src first.");
                return false;
            }

            if (args == null) {
                if (!dbmgr.checkSingleStream ())
                    return (true);

                ImportExportHelper.filterMessageFile(srcMsgFile, dbmgr.getStreams()[0], selector);
            } else if (args.length() > 0) {
                DXTickStream stream = dbmgr.getStream(args);
                if (stream == null) {
                    MessageFileHeader header = Protocol.readHeader(srcMsgFile);

                    StreamOptions options = new StreamOptions();
                    options.name = args;
                    options.setPolymorphic(header.getTypes());
                    options.distributionFactor = dbmgr.getDF();

                    DXTickStream target = dbmgr.getDB().createStream(options.name, options);
                    ImportExportHelper.filterMessageFile(srcMsgFile, target, selector);
                }
            }

            return (true);
        }

        if (key.equalsIgnoreCase ("export")) {
            if (!dbmgr.checkStream ())
                return (true);

            if (args == null) {
                System.out.println ("Usage: export <destination>");
                return (true);
            }

            SelectionOptions options = selector.getSelectionOptions();

            Interval periodicity = dbmgr.getStreams ().length == 1 ? dbmgr.getStreams ()[0].getPeriodicity().getInterval() : null;
            MessageWriter2  writer = MessageWriter2.create (
                    new File(args),
                    periodicity,
                    options.raw ? null : TypeLoaderImpl.DEFAULT_INSTANCE,
                    collectTypes (dbmgr.getStreams ())
            );

            try {
                export (dbmgr.getStreams(), writer, options);
            } finally {
                writer.close ();
            }
            
            return (true);
        }

//        if (key.equalsIgnoreCase("quote")) {
//            if (!dbmgr.checkStream())
//                return (true);
//
//            final long ts = TimeKeeper.currentTime;
//            MessageSource<InstrumentMessage> cur = selectQuotes();
//
//            printCursor (cur);
//
//            if (!Util.QUIET)
//                System.out.println("total time (ms): " + (TimeKeeper.currentTime - ts));
//            return (true);
//        }

         if (key.equalsIgnoreCase("delete")) {
            if (!dbmgr.checkStream())
                return (true);

            if (args == null) {
                System.out.println ("Usage: delete <time_from>;<time_to>");
                return (true);
            }
            String[] arguments =  StringUtils.split(args,";",true,false);
            TimeStamp argTimeFrom = new TimeStamp();
            if(arguments[0] == null || arguments[0].isEmpty()) {
                argTimeFrom.setTime(0);
            } else {
                argTimeFrom.setTime(df.parse(arguments[0]).getTime());
            }

            TimeStamp argTimeTo = new TimeStamp();
            if(arguments.length < 2) {
                 argTimeTo.setNanoTime(Long.MAX_VALUE);
            } else {
                 argTimeTo.setTime(df.parse(arguments[1]).getTime());
            }

            if(argTimeFrom.getTime()>argTimeTo.getTime()) {
                System.out.println ("First parameter can't be greater than second");
                return (true);
            }
            final long ts = TimeKeeper.currentTime;

            for (TickStream stream : dbmgr.getStreams ()) {
                ((DXTickStream)stream).delete(argTimeFrom, argTimeTo);
            }

            if (!Util.QUIET)
                System.out.println("total time (ms): " + (TimeKeeper.currentTime - ts));
            return (true);
         }
         if (key.equalsIgnoreCase("truncate")) {
            if (!dbmgr.checkStream())
                return (true);

            if (args == null) {
                System.out.println ("Usage: truncate <time>");
                return (true);
            }
            long argTime = df.parse (args).getTime ();

            final long ts = TimeKeeper.currentTime;

            for (TickStream stream : dbmgr.getStreams ()) {
                ((DXTickStream)stream).truncate(argTime);
            }

            if (!Util.QUIET)
                System.out.println("total time (ms): " + (TimeKeeper.currentTime - ts));
            return (true);
        }

        if (key.equalsIgnoreCase("purge")) {
            if (!dbmgr.checkStream())
                return (true);

            if (args == null) {
                System.out.println ("Usage: purge <time>");
                return (true);
            }
            long argTime = df.parse (args).getTime ();

            final long ts = TimeKeeper.currentTime;

            for (DXTickStream stream : dbmgr.getStreams ()) {
                stream.purge(argTime); // now synchronous
            }

            if (!Util.QUIET)
                System.out.println("Purge total time (ms): " + (TimeKeeper.currentTime - ts));
            return (true);
        }

        if (key.equalsIgnoreCase("rename")) {
            if (!dbmgr.checkStream())
                return (true);

            if (args == null) {
                System.out.println ("Usage: rename <key>");
                return (true);
            }             

            final long ts = TimeKeeper.currentTime;

            dbmgr.getSingleStream().rename(args);

            if (!Util.QUIET)
                System.out.println("total time (ms): " + (TimeKeeper.currentTime - ts));
            return (true);
        }

        if (key.equalsIgnoreCase("cleanup")) {
            if (!dbmgr.checkStream())
                return (true);

            final long ts = TimeKeeper.currentTime;

            cleanupEntities(dbmgr.getSingleStream());

            if (!Util.QUIET)
                System.out.println("total time (ms): " + (TimeKeeper.currentTime - ts));
            return (true);
        }

        if (key.equalsIgnoreCase("send")) {
            if (!dbmgr.checkStream())
                return (true);

            if (args == null) {
                System.out.println ("Usage: send <JSON array of messages>");
                return (true);
            }

            final long ts = TimeKeeper.currentTime;

            try {
                JSONHelper.parseAndLoad(args, dbmgr.getSingleStream());
            } catch (Exception exc) {
                System.out.println("Got exception while performing operation.");
                exc.printStackTrace();
            }
            if (!Util.QUIET)
                System.out.println("total time (ms): " + (TimeKeeper.currentTime - ts));

            return (true);
        }

        return (super.doCommand (key, args));
    }

    public static SchemaConverter      createConverter (DXChannel dest, DXTickStream... src) {
        final RecordClassSet out = new RecordClassSet ();
        out.addContentClasses(dest.getTypes());
        MetaDataChange.ContentType outType = MetaDataChange.ContentType.Polymorphic;

        final SimpleClassSet in = new SimpleClassSet ();
        for (DXTickStream t : src)
            in.addContentClasses(DXTickStream.getClassDescriptors(t));

        MetaDataChange.ContentType inType = src.length > 1 ? MetaDataChange.ContentType.Mixed :
                        (src[0].isFixedType() ? MetaDataChange.ContentType.Fixed : MetaDataChange.ContentType.Polymorphic );

        final SchemaConverter converter = new SchemaConverter (
                SchemaAnalyzer.DEFAULT.getChanges(in, inType, out, outType));

        if (!converter.canConvert()) {
            System.out.println ("Source and destination streams in not compatible.");
            return (null);
        }
        
        return (converter);
    }
    
    public TickLoader   createLoader (DXTickStream dest, boolean raw, DXTickStream ... excludeStreams) {
        for (DXTickStream s : excludeStreams)
            if (s == dest) {
                System.out.println ("Destination stream " + dest.getKey () + " may not be one of the sources.");
                return (null);
            }

        return (dest.createLoader (new LoadingOptions (raw)));
    }

    public MessageChannel<InstrumentMessage>   createPublisher (DXChannel<InstrumentMessage> dest, boolean raw, DXChannel ... exclude) {
        for (DXChannel s : exclude)
            if (s == dest) {
                System.out.println ("Destination stream " + dest.getKey () + " may not be one of the sources.");
                return (null);
            }

        return dest.createPublisher(new LoadingOptions (raw));
    }
        
    private void                export (
            DXTickStream [] src,
            MessageChannel <InstrumentMessage>  dest,
            SelectionOptions options
    )
    {
        long []                 tr = TickTools.getTimeRange (src);

        if (tr == null) {
            System.out.println ("No data in source.");
            return;
        }

        long startTime = selector.getTime();
        if (startTime > tr[0] && startTime != TimeConstants.TIMESTAMP_UNKNOWN)
            tr[0] = startTime;

        long endTime = selector.getEndtime();
        if (endTime < tr[1] && endTime != TimeConstants.TIMESTAMP_UNKNOWN)
            tr[1] = endTime;

        ConsoleProgressIndicator    cpi = new ConsoleProgressIndicator ();

        System.out.println ("Copying ...");

        MessageSource <InstrumentMessage>  cur = selector.select(tr[0], options, src);

        try {
            TickTools.copy (cur, tr, dest, cpi);
        } finally {
            System.out.println ();
            cur.close ();
        }
    }

    private void                filterMessageFile (File srcMsgFile, String dest)
        throws IOException, ClassNotFoundException
    {
        System.out.println("Filtering; hit <Enter> to abort ...");

        MessageFileHeader header = Protocol.readHeader(srcMsgFile);

        boolean v0 = header.version == 0;

        ConsumableMessageSource<InstrumentMessage> reader =
                Protocol.openReader(srcMsgFile, 8*1024, null);

        MessageWriter2 writer = MessageWriter2.create (new File (dest),
                header.periodicity,
                v0 ? Protocol.getDefaultTypeLoader() : null,
                header.getTypes());
        loadTicks(reader, writer);
    }

    public static void          loadMessageFile (File srcMsgFile, DXTickStream stream)
        throws IOException
    {
        new TickDBShell ().filterMessageFile (srcMsgFile, stream);
    }
    
    private void                filterMessageFile (File srcMsgFile, DXTickStream stream)
        throws IOException
    {
        if (!Util.QUIET)
            System.out.println("Importing; hit <Enter> to abort ...");

        DBLock lock = null;
        try {
            if (SECURITIES_STREAM.equalsIgnoreCase(stream.getKey()))
                lock = stream.tryLock(LockType.WRITE, 5000L);

            RecordClassDescriptor[] outTypes = stream.isFixedType() ?
                    new RecordClassDescriptor[]{stream.getFixedType()} :
                    stream.getPolymorphicDescriptors();

            MessageFileHeader header = Protocol.readHeader(srcMsgFile);

            boolean firstVersion = header.version == 0;

            if (firstVersion) {
                throw new IllegalArgumentException("Version 0 is not supported.");
            } else if (!firstVersion) {
                // check dbmgr.getStreams () for compatibility
                RecordClassDescriptor[] inTypes = header.getTypes();
                if (!MessageProcessor.isCompatible(inTypes, outTypes)) {
                    throw new IllegalArgumentException("Input types (" + MessageProcessor.toDetailedString(inTypes) +
                            ") \nis not compatible with \noutput types (" +
                            MessageProcessor.toDetailedString(outTypes) + ")");
                }
            }

            final TickLoader loader = stream.createLoader(new LoadingOptions(!firstVersion));

            final ConsumableMessageSource<InstrumentMessage> reader =
                    Protocol.createReader(srcMsgFile, outTypes);

            loadTicks(reader, loader);
        } finally {
            if (lock != null)
                lock.release();
        }
    }

    private void                loadTicks (
        ConsumableMessageSource <InstrumentMessage> reader,
        MessageChannel <InstrumentMessage>          writer
    )
        throws IOException
    {
        try {
            ConsoleProgressIndicator cpi = new ConsoleProgressIndicator();

            if (!Util.QUIET)
                cpi.setTotalWork(1);

            long inCount = 0;
            long outCount = 0;

            while (reader.next()) {
                InstrumentMessage msg = reader.getMessage();

                if (msg.getTimeStampMs() < selector.getTime()) // filter messages by start time
                    continue;

                inCount++;

                if (inCount % 1000 == 0) {
                    if (!Util.QUIET)
                        cpi.setWorkDone(reader.getProgress());

                    if (checkInterrupt())
                        break;
                }

                
                if (selector.accept (msg)) {
                    outCount++;
                    writer.send(msg);           
                }
                
                if (selector.enough (msg))
                    break;
            }

            writer.close();
            writer = null;

            if (!Util.QUIET)
                System.out.printf("\nIn: %,d messages; out: %,d messages.\n", inCount, outCount);
        } finally {
            Util.close(reader);
            Util.close(writer);
        }
    }

    private Collection <IdentityKey> listEntities () {
        if (!dbmgr.checkStream ())
            return (null);

        TreeSet <IdentityKey>    ids = new TreeSet <IdentityKey> ();
        
        for (TickStream s : dbmgr.getStreams ()) 
            for (IdentityKey id : s.listEntities ())
                ids.add (id);

        return (ids);
    }

    private void                        doRenameInstruments() throws Exception {
        final long ts = TimeKeeper.currentTime;
        for (DXTickStream stream : dbmgr.getStreams ()) {
            try {
                DBLock writeLock = stream.tryLock(LockType.WRITE, 5000L);
                try {
                    stream.renameInstruments(
                        fromEntities.toArray(new IdentityKey[fromEntities.size()]),
                        toEntities.toArray(new IdentityKey[toEntities.size()]));

                    if (stream.getFormatVersion() < 5) {
                        waitAndDrawProgressBar(stream);
                    }
                } finally {
                    if (writeLock != null)
                        writeLock.release();
                }
            } finally {
            }
        }
        if (!Util.QUIET)
            System.out.println("\ntotal time (ms): " + (TimeKeeper.currentTime - ts));
    }

    private void                        waitAndDrawProgressBar(DXTickStream stream) throws InterruptedException {
        ConsoleProgressIndicator cpi = new ConsoleProgressIndicator();
        cpi.setTotalWork(1.0);

        boolean complete = false;
        while (!complete) {
            BackgroundProcessInfo process = stream.getBackgroundProcess();
            complete = (process != null && process.isFinished());
            cpi.setWorkDone(process == null ? 0 : process.progress);
            Thread.sleep(100);
        }
    }

    private void                        doChangeInstruments(String[] instruments) {
        if (instruments.length < 3) {
            System.out.println ("Usage: entity change <SRC_SYMBOL> <TRG_SYMBOL>\n" +
                "   or: entity rename");
            return;
        }

        IdentityKey from = new ConstantIdentityKey(instruments[1]);
        IdentityKey to = new ConstantIdentityKey(instruments[2]);

        fromEntities.add(from);
        toEntities.add(to);
        doChangeInstrumentsList();
    }

    private void                      doChangeInstrumentsList() {
        System.out.println("Entities for renaming:");
        for (int i = 0; i < fromEntities.size(); ++i) {
            System.out.println("\t" + fromEntities.get(i) + " > " + toEntities.get(i));
        }
    }

    private void                       doChangeInstrumentsClear() {
        fromEntities.clear();
        toEntities.clear();
    }

    public static boolean             checkInterrupt () throws IOException {
        if (System.in.available () == 0)
            return (false);
        
        while (System.in.available () > 0)
            System.in.read ();
        
        System.out.print ("    STOP CURRENT OPERATION (y,n) ? ==> ");
        int        ch = System.in.read ();

        while (System.in.available () > 0)
            System.in.read ();
        
        return (ch == 'Y' || ch == 'y');
    }

    
//    private InstrumentMessageSource select (SelectionOptions options) {
//        return select(time, options, dbmgr.getStreams ());
//    }

    String []           splitSymbols (String s) {
        if (spaceSep)
            return (s.split ("[\n\r\t ,;]+"));
        else {
            String []   ss = s.split ("[\n\r\t,;]+");

            for (int ii = 0; ii < ss.length; ii++)
                ss [ii] = ss [ii].trim ();

            return (ss);
        }
    }

    private String []           readSymbols (String filePath) throws Exception {
        return (splitSymbols (IOUtil.readTextFile (filePath)));
    }
    
    
    // NB: the function below is copied from deltix.qsrv.hf.tickdb.ui.administrator.util.Common  
    // to skip unnecessary dependencies in qsc.dll
    public static RecordClassDescriptor[] collectTypes (final TickStream... streams) {
        final List<RecordClassDescriptor> types = new ArrayList<RecordClassDescriptor> ();
        for (final TickStream stream : streams) {
            if (stream.isFixedType ())
                types.add (stream.getFixedType ());
            else
                Collections.addAll (types,
                                    stream.getPolymorphicDescriptors ());
        }

        return types.toArray (new RecordClassDescriptor[types.size ()]);
    }

    public boolean          isTiming () {
        return timing;
    }

    public void             setTiming (boolean timing) {
        this.timing = timing;
    }   
    
    public void             setTz (TimeZone tz) {
        this.tz = tz;
    }
               
    public static double       parseDoubleOrOff (String value) {
        return (value.equals ("off") ? Double.NaN : Double.parseDouble (value));
    }
    
    public static String        formatDoubleOrOff (double d) {
        return (Double.isNaN (d) ? "off" : String.valueOf (d));
    }
    
    public static String        getMultiLineInput (String init, String fileId, LineNumberReader reader)
        throws IOException, InterruptedException 
    {
        StringBuilder   sb = new StringBuilder ();
        
        if (init != null) {
            sb.append (init);
            sb.append ('\n');
        }
        
        for (;;) {
            String      line = reader.readLine ();
            
            if (line == null)
                break;
            
            if (!fileId.equals (STDIN_FILEID))
                outWriter.println (line);
            
            if (line.trim ().equals ("/"))
                break;
            
            sb.append (line);
            sb.append ('\n');
        }
        
        return (sb.toString ());
    }

    public static void cleanupEntities(DXTickStream stream) {
        List<IdentityKey> ids = new ArrayList<>();
        for (IdentityKey identity : stream.listEntities()) {
            long[] timeRange = stream.getTimeRange(identity);
            if (isUndefined(timeRange))
                ids.add(identity);
        }
        if (ids.size() != 0) {
            IdentityKey[] array = ids.toArray(new IdentityKey[]{});
            stream.clear(array);
            System.out.printf("Entities with no data removed from the stream: %s.\n", Arrays.toString(array));
        } else {
            System.out.println("Nothing to clean up.");
        }
    }

    private static boolean isUndefined(long[] timeRange) {
        return timeRange == null || timeRange.length != 2 ||
                timeRange[0] == Long.MAX_VALUE || timeRange[0] == Long.MIN_VALUE ||
                timeRange[1] == Long.MAX_VALUE || timeRange[1] == Long.MIN_VALUE;
    }

}
