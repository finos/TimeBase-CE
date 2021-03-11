package com.epam.deltix.qsrv.hf.tickdb.tests.procs;

import com.epam.deltix.anvil.util.ShutdownSignal;
import com.epam.deltix.gflog.Log;
import com.epam.deltix.gflog.LogFactory;
import com.epam.deltix.qsrv.test.messages.MarketMessage;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.tests.TestUtils;
import com.epam.deltix.qsrv.hf.tickdb.tests.TestUtils.DBWrapper;
import com.epam.deltix.qsrv.hf.tickdb.tests.TestUtils.MessagesMonitor;
import com.epam.deltix.qsrv.hf.tickdb.tests.reports.JsonReport;
import com.epam.deltix.qsrv.hf.tickdb.tests.reports.Report;
import com.epam.deltix.util.cmdline.DefaultApplication;
import com.epam.deltix.util.concurrent.IntermittentlyAvailableCursor;
import com.epam.deltix.util.concurrent.NextResult;
import com.epam.deltix.util.concurrent.UnavailableResourceException;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class TestReader extends DefaultApplication implements Runnable {

    private static final Log LOG = LogFactory.getLog(TestReader.class);

    private final String reportsDir;
    private final ShutdownSignal shutdownSignal = new ShutdownSignal();

    public TestReader(String[] args) {
        super(args);
        reportsDir = getArgValue("-reports", null);
    }

    @Override
    public void run() {
        int readers = getIntArgValue("-readers", 5);
        String streamKey = getArgValue("-stream", "testStream");
        String dbUrl = getArgValue("-db", "dxtick://localhost:8011");
        boolean raw = isArgSpecified("-raw");
        boolean periodical = isArgSpecified("-periodical");
        int symbols = getIntArgValue("-symbols", 100);
        LOG.info().append("Starting ")
                .append(readers)
                .append(" live read tasks on stream ")
                .append(streamKey).append(".")
                .commit();
        ExecutorService readersService = Executors.newFixedThreadPool(readers + 1);
        for (int i = 0; i < readers; i++) {
            if (raw) {
                readersService.execute(new LiveReadTask(String.format("Reader-%s-%d", streamKey, i), dbUrl, streamKey, raw, x -> {}, symbols));
            } else {
                readersService.execute(new LiveReadTask(String.format("Reader-%s-%d", streamKey, i), dbUrl, streamKey, raw, new DataConsumer(), symbols));
            }
        }

        if (periodical) {
            readersService.execute(new ReadTask(dbUrl, streamKey, null));
        }

        new Scanner(System.in).nextLine();
        shutdownSignal.signal();
        System.exit(0);
    }

    public static class DataConsumer implements Consumer<InstrumentMessage> {

        long lastSequence = Long.MIN_VALUE;

        @Override
        public void accept(InstrumentMessage msg) {
            long sequence = ((MarketMessage) msg).getOriginalTimestamp();
            if (sequence < lastSequence)
                System.out.println("Message goes back: " +  sequence + " -> " + lastSequence );
            lastSequence = sequence;
        }
    }


    public class LiveReadTask implements Runnable {
        private final String key;
        private final Consumer<InstrumentMessage> consumer;
        private final DBWrapper wrapper;
        private final String id;
        private final boolean raw;
        private final int symbols;

        LiveReadTask(String id, String url, String key, boolean raw, Consumer<InstrumentMessage> consumer, int symbols) {
            this.id = id;
            this.wrapper = new DBWrapper(url);
            this.key = key;
            this.consumer = consumer;
            this.raw = raw;
            this.symbols = symbols;
        }

        @Override
        public void run() {
            MessagesMonitor messagesMonitor = new MessagesMonitor(60000, id);
            if (reportsDir != null) {
                Report report = new JsonReport();
                report.addMetric("messagesRate", messagesMonitor.getMetric());
                TestUtils.scheduleReportFlush(report, Paths.get(reportsDir, id + ".json"));
            }
            try {
                messagesMonitor.start();
                while (!shutdownSignal.isSignaled()) {
                    try {
                        runUnchecked(messagesMonitor);
                    } catch (Exception exc) {
                        LOG.error().append(exc).commit();
                    }
                }
            } finally {
                messagesMonitor.stop();
            }
        }

        public void runUnchecked(MessagesMonitor messagesMonitor) {
            try (DXTickDB db = wrapper.getDB()) {
                DXTickStream stream;
                do {
                    stream = db.getStream(key);
                } while (stream == null);
                final SelectionOptions selectionOptions = new SelectionOptions(raw, true);
                selectionOptions.allowLateOutOfOrder = true;

                try (TickCursor cursor = stream.select(Long.MIN_VALUE, selectionOptions);
                     IntermittentlyAvailableCursor c = (IntermittentlyAvailableCursor) cursor) {

                    while (!shutdownSignal.isSignaled()){

                        NextResult next;

                        Throwable exception = null;
                        try {
                            next = c.nextIfAvailable();
                        } catch (UnavailableResourceException x) {
                            continue;
                        } catch (CursorException x) {
                            next = NextResult.OK;
                            exception = x;
                        } catch (Throwable x) {
                            LOG.error().append(x).commit();
                            break;
                        }

                        if (exception != null) {
                            LOG.error().append(exception).commit();
                            continue;
                        }

                        if (next == NextResult.OK) {
                            if (consumer != null)
                                consumer.accept(cursor.getMessage());
                            messagesMonitor.count();
                        } else if (next == NextResult.END_OF_CURSOR) {
                            LOG.info().append("END OF CURSOR").commit();
                            break;
                        }
                    }
                }
            }
        }
    }

    public class ReadTask implements Runnable {

        private final String key;
        private final Consumer<InstrumentMessage> consumer;
        private final DBWrapper wrapper;
        private final ShutdownSignal shutdownSignal = new ShutdownSignal();

        ReadTask(String url, String key, Consumer<InstrumentMessage> consumer) {
            this.wrapper = new DBWrapper(url);
            this.key = key;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            while (!shutdownSignal.isSignaled()){
                try {
                    runUnchecked();
                } catch (Exception exc) {
                    LOG.error().append(exc).commit();
                }
            }
        }

        private void runUnchecked() {
            try (DXTickDB db = wrapper.getDB()) {
                DXTickStream stream;
                do {
                    stream = db.getStream(key);
                } while (stream == null);
                final SelectionOptions selectionOptions = new SelectionOptions(false, false);

                try (TickCursor cursor = stream.select(Long.MIN_VALUE, selectionOptions);
                     IntermittentlyAvailableCursor c = (IntermittentlyAvailableCursor) cursor) {

                    while (!shutdownSignal.isSignaled()) {

                        NextResult next;

                        Throwable exception = null;
                        try {
                            next = c.nextIfAvailable();
                        } catch (UnavailableResourceException x) {
                            continue;
                        } catch (CursorException x) {
                            next = NextResult.OK;
                            exception = x;
                        } catch (Throwable x) {
                            LOG.error().append(x).commit();
                            break;
                        }

                        if (exception != null) {
                            LOG.error().append(exception).commit();
                            continue;
                        }

                        if (next == NextResult.OK) {
                            if (consumer != null)
                                consumer.accept(cursor.getMessage());
                        } else if (next == NextResult.END_OF_CURSOR) {
                            LOG.info().append("END OF CURSOR").commit();
                            cursor.reset(Long.MIN_VALUE);
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        new TestReader(args).start();
    }

    public static TestReader create(String dbUrl, String stream, int readers, boolean raw) {
        if (raw) {
            return new TestReader(new String[]{
                    "-db", dbUrl,
                    "-stream", stream,
                    "-readers", Integer.toString(readers),
                    "-raw"
            });
        } else {
            return new TestReader(new String[]{
                    "-db", dbUrl,
                    "-stream", stream,
                    "-readers", Integer.toString(readers)
            });
        }
    }
}
