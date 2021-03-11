package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.pub.ChannelPerformance;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.test.messages.MarketMessage;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.TimeKeeper;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Author Alex
 * Date: May 26, 2010
 */
public class Test_MessageLatency extends TDBTestBase {
    private static final int WARMUP = 20000;
    private static final int TOTAL = 100000;

    private double minLatency = Long.MAX_VALUE;
    private double maxLatency;
    private double avgLatency;
    private long[] result = new long[TOTAL + 1];

    public Test_MessageLatency() {
        super(true);
    }

    public DXTickStream getStream(DXTickDB db, StreamScope scope) {
        DXTickStream stream = db.getStream("message");
        if (stream != null)
            stream.delete();

        RecordClassDescriptor mcd = StreamConfigurationHelper.mkMarketMessageDescriptor(null, false);
        RecordClassDescriptor rcd = StreamConfigurationHelper.mkTradeMessageDescriptor(
                mcd, null, null, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO);

        StreamOptions options = StreamOptions.fixedType(scope, "message", "message", 0, rcd);
        options.bufferOptions = new BufferOptions ();
        options.bufferOptions.initialBufferSize = 1024  << 10;
        options.bufferOptions.maxBufferSize = 1024 << 10;
        options.bufferOptions.lossless = true;
        return db.createStream("message", options);
    }

    @Test
    public void testLocalDurable() throws Throwable  {
        System.out.println("Running with local database (DURABLE stream)... ");
        test(getServerDb(), StreamScope.DURABLE);
    }

    @Test
    public void testLocalTransient() throws Throwable  {
        System.out.println("Running with local database (TRANSIENT stream)... ");
        test(getServerDb(), StreamScope.TRANSIENT);
    }

    @Test
    public void testRemoteDurable() throws Throwable  {
        System.out.println("Running with remote database (DURABLE stream) ... ");

        //test(TickDBFactory.openFromUrl("dxtick://localhost:8011", false), StreamScope.DURABLE);
        test(getTickDb(), StreamScope.DURABLE);
    }

    @Test
    public void testRemoteTransient() throws Throwable  {
        System.out.println("Running with remote database (TRANSIENT stream) ... ");

        //test(TickDBFactory.openFromUrl("dxtick://localhost:8011", false), StreamScope.TRANSIENT);
        test(getTickDb(), StreamScope.TRANSIENT);
    }

    public void test(DXTickDB tickdb, StreamScope scope) throws Throwable  {
        test (tickdb, scope, new File("C:\\TEMP"), 20000, 1);
    }

    public void test(DXTickDB tickdb, StreamScope scope, File folder, int throughput, int runCount)  throws Throwable {

        DXTickStream stream = getStream(tickdb, scope);

        LoadingOptions loadingOptions = new LoadingOptions(false);
        loadingOptions.channelPerformance = ChannelPerformance.LOW_LATENCY;
        TickLoader loader = stream.createLoader(loadingOptions);

        SelectionOptions selectionOptions = new SelectionOptions(false, true);
        selectionOptions.channelPerformance = ChannelPerformance.LOW_LATENCY;
        selectionOptions.allowLateOutOfOrder = true;

        TickCursor cursor = stream.createCursor(selectionOptions);
        cursor.subscribeToAllEntities();
        cursor.subscribeToAllTypes();
        cursor.reset(System.currentTimeMillis());

        Thread.sleep(5000);

        String fileName = "latency." + System.currentTimeMillis() + ".";
        try {

            while (runCount-- > 0) {
                MessageGenerator gn = new MessageGenerator(loader, throughput);
                minLatency = Long.MAX_VALUE;
                maxLatency = 0;
                avgLatency = 0;
                int messageCount = 0;
                try {

                    Thread thread = new Thread(gn);
                    thread.setDaemon(true);
                    thread.start();
                    while (messageCount++ < TOTAL && cursor.next()) {
                        onMessage((MarketMessage) cursor.getMessage(), messageCount);
                    }
                    gn.active = false;
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                System.out.printf ("Max Latency %,.0f ns \n", maxLatency * 1000);
                System.out.printf ("Min Latency %,.0f ns \n", minLatency * 1000);
                System.out.printf ("AVG Latency %,.0f ns \n", avgLatency * 1000);
                System.out.println ("--------------------------------------------");

                StringBuilder sb = new StringBuilder();
                for (long aResult : result) {
                    sb.append(aResult).append("\n");
                }

                FileWriter fw = null;
                try {
                    File out = new File(folder, fileName + runCount + ".csv");
                    fw = new FileWriter (out, true);
                    fw.write("latency");
                    fw.write (sb.toString());
                    fw.close ();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    Util.close (fw);
                }
            }
        } finally {
            Util.close(loader);
            Util.close(cursor);
        }
    }

    public void onMessage(MarketMessage msg, int messageCount) {
        final long now = System.nanoTime();
        long latency = (now - msg.getOriginalTimestamp()) / 1000;
        result[messageCount] = latency;

        if (messageCount > WARMUP)
        {
            minLatency = Math.min(minLatency, latency);
            maxLatency = Math.max(maxLatency, latency);
            result[messageCount] = latency;
            int count = messageCount - WARMUP;
            avgLatency = (avgLatency * (count - 1) + latency) / count;
        }
    }

    private class MessageGenerator implements Runnable {
        private static final long MAX_FEED_RATE_FOR_SPIN_LOCK = 10000;
        public volatile boolean active = true;
        private final TickLoader loader;
        private int throughputMessagesPerSecond;
        private String[] table = new String[32];
        private int[] values = new int[32];

        private MessageGenerator(TickLoader loader, int throughputMessagesPerSecond) {
            this.loader = loader;
            this.throughputMessagesPerSecond = throughputMessagesPerSecond;
            Random rnd = new Random(2012);


            assert (Integer.bitCount(table.length) == 1); // so that index can be (messageCount & (table.length-1))

            for (int i = 0; i < table.length; i++) {
                values[i] = Math.abs(rnd.nextInt()) % 10 + 48;
                table[i] = String.valueOf(values[i]);
            }
        }

        @Override
        public void run() {
            final long intervalBetweenMessagesInNanos;
            if (throughputMessagesPerSecond > MAX_FEED_RATE_FOR_SPIN_LOCK)
                intervalBetweenMessagesInNanos = 0;
            else
                intervalBetweenMessagesInNanos = TimeUnit.SECONDS.toNanos(1) / throughputMessagesPerSecond;


            final TradeMessage msg = new TradeMessage();
            //msg.symbol = "A";
            try {
                final int indexMask = table.length-1;
                long messageCount = 0;
                while (active) {
                    try {
                        long nextNanoTime = (intervalBetweenMessagesInNanos != 0) ? System.nanoTime() + intervalBetweenMessagesInNanos : 0;
                        while (active) {
                            if (intervalBetweenMessagesInNanos != 0) {
                                if (System.nanoTime() < nextNanoTime)
                                    continue; // spin-wait
                            }

                            msg.setSymbol(table[(int) (++messageCount & indexMask)]);
                            msg.setOriginalTimestamp(System.nanoTime());
                            msg.setTimeStampMs(TimeKeeper.currentTime);
                            //msg.timestamp = TimeStampedMessage.TIMESTAMP_UNKNOWN;
                            loader.send (msg);

                            nextNanoTime += intervalBetweenMessagesInNanos;
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }

            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static void main (String ... args) throws Throwable {
        Test_MessageLatency test = new Test_MessageLatency();
        test.startup();

        if (args.length == 0) {
            System.out.println("Usage: <output> <throughput> <cycles> ");
            System.out.println("  <output> is path for the results. default 'C:\\TEMP' ");
            System.out.println("  <throughput> is number of messages per second. default is 10000.");
            System.out.println("  <cycles> is number test cycles. each cycle will produce 1 mln messages.");
        }

        File folder = args.length > 0 ? new File(args[0]) : new File("C:\\TEMP");
        int throughput = args.length > 1 ? Integer.parseInt(args[1]) : 10000;
        int cycles = args.length > 2 ? Integer.parseInt(args[2]) : 50;

        System.out.println(String.format("Running test: output %s, throughput %s, cycles %d", folder, throughput, cycles));

        //DXTickDB tickdb = TickDBFactory.openFromUrl("dxtick://localhost:8011", false);
        DXTickDB tickdb = test.getTickDb();
        test.test(tickdb, StreamScope.DURABLE, folder, throughput, cycles);
        tickdb.close();
    }
}
