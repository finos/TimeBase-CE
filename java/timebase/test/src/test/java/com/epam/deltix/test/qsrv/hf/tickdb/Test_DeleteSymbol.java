package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.anvil.util.ShutdownSignal;
import com.epam.deltix.gflog.Log;
import com.epam.deltix.gflog.LogFactory;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.test.qsrv.hf.tickdb.server.ServerRunner;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.tests.reports.Metrics;
import com.epam.deltix.qsrv.hf.tickdb.tests.reports.TimestampedMetric;
import com.epam.deltix.util.concurrent.IntermittentlyAvailableCursor;
import com.epam.deltix.util.concurrent.NextResult;
import com.epam.deltix.util.concurrent.UnavailableResourceException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Test_DeleteSymbol {
    private static final Log LOG = LogFactory.getLog(Test_DeleteSymbol.class);

    private static final char[] CHARS = "abcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final Random RANDOM = new Random(System.currentTimeMillis());
    private static final String DELETE_SYMBOL = "CCC";
    private static final String KEY = "testStream";
    private static final ArrayList<String> SYMBOLS = getRandomStringsList(100);
    private static final Set<String> SUBSCRIBE_SYMBOLS = getSubscribeSymbols(50);
    private static final ShutdownSignal shutdownSignal = new ShutdownSignal();

    private static TDBRunner runner;

    @BeforeClass
    public static void start() throws Throwable {
        runner = new ServerRunner(true, true);
        runner.startup();
    }

    @AfterClass
    public static void stop() throws Throwable {
        runner.shutdown();
        runner = null;
    }

    @Test
    public void test() {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        executorService.execute(new LoadTask());
        executorService.execute(new ReadTask(null));
        executorService.execute(new ReadTask(null));
        shutdownSignal.await(5, TimeUnit.MINUTES);
    }

    private static class LoadTask implements Runnable {

        @Override
        public void run() {
            try {
                ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
                LOG.info().append("Scheduled symbols delete in 30 seconds.").commit();
                executorService.schedule(this::deleteSymbol, 30, TimeUnit.SECONDS);

                DXTickDB db = runner.getTickDb();
                StreamOptions options = new StreamOptions(StreamScope.DURABLE, KEY, KEY, 0);
                Introspector introspector = Introspector.createEmptyMessageIntrospector();
                RecordClassDescriptor rcd = introspector.introspectRecordClass(BarMessage.class);
                options.setFixedType(rcd);
                DXTickStream stream = db.createStream(KEY, options);
                MessagesMonitor messagesMonitor = new MessagesMonitor(10000, "LOADER");
                try (TickLoader loader = stream.createLoader()) {
                    messagesMonitor.start();
                    BarMessage message = new BarMessage();
                    message.setClose(0.32636);
                    message.setOpen(0.564767);
                    message.setExchangeId(4);
                    Random random = new Random(System.currentTimeMillis());

                    while (!shutdownSignal.isSignaled()) {
                        synchronized (SYMBOLS) {
                            message.setSymbol(SYMBOLS.get(random.nextInt(SYMBOLS.size())));
                            loader.send(message);
                            messagesMonitor.count();
                        }
                    }
                } finally {
                    messagesMonitor.stop();
                }
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }

        private void deleteSymbol() {
            DXTickDB db = runner.getTickDb();
            DXTickStream stream = db.getStream(KEY);
            LOG.info().append("Stop loading symbol ").append(DELETE_SYMBOL).commit();
            synchronized (SYMBOLS) {
                SYMBOLS.remove(DELETE_SYMBOL);
                System.out.println(SYMBOLS);
            }

            IdentityKey identity = getDeleteSymbol(stream);
            LOG.info().append("Truncate symbol ").append(DELETE_SYMBOL).commit();
            stream.truncate(0, identity);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            LOG.info().append("Delete symbol ").append(DELETE_SYMBOL).commit();
            stream.clear(identity);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            LOG.info().append(Arrays.toString(stream.listEntities())).commit();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    private static class ReadTask implements Runnable {

        private final Consumer<InstrumentMessage> consumer;

        private ReadTask(Consumer<InstrumentMessage> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void run() {
            DXTickDB db = runner.getTickDb();
            SelectionOptions selectionOptions = new SelectionOptions(true, true);
            selectionOptions.setAllowLateOutOfOrder(true);
            DXTickStream stream;
            do {
                stream = db.getStream(KEY);
            } while (stream == null);
            IdentityKey[] ids;
            do {
                ids = stream.listEntities();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (ids.length < SYMBOLS.size());

            MessagesMonitor messagesMonitor = new MessagesMonitor(10000, "READER");
            try (TickCursor cursor = db.select(Long.MIN_VALUE, selectionOptions, null, getSubscribeSymbols(stream), stream);
                 IntermittentlyAvailableCursor c = (IntermittentlyAvailableCursor) cursor) {
                messagesMonitor.start();
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
            } finally {
                messagesMonitor.stop();
            }
        }
    }

    private static String[] getSubscribeSymbols(DXTickStream stream) {
        return Arrays.stream(stream.listEntities())
                .filter(id -> SUBSCRIBE_SYMBOLS.contains(id.getSymbol().toString())).map(IdentityKey::getSymbol)
                .toArray(String[]::new);
    }

    private static IdentityKey getDeleteSymbol(DXTickStream stream) {
        return Arrays.stream(stream.listEntities())
                .filter(id -> id.getSymbol().toString().equals(DELETE_SYMBOL))
                .findAny().orElseThrow(RuntimeException::new);
    }

    public static ArrayList<String> getRandomStringsList(int n) {
        HashSet<String> strings = new HashSet<>(n);
        while (strings.size() < n - 1) {
            strings.add(getRandomString(8));
        }
        strings.add(DELETE_SYMBOL);
        return new ArrayList<>(strings);
    }

    public static String getRandomString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(CHARS[RANDOM.nextInt(CHARS.length)]);
        }
        return sb.toString();
    }

    public static Set<String> getSubscribeSymbols(int n) {
        Set<String> result = SYMBOLS.stream()
                .filter(s -> !s.equals(DELETE_SYMBOL))
                .limit(n - 1)
                .collect(Collectors.toSet());
        result.add(DELETE_SYMBOL);
        return result;
    }


    public static class MessagesMonitor {

        private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        private final AtomicLong count = new AtomicLong();
        private final long interval;
        private final Runnable task;
        private final TimestampedMetric<Double> metric = Metrics.createDoubleTimestamped();
        private Future<?> scheduled = null;

        public MessagesMonitor(long interval, String id) {
            this.interval = interval;
            this.task = () -> {
                long messages = count.get();
                count.set(0);
                double value = 1000. * messages / interval;
                metric.addValue(System.currentTimeMillis(), value);
                LOG.info().append(id).append(": ")
                        .append(value)
                        .append(" msg/sec.")
                        .commit();
            };
        }

        public TimestampedMetric<Double> getMetric() {
            return metric;
        }

        public synchronized void start() {
            if (scheduled != null) {
                stop();
            }
            scheduled = executor.scheduleAtFixedRate(task, interval, interval, TimeUnit.MILLISECONDS);
        }

        public synchronized void stop() {
            if (scheduled != null) {
                scheduled.cancel(true);
                scheduled = null;
            }
        }

        public void count() {
            count.incrementAndGet();
        }

    }

}
