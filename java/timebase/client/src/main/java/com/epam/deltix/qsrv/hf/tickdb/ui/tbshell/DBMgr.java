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
import com.epam.deltix.qsrv.hf.pub.MappingTypeLoader;
import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.timebase.messages.*;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.TickDBClient;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.DBLock;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockType;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.DirectChannel;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;
import com.epam.deltix.qsrv.hf.tickdb.tool.TDBDowngrade;
import com.epam.deltix.util.collections.generated.ObjectToObjectHashMap;
import com.epam.deltix.util.lang.*;

import java.io.IOException;
import java.util.*;

/**
 *
 */
public class DBMgr {
    private final TickDBShell   shell;
    private long                ramdisksize = DataCacheOptions.DEFAULT_CACHE_SIZE;
    private DXTickDB            db = null;
    private DXTickStream []     streams = null;
    private DXChannel []        channels = null;
    private ShutdownHook        shutdownHook;
    private boolean             compression = false;
    private int                 targetDF = -1;

    private final ObjectToObjectHashMap<String, DBLock> locks = new ObjectToObjectHashMap<>();

    public DBMgr (TickDBShell shell) {
        this.shell = shell;
    }

    public void                 setDb (DXTickDB db) {
        this.db = db;
    }

    public void                 setStreams (DXTickStream ... streams) {
        this.streams = streams;
    }

    public DXTickDB             getDB () {
        return (db);
    }

//    public boolean              hasSelectedStreams() {
//        return streams != null && streams.length > 0;
//    }

    public DXTickStream []      getStreams () {
        return (streams);
    }

    public DXTickStream         getSingleStream () {
        return (streams[0]);
    }

    private String              targetDFString () {
        switch (targetDF) {
            case -1:    return "keep";
            case 0:     return "max";
            default:    return String.valueOf (targetDF);
        }
    }

    public int getDF() {
        return targetDF;
    }

    protected void              doSet () {
        System.out.println ("db:            " + db);
        System.out.println ("stream:        " + pstreams ());
        System.out.println ("ramdisksize:   " + ramdisksize);
        System.out.println ("compression:   " + compression);
        System.out.println ("targetDF:      " + targetDFString ());
    }

    boolean                     doSet (String option, String value) throws Exception {
        switch (option) {
            case "db":
                if (db != null) {
                    db.close ();
                    db = null;
                }

                if (TickDBFactory.isRemote(value))
                    db = TickDBFactory.createFromUrl (value);
                else
                    db =
                            TickDBFactory.create (
                                    new DataCacheOptions (Integer.MAX_VALUE, ramdisksize),
                                    shell.expandPath (value)
                            );

                return (true);

            case "ramdisksize":
                ramdisksize = Long.parseLong (value);
                shell.confirm ("RAM Disk size: ~" + (ramdisksize >> 20) + "MB");
                return (true);

            case "user":
                String[] login = value.split(" ");

                if (db == null) {
                    shell.error ("Database is not specified yet", 1);
                    return (true);
                }

                String id = db.getId();
                boolean readOnly = db.isReadOnly();
                db.close();

                db = TickDBFactory.createFromUrl(id, login[0], login.length > 1 ? login[1] : null);
                db.open(readOnly);
                return (true);

            case "stream": {
                DXTickStream []           tmp = getStreams (value);

                streams = tmp;
                if (tmp != null)
                    shell.confirm (pstreams ());

                return (true);
            }

            case "channel": {
                if (db instanceof TopicDB) {
                    DXChannel[] tmp = getChannels((TopicDB)db, value);

                    channels = tmp;
                    if (tmp != null)
                        shell.confirm(toString(channels));

                    return (true);
                }
                shell.error ("Cannot obtain channels", 1);
                return (false);
            }

            case "compression":
                if (db instanceof TickDBClient) {
                    compression = "on".equals(value);
                    ((TickDBClient)db).setCompression(compression);

                    if ("auto".equalsIgnoreCase (value)) {

                        System.out.println("Autodetecting best mode ...");
                        long[] times = ((TickDBClient)db).testConnectLatency(10);
                        Arrays.sort(times);
                        double factor = 1000;

                        System.out.println(String.format(
                                "Stats: [%,.3f, %,.3f, %,.3f, %,.3f, %,.3f] mks",
                                times[0] / factor, times[2] / factor, times[5] / factor, times[8] / factor, times[9] / factor ));

                        if (times[8] < 1000 * factor) { // < 1 ms
                            System.out.println("Using compression is not preferable.");
                            compression = false;
                        }
                        else if (times[5] > 10 * 1000 * factor) { // 10 ms
                            System.out.println("Compression definitely should be used");
                            compression = true;
                        }
                    }

                    shell.confirm ("Using compression: " + compression);
                } else {
                    shell.confirm("Compression available only in remote mode");
                }

                return (true);

            case "targetDF": {
                switch (value) {
                    case "keep":    targetDF = -1; break;
                    case "max":     targetDF = 0; break;
                    default:        targetDF = Integer.parseInt (value); break;
                }

                return (true);
            }
        }

        return (false);
    }

    private void        onOpen() {
        if (shutdownHook == null)
            Runtime.getRuntime().addShutdownHook(shutdownHook = new ShutdownHook(db));
    }

    private void        onClose() {
        locks.clear();
        streams = null;

        if (shutdownHook != null)
            Runtime.getRuntime().removeShutdownHook(shutdownHook);

        shutdownHook = null;
    }

    public boolean       doCommand (String key, String args)
            throws Exception
    {
        switch (key) {
            case "open": {
                if (!checkDb())
                    return true;

                db.open (args != null && args.equalsIgnoreCase ("ro"));

                onOpen();

                DXTickStream []    dbstreams = db.listStreams ();

                for (DXTickStream stream : dbstreams) {
                    if (!stream.getKey().endsWith("#")) {
                        if (streams == null) {
                            streams = new DXTickStream [] { stream };
                        } else {
                            streams = null;
                            break;
                        }
                    }
                }

                return (true);
            }

            case "format":
                if (!checkDb())
                    return true;

                db.format ();
                streams = null;

                return (true);

            case "downgrade":
                if (!checkDb())
                    return true;

                if (!db.isOpen())
                    TDBDowngrade.downgrade(db.getDbDirs());
                else
                    throw new IllegalStateException("Database is open.");

                return (true);

            case "close":
                if (!checkDb())
                    return true;

                db.close ();
                onClose();
                return (true);

            case "warmup":
                if (!checkDb())
                    return true;

                db.warmUp ();
                return (true);

            case "cooldown":
                if (!checkDb())
                    return true;

                db.coolDown ();
                return (true);

            case "trim": {
                if (!checkDb())
                    return true;

                long        before = db.getSizeOnDisk ();
                db.trimToSize ();
                long        after = db.getSizeOnDisk ();

                System.out.printf (
                        "Trimmed: %,d -> %,d bytes (%d%%).\n",
                        before,
                        after,
                        (before - after) * 100L / before
                );
                return (true);
            }

            case "showsize":
                if (!checkDb())
                    return true;

                System.out.printf ("Approximate DB size: %,d bytes\n", ((DXTickDB) db).getSizeOnDisk ());
                return (true);

            case "streams":
                if (!checkDb())
                    return true;

                for (WritableTickStream s : db.listStreams ()) {
                    if (!s.getKey ().endsWith ("#"))
                        System.out.printf (
                                "%-16s %-32s\n",
                                s.getKey (),
                                s.getName ()
                        );
                }

                return (true);

            case "channels":
                if (!checkDb())
                    return true;

                for (DXChannel s : db.listChannels()) {
                    String type = s instanceof DXTickStream ? "[stream]" : "[topic]";
                    if (!s.getKey ().endsWith ("#"))
                        System.out.printf (
                                "%-16s %-32s %s\n",
                                s.getKey (),
                                s.getName (),
                                type
                        );
                }

                return (true);

            case "delete":
                if (args != null)
                    return false;

                if (!checkStream ())
                    return (true);

                for (TickStream s : streams)
                    ((DXTickStream) s).delete ();

                streams = null;
                return (true);

            case "remove":

                // delete current selected streams/channels
                if (args != null) {
                        for (DXChannel channel : getChannels(db, args))
                        if (channel instanceof DXTickStream)
                            ((DXTickStream) channel).delete();
                        else if (db instanceof TopicDB)
                            ((TopicDB) db).deleteTopic(channel.getKey());

                    return (true);
                }

            case "mkstream": {
                if (!checkSingleStream ())
                    return (true);

                DXTickStream    template = streams [0];
                StreamOptions   opts = template.getStreamOptions ();
                opts.name = null;

                if (targetDF != -1)
                    opts.distributionFactor = targetDF;

                streams = new DXTickStream [] { db.createStream (args, opts) };

                return (true);
            }

            case "mktopic": {
                if (!checkSingleStream ())
                    return (true);

                if (!(db instanceof TopicDB))
                    return (true);

                DXTickStream    template = streams [0];
                StreamOptions   opts = template.getStreamOptions ();
                opts.name = null;

                DirectChannel channel = ((TopicDB) db).createTopic(args, opts.getMetaData().getTopTypes(), null);
                channels = new DXChannel[] {channel};

                return (true);
            }

            case "lock": {
                if (!checkSingleStream())
                    return (true);

                DBLock lock = getStreamLock(false);

                if (lock != null)
                    throw new IllegalStateException("Stream lock already acquired: " + lock);

                LockType type = LockType.WRITE;
                if (args != null && !args.isEmpty())
                    type = "r".equalsIgnoreCase(args) ? LockType.READ : type;

                DXTickStream stream = streams[0];
                System.out.println("Acquiring " + type + " lock on stream [" + stream.getKey() + "]");
                lock = stream.tryLock(type, 5000);

                locks.put(stream.getKey(), lock);

                return (true);
            }

            case "unlock": {
                DBLock lock = getStreamLock(true);

                if (lock != null) {
                    System.out.println("Releasing lock: " + lock);
                    lock.release();
                }
                else
                    System.out.println("Current stream is not locked.");

                return (true);
            }

            case "generate": {
                String []   argx = (args != null ? args.split (" ") : new String[0]);
                String name = argx.length == 1 ? argx[0] : "bars";
                createSampleStream(getDB(), name);
                return (true);
            }
        }

        return (false);
    }

    public static DXTickStream createSampleStream(DXTickDB tdb, String name) throws IOException {

        StreamOptions options = new StreamOptions();
        options.name = name;

        String priceEncoding = FloatDataType.ENCODING_SCALE_AUTO;

        DataField[] fields = new DataField[]{
                new NonStaticDataField("close", "Close", new FloatDataType(priceEncoding, true)),
                new NonStaticDataField("open", "Open", new FloatDataType(priceEncoding, true), "close"),
                new NonStaticDataField("high", "High", new FloatDataType(priceEncoding, true), "close"),
                new NonStaticDataField("low", "Low", new FloatDataType(priceEncoding, true), "close"),
                new NonStaticDataField("volume", "Volume", new FloatDataType(priceEncoding, true))
        };
        options.setFixedType(new RecordClassDescriptor("com.epam.deltix.timebase.messages.BarMessage", "Sample Bar Message", false, null, fields));

        DXTickStream stream = tdb.createStream(name, options);

        LoadingOptions lo = new LoadingOptions();

        try (TickLoader loader = stream.createLoader(lo)) {
            SimpleLoader.loadBarsFromZipResource("com/epam/deltix/qsrv/testsetup/TestBars.zip", loader);
        }

        return stream;
    }

    private DBLock            getStreamLock(boolean remove) {
        if (!checkSingleStream())
            throw new IllegalStateException("Please select single stream to lock.");

        return remove ? locks.remove(streams[0].getKey(), null) : locks.get(streams[0].getKey(), null);
    }

    public DXTickStream     getStream (String key) {
        DXTickStream    s = db.getStream (key);

        if (s == null) {
            System.out.printf ("Stream '%s' was not found.\n", key);
            return (null);
        }

        return (s);
    }

    public DXTickStream []  getStreams (String list) {
        String []               keys = shell.splitSymbols (list);

        DXTickStream[] result = Arrays.stream(keys)
                .map(key -> {
                    DXTickStream stream = db.getStream(key);
                    if (stream == null) {
                        System.out.printf("Stream '%s' was not found.\n", key);
            }
                    return stream;
                })
                .filter(Objects::nonNull)
                .toArray(DXTickStream[]::new);
        return result.length == 0 ? null: result;
    }

    public DXChannel []         getChannels (TopicDB tdb, String list) {
        String []               keys = shell.splitSymbols (list);
        int                     ns = keys.length;

        DXChannel []         tmp = new DXChannel[ns];

        for (int ii = 0; ii < ns; ii++) {
            tmp [ii] = tdb.getTopic (keys [ii]);

            if (tmp [ii] == null) {
                System.out.printf ("Channel '%s' was not found.\n", keys [ii]);
                return (null);
            }
        }

        return (tmp);
    }

    public DXChannel<InstrumentMessage>         getChannel (String key) {
        DXChannel[]         all = db.listChannels();
        return Arrays.stream(all).filter(x -> key.equals(x.getKey())).findFirst().orElse(null);
    }

    public DXChannel []         getChannels (DXTickDB tdb, String list) {
        String []               keys = shell.splitSymbols (list);
        int                     ns = keys.length;

        DXChannel[]         tmp = new DXChannel[ns];
        DXChannel[]         all = tdb.listChannels();

        for (int ii = 0; ii < ns; ii++) {
            final String key = keys[ii];
            tmp [ii] = Arrays.stream(all).filter(x -> key.equals(x.getKey())).findFirst().orElse(null);

            if (tmp [ii] == null) {
                System.out.printf ("Channel '%s' was not found.\n", keys [ii]);
                return (null);
            }
        }

        return (tmp);
    }

    public DXChannel<InstrumentMessage>          getSingleChannel () {
        return channels != null && channels.length > 0 ? channels[0] : null;
    }

    public String           pstreams () {
        return toString(streams);
    }

    public String           toString (DXChannel[] channels) {
        if (channels == null)
            return ("<null>");

        int                 ns = channels.length;

        if (ns == 1)
            return (channels [0].getKey ());

        StringBuilder       sb = new StringBuilder (streams [0].getKey ());

        for (int ii = 1; ii < ns; ii++) {
            sb.append (", ");
            sb.append (channels [ii].getKey ());
        }

        return (sb.toString ());
    }

    public boolean         checkStream () {
        if (streams == null)
            shell.error ("Current stream is not chosen. Use set stream <key>", 1);

        return (streams != null);
    }

    public boolean         checkChannels () {
        if (channels == null && streams == null)
            shell.error ("Current stream (or channel) is not chosen. Use set stream <key> or set channel <key>", 1);

        return (streams != null || channels != null);
    }

    public boolean         checkDb () {
        if (db == null)
            shell.error ("Current database is not chosen. Use 'set db <path>'", 1);

        return (db != null);
    }

    public boolean         checkSingleStream () {
        if (!checkStream ())
            return (false);

        if (streams.length > 1)
            System.out.println ("Operation will be applied to stream " + streams [0].getKey () + " only.");

        return (true);
    }


}