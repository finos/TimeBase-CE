/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.tickdb.comm.client.TickDBClient;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.qsrv.test.messages.BestBidOfferMessage;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.SecuredDbClient;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.DataCacheOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.DirectTickDBClient;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeBaseServerRegistry;
import com.epam.deltix.qsrv.hf.tickdb.pub.TopicDBFactory;
import com.epam.deltix.qsrv.hf.tickdb.test.EmbeddedServer;
import com.epam.deltix.timebase.messages.TimeStampedMessage;
import com.epam.deltix.util.io.GUID;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.vsocket.TransportProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Random;

public class TDBRunner {

    static {
        System.setProperty(TickDBFactory.VERSION_PROPERTY, "5.0");
    }

    protected final boolean     remote;
    protected final boolean     embeddedTopics; // Enables topic support in local mode
    private boolean             doFormat = true;
    private boolean             cleanup = false;

    private DXTickDB            db;
    private DXTickDB            embeddedDb; // Embedded set - set in local mode with "embeddedTopics" enabled
    private EmbeddedServer      server; // Embedded server - set in remote mode

    private DXTickDB            client;
    private final String        location;

    public DataCacheOptions     options = new DataCacheOptions();
    public String               user = null;
    public String               pass = null;
    public TransportProperties  transportProperties;
    public boolean              useSSL;
    public SSLContext           sslContext;

    private int port = 0;

    public static String        getTemporaryLocation() {
        return getTemporaryLocation("tickdb");
    }

    public static String        getTemporaryLocation(String subpath) {
        File random = Home.getFile("temp" + File.separator + new GUID().toString() + File.separator + subpath);

        if (random.mkdirs())
            random.deleteOnExit();

        return random.getAbsolutePath();
    }

    public TDBRunner(boolean isRemote) {
        this(isRemote, true);
    }

    public TDBRunner(boolean isRemote, boolean doFormat) {
        this(isRemote, doFormat, getTemporaryLocation());
    }

    public TDBRunner(boolean isRemote, boolean doFormat, String location) {
        this(isRemote, doFormat, location, null, false);
    }

    public TDBRunner(boolean isRemote, boolean doFormat, EmbeddedServer server){
        this(isRemote, doFormat, getTemporaryLocation(), server);
    }

    public TDBRunner(boolean isRemote, boolean doFormat, String location, EmbeddedServer server) {
        this(isRemote, doFormat, location, server, false);
    }

    public TDBRunner(boolean isRemote, boolean doFormat, String location, EmbeddedServer server, boolean enableLocalTopics) {
        this.remote = isRemote;
        this.doFormat = doFormat;
        this.location = location;
        this.server = server;
        this.embeddedTopics = enableLocalTopics;
    }

    public void                 startup() throws Exception {
        File folder = new File(location);

        if (doFormat)
            IOUtil.removeRecursive(folder);

        if (!folder.exists() && !folder.mkdirs())
            throw new IllegalStateException("Unable to create " + folder);

        // Define QSHome
        System.setProperty ("deltix.qsrv.home", folder.getParent()); // TODO: QS Home

        if (!remote) {
            DXTickDB tickDB = TickDBFactory.create(options, folder);
            if (embeddedTopics) {
                tickDB.open(false);
                this.embeddedDb = TopicDBFactory.create(tickDB);
                this.db = new DirectTickDBClient(this.embeddedDb);
            } else {
                this.db = tickDB;
            }
        } else {
            if (server == null)
                throw new IllegalStateException("EmbeddedServer is undefined");

            this.port = server.start();

            TickDBClient connection = createClient();
            connection.setSslContext(sslContext);
            client = connection;

//            // TODO: Find out what kind of workers we want to start.
//            // start workers
//            for (int i = 0; i < 4; i++) {
//                new QuickExecutor.QuickTask()  {
//                    @Override
//                    public void run() throws InterruptedException {
//                        Thread.sleep(100);
//                    }
//                }.submit();
//            }
        }

        open(false);
    }

    public TickDBClient         createClient() {
        TickDBClient result;
        if (user != null && pass != null) {
            result = new SecuredDbClient("localhost", this.port, useSSL, user, pass);
        } else {
            result = (TickDBClient) TickDBFactory.connect("localhost", this.port, useSSL);
        }

        TickDBFactory.setApplicationName(result, "TBRunner");
        return result;
    }

    public void                 shutdown() throws Exception {
        close();

        if (db != null)
            db.close();

        if (embeddedDb != null)
            embeddedDb.close();

        if (server != null)
            server.stop();

        TimeBaseServerRegistry.clear();

        if (cleanup) {
            try {
                IOUtil.delete(new File(location));
            } catch (IOException e) {
                System.out.println("WARNING: cleanup failed: " + e.getStackTrace()[1] + "\nDir: " + location + "\nException: " + e.getMessage());
            }
        }
    }

    public DXTickDB             getTickDb() {
        return remote ? client : getServerDb();
    }

    public DXTickDB             getServerDb() {
        return db != null ? db : server.getDB();
    }

    public void              open(boolean ro) {
        getTickDb().open(ro);
    }

    public void              close() {
        TickDB xdb = getTickDb();
        if (xdb != null)
            xdb.close();
    }

    public void                 setDoFormat(boolean doFormat) {
        this.doFormat = doFormat;
    }

    public void                 setPort(int port) {
        this.port = port;
    }

    public int                  getPort() {
        return this.port;
    }

    public int                  getWebPort() {
        return server.getWebPort();
    }

    public DXTickStream     createStream(DXTickDB db, String key, StreamOptions so) {
        DXTickStream stream = db.getStream(key);
        if (stream != null)
            stream.delete();

        return db.createStream(key, so);
    }

    public String               getLocation() {
        return location;
    }

    /**
     * Enables cleanup on shutdown (all local data gets deleted).
     */
    public void setCleanup(boolean cleanup) {
        this.cleanup = cleanup;
    }

    @SuppressFBWarnings(value = "PREDICTABLE_RANDOM", justification = "Random is used for fuzzy tests, not cryptography")
    public static class BBOGenerator extends BaseGenerator<InstrumentMessage>
    {
        private Random rnd = new Random();
        private int count;

        public BBOGenerator(GregorianCalendar calendar, int barSize, int count, String ... tickers) {
            super(calendar, barSize, tickers);
            this.count = count;
        }

        public boolean next() {
            if (isAtEnd())
                return false;

            BestBidOfferMessage message = new BestBidOfferMessage();
            message.setSymbol(symbols.next());
            message.setTimeStampMs(symbols.isReseted() ? getNextTime() : getActualTime());
            message.setCurrencyCode((short)840);

            if (count % 2 == 0) {
                message.setBidPrice(rnd.nextDouble()*100);
                message.setBidSize(rnd.nextInt(1000));
            } else {
                message.setOfferPrice(rnd.nextDouble()*100);
                message.setOfferSize(rnd.nextInt(1000));
            }

            current = message;
            count--;
            return true;
        }

        public boolean isAtEnd() {
            return count == 0;
        }
    }

    @SuppressFBWarnings(value = "PREDICTABLE_RANDOM", justification = "Random is used for fuzzy tests, not cryptography")
    public static class TradesGenerator extends BaseGenerator<InstrumentMessage>
    {
        private Random rnd = new Random();
        private int count;

        public TradesGenerator(GregorianCalendar calendar, int size, int count, String ... tickers) {
            super(calendar, size, tickers);
            this.count = count;
        }

        public boolean next() {
            if (isAtEnd())
                return false;

            TradeMessage message = new TradeMessage();
            message.setSymbol(symbols.next());
            message.setTimeStampMs(symbols.isReseted() ? getNextTime() : getActualTime());

            message.setPrice(rnd.nextDouble()*100);
            message.setSize(rnd.nextInt(1000));
            message.setCurrencyCode((short)840);

            current = message;
            count--;
            return true;
        }

        public boolean isAtEnd() {
            return count == 0;
        }
    }

    @SuppressFBWarnings(value = "PREDICTABLE_RANDOM", justification = "Random is used for fuzzy tests, not cryptography")
    public static class BarsGenerator extends BaseGenerator<InstrumentMessage>
    {
        private Random rnd = new Random();
        private int count;

        public BarsGenerator(GregorianCalendar calendar, int interval, int count, String ... tickers) {
            super(calendar, interval, tickers);
            this.count = count;
        }

        public boolean next() {
            if (isAtEnd())
                return false;

            BarMessage message = new BarMessage();
            message.setSymbol(symbols.next());
            message.setTimeStampMs(symbols.isReseted() ? getNextTime() : getActualTime());

            message.setHigh(getDouble(100, 6));
            message.setOpen(message.getHigh() - getDouble(10, 6));
            message.setClose(message.getHigh() - getDouble(10, 6));
            message.setLow(Math.min(message.getOpen(), message.getClose()) + getDouble(10, 6));
            message.setVolume(count);
            message.setCurrencyCode((short)840);

            current = message;
            count--;
            return true;
        }

        public double getDouble(int max, int precision) {
            return (int)(rnd.nextDouble() * Math.pow(10, max + precision)) / Math.pow(10, precision);
        }

        public boolean isAtEnd() {
            return count == 0;
        }
    }

    public static class CycleIterator<T> {
        private Collection<T> data;
        private Iterator<T> it;
        private boolean reseted;

        public CycleIterator(Collection<T> data) {
            this.data = data;
        }

        public boolean isReseted() {
            return reseted;
        }

        public T next() {
            reseted = it != null && !it.hasNext();

            if (it == null || !it.hasNext())
                it = data.iterator();

            return it.next();
        }
    }

    public static abstract class BaseGenerator<T> implements MessageSource<T>
    {
        protected GregorianCalendar calendar;
        private int interval;
        protected T current;
        protected CycleIterator<String> symbols;

        protected BaseGenerator(GregorianCalendar calendar, int interval, String ... tickers) {
            this.calendar = calendar;
            this.interval = interval;
            this.symbols = new CycleIterator<String>(
                    Arrays.asList(tickers != null && tickers.length > 0 ? tickers : new String[] {"ES"} ));
        }

        public T getMessage() {
            return current;
        }

        protected long getNextTime() {
            if (calendar != null) {
                calendar.add(Calendar.MILLISECOND, interval);
                return calendar.getTimeInMillis();
            }
            return TimeStampedMessage.TIMESTAMP_UNKNOWN;
        }

        public long getActualTime() {
            return calendar != null ? calendar.getTimeInMillis() : TimeStampedMessage.TIMESTAMP_UNKNOWN;
        }

        public void setSymbols(String ... tickers) {
            this.symbols = new CycleIterator<String>(Arrays.asList(tickers));
        }

        public void close() {
        }
    }
}