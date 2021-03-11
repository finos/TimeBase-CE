package com.epam.deltix.test.qsrv.hf.tickdb.perfomancedrop;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.data.stream.MessageSourceMultiplexer;
import com.epam.deltix.test.qsrv.hf.tickdb.perfomancedrop.stubs.StubMessageSource;
import com.epam.deltix.test.qsrv.hf.tickdb.perfomancedrop.stubs.TickCursorStub1;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.impl.DebugFlags;
import com.epam.deltix.qsrv.hf.tickdb.impl.TransientStreamImpl;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.TypedMessageSource;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * See {@link deltix.qsrv.hf.tickdb.perfomancedrop} for problem description.
 *
 * <p>Tests that was designed to minimize code footprint of the problem to help to localize it.
 * However at some point the problem stated to disappear with removal of any code.
 * So this test doesn't reproduces the problem anymore.
 *
 * <p>This test fails if performance drop is detected.
 */
// TODO: This test actually does not depend on external data
public class Test_ThroughputDropAfterLiveMessage_Micro2 extends Test_ThroughputDropAfterLiveMessage_Base {
    private static final int NUM_READERS = 2;

    private static final AtomicBoolean stopped = new AtomicBoolean(false);

    public static final int NUMBER_OF_SECONDARY_MESSAGES = 5; // With values less than 4 the problem is often not reproduced
    public static final int MEASUREMENT_DURATION_MS = 1_000;
    public static final int MEASUREMENT_ITERATIONS = 5;
    public static final int INTERVAL_BETWEEN_SECONDARY_MESSAGES_MS = 1000;



    private static RecordClassDescriptor tradeDescriptor;

    public DataCacheOptions     options = new DataCacheOptions();

    public Test_ThroughputDropAfterLiveMessage_Micro2() throws FileNotFoundException {
    }


    @Test
    public void test() throws Exception {
        //Thread.sleep(30000);
        //Thread.sleep(5000);
        printTimed("Test start");

        // Setup

        printTimed("DB setup end");

        //final DXTickStream mainStream = createMainStream(db);
        //final DXTickStream mainStream = null;
        final DXTickStream secondaryStream = createSecondStream();


        //TickCursor mainCursor = mainStream.select(Long.MIN_VALUE, new SelectionOptions(true, false), null, null);

        TickCursorStub1 secondaryCursor = createSecondaryCursor(secondaryStream);
        TickLoader secondarySender = secondaryStream.createLoader(new LoadingOptions(true));

        //Thread.sleep(30000);

        // Start readers
        List<MainStreamReader> readers = new ArrayList<>();
        createAndStartMainReaders(readers);

        // Warmup
        //Thread.sleep(3000);

        printTimed("Starting pre-live measurements");

        // Measure initial performance
        List<Integer> measurementsBefore = takeMeasurements(MEASUREMENT_ITERATIONS, MEASUREMENT_DURATION_MS, readers);
        Integer meanBefore = getMean(measurementsBefore);
        Assert.assertNotEquals("Measurement must be non-zero", 0, meanBefore.intValue());
        printTimed("meanBefore: " + df.format(meanBefore));

        printTimed("Reading/sending live messages");
        // Start live message sender
        Thread liveSenderThread = new SecondaryMessageSender(secondarySender);
        liveSenderThread.start();
        //sendMessages(NUMBER_OF_LIVE_MESSAGES, liveSender);

        // Receive live messages
        for (int i = 0; i < NUMBER_OF_SECONDARY_MESSAGES; i++) {
            secondaryCursor.next();
        }
        // Stop live sending thread
        liveSenderThread.interrupt();
        liveSenderThread.join();
        printTimed("Finished secondary");

        // Wait before second measurement
        Thread.sleep(1000);

        printTimed("Starting post-live measurements");
        // Measure after live messages
        List<Integer> measurementsAfter = takeMeasurements(MEASUREMENT_ITERATIONS, MEASUREMENT_DURATION_MS, readers);
        Integer meanAfter = getMean(measurementsAfter);
        printTimed("meanAfter: " + df.format(meanAfter));


        float performanceLossPercent = getLossPercent(meanBefore, meanAfter);
        if (performanceLossPercent > 5) {
            printTimed("Performance LOSS: " + performanceLossPercent + "%");
            Assert.fail("Performance LOSS: " + performanceLossPercent + "%");
        } else {
            printTimed("NO significant performance loss: " + performanceLossPercent + "%");
        }
    }

    private TickCursorStub1 createSecondaryCursor(DXTickStream secondaryStream) {
        SelectionOptions options = new SelectionOptions(true, true);
        //TickCursorImpl cursor = new TickCursorImpl(null, options);
        TickCursorStub1 cursor = new TickCursorStub1(options.live);
        long time = Long.MIN_VALUE;
        cursor.reset (time);

        // cursor by default subscribed to all types, so change if types != null
        cursor.subscribeToAllEntities();

        // apply whole subscription
        //cursor.addStream(stream);
        //cursor.mx.add(new StubMessageSource<>());
        TransientStreamImpl transientStream = (TransientStreamImpl) secondaryStream;
        InstrumentMessageSource source = transientStream.createSource(time, options);
        //cursor.addStream(secondaryStream);
        cursor.mx.add(source, time);

        return cursor;
        //return secondaryStream.select(Long.MIN_VALUE, new SelectionOptions(true, LIVE_READER), null, null);
    }

    private void createAndStartMainReaders(List<MainStreamReader> readers) {
        for (int i = 0; i < NUM_READERS; i++) {
            MainStreamReader reader = USE_MSM_READER ? new MainStreamReaderMSM() : new MainStreamReaderTCI();
            mainExecutorService.submit(reader);
            readers.add(reader);
        }
    }



    private DXTickStream createSecondStream() {
        /*DXTickStream liveTrans = db.getStream(TRANSIENT_KEY);
        if (liveTrans != null) {
            print(TRANSIENT_KEY + " stream exists. Removing...");
            liveTrans.delete();
            print("OK");
        }*/

        RecordClassDescriptor mmDescriptor = StreamConfigurationHelper.mkMarketMessageDescriptor(null, false);
        tradeDescriptor = StreamConfigurationHelper.mkTradeMessageDescriptor(
                mmDescriptor, null, null, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);

        StreamOptions options = StreamOptions.fixedType(StreamScope.RUNTIME, LocalTestBaseConfig.TRANSIENT_KEY, LocalTestBaseConfig.TRANSIENT_KEY, 0, tradeDescriptor);
        options.bufferOptions = new BufferOptions();
        options.bufferOptions.initialBufferSize = 1024 << 10;
        options.bufferOptions.maxBufferSize = 1024 << 10;
        options.bufferOptions.lossless = true;
        //return db.createStream(options.name, options);

        TransientStreamImpl stream = new TransientStreamImpl (null, LocalTestBaseConfig.TRANSIENT_KEY, options);
        stream.open(false);
        return stream;
    }

    private static void sendMessages(int messagesToSend, TickLoader liveSender) throws InterruptedException {
        RawMessage msg = createRawMessage();

        int sentCount = 0;
        while (sentCount < messagesToSend) {
            //Thread.sleep(INTERVAL_BETWEEN_SECONDARY_MESSAGES_MS);

            msg.setNanoTime(System.nanoTime());
            //System.out.println(getUptimeString() +": Sent live message");
            liveSender.send(msg);
            sentCount ++;
        }
    }

    // TODO: do not repeat same function
    private static RawMessage createRawMessage() {
        RawMessage msg = new RawMessage();
        msg.type = tradeDescriptor;
        msg.data = new byte[10];
        msg.setSymbol("AAA");
        return msg;
    }

    private final ExecutorService mainExecutorService = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("MainReader-%d").build()
    );



    /**
     * @return sorted list of measurements
     */
    private List<Integer> takeMeasurements(int iterations, int durationMs, List<MainStreamReader> readers) {
        List<Integer> measurements = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            Integer measurementResult = makeMeasurement(durationMs, readers);
            measurements.add(measurementResult);
            print("Measurement: " + df.format(measurementResult));
        }
        return measurements;
    }

    private Integer makeMeasurement(int durationMs, List<MainStreamReader> readers) {
        // Note: taking results takes some time itself. For now we ignore that.
        long countBefore = 0;
        for (MainStreamReader reader : readers) {
            countBefore += reader.getTotalMessages();
        }

        long startTime = System.currentTimeMillis();
        try {
            // This time is not precise
            Thread.sleep(durationMs);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        long countAfter = 0;
        for (MainStreamReader reader : readers) {
            countAfter += reader.getTotalMessages();
        }
        long stopTime = System.currentTimeMillis();

        long totalTime = stopTime - startTime;

        return Math.round(((float) countAfter - countBefore) * 1000 / totalTime);
    }



    interface MainStreamReader extends Runnable {
        long getTotalMessages();
    }

    static private class MainStreamReaderTCI implements Runnable, MainStreamReader {
        private volatile long count = 0;
        //private volatile long prevCount = 0;
        //private volatile long startTime;
        //private volatile long finishTime = Long.MIN_VALUE;

        MainStreamReaderTCI() {
        }

        @Override
        public void run() {
            //startTime = System.nanoTime();

            TickCursorStub1  cursor = createMainCursor();
            //TickCursor cursor = stream.select(Long.MIN_VALUE, new SelectionOptions(true, false), null, null);
            try {
                while (cursor.next()) {
                    count++;
                    InstrumentMessage message = cursor.getMessage();
                    // This code is needed just to simulate real message access. So JIT will not optimize to message skipping.
                    if (message.getNanoTime() < 0) {
                        throw new IllegalStateException();
                    }

                    if (stopped.get())
                        break;
                }
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                cursor.close();
                //finishTime = System.nanoTime();
            }
        }

        private TickCursorStub1 createMainCursor() {
            TickCursorStub1 cursor = new TickCursorStub1(false);
            //TickCursorStub1 cursor = new TickCursorImpl(null, new SelectionOptions(true, false));
            cursor.reset (Long.MIN_VALUE);

            // cursor by default subscribed to all types, so change if types != null
            cursor.subscribeToAllEntities();

            // apply whole subscription
            //cursor.addStream(stream);
            cursor.mx.add(new StubMessageSource<>());

            return (cursor);
        }
/*
        public double calcThroughput() {
            long endTime = finishTime == Long.MIN_VALUE ? System.nanoTime() : finishTime;
            double timePass = ((double) (endTime - startTime)) / 1000000000.0;
            long count = this.count;
            double throughput = ((double) (count - prevCount)) / timePass;
            this.prevCount = count;
            startTime = System.nanoTime();
            return throughput;
        }*/

        public long getTotalMessages() {
            return count;
        }
    }

    public static final boolean USE_MSM_READER = false;

    static private class MainStreamReaderMSM implements Runnable, MainStreamReader {
        private volatile long count = 0;

        private Set<String> subscribedTypeNames = null;
        private boolean isSubscribedToAllEntities = true;
        private boolean realTimeNotifications = false;
        private boolean inRealtime = false;

        private InstrumentMessage                   currentMessage;
        private RecordClassDescriptor               currentType;
        private TickStream                          currentStream;
        private TypedMessageSource currentSource = null;

        MainStreamReaderMSM() {
        }

        public boolean              next (MessageSourceMultiplexer<InstrumentMessage> mx) {
            boolean                 ret;
            RuntimeException        x = null;
            Runnable                lnr;
            //nextWatch.start();
            synchronized (mx) {

                for (;;) {
                    //x1Watch.start();
                    try {
                        boolean hasNext;

                        try {
                            hasNext = mx.syncNext();
                        } catch (RuntimeException xx) {
                            x = xx;
                            ret = false;    // make compiler happy
                            break;
                        }

                        if (!hasNext) {
                            ret = false;
                            break;
                        }
                    } finally {
                        //x1Watch.stop();
                    }


                    //x2Watch.start();
                    currentMessage = mx.syncGetMessage ();
                    //x2Watch.stop();

                    //x3Watch.start();
                    try {
                        // current message is indicator of real-time mode
                        if (realTimeNotifications && mx.isRealTimeStarted()) {
                            currentType = StreamConfigurationHelper.REAL_TIME_START_MESSAGE_DESCRIPTOR;
                            ret = inRealtime = true;
                            break;
                        }

                        if (!isSubscribedToAllEntities /*&& !isSubscribed(currentMessage)*/) {
                            if (DebugFlags.DEBUG_MSG_DISCARD) {
                                DebugFlags.discard(
                                        "TB DEBUG: Discarding message " +
                                                currentMessage + " because we are not subscribed to its entity"
                                );
                            }

                            /*if (closed)
                                throw new CursorIsClosedException();*/

                            continue;
                        }
                    } finally {
                        //x3Watch.stop();
                    }


                    //x4Watch.start();
                    //x4a1Watch.start();

                    final TypedMessageSource  source =
                            (TypedMessageSource) mx.syncGetCurrentSource ();

                    //x4a1Watch.stop();

                    //x4a2Watch.start();

                    //x4a2Watch.start();
                    currentStream = null;//((TickStreamRelated) source).getStream ();
                    currentSource = source;
                    //x4a2Watch.stop();

                    //x4a3Watch.start();
                    if (currentMessage.getClass () == RawMessage.class)
                        currentType = ((RawMessage) currentMessage).type;
                    else
                        currentType = source.getCurrentType ();
                    //x4a3Watch.stop();

                    //x4Watch.stop();

                    //x5Watch.start();
                    try {
                        if (subscribedTypeNames != null &&
                                !subscribedTypeNames.contains(currentType.getName())) {
                            if (DebugFlags.DEBUG_MSG_DISCARD) {
                                DebugFlags.discard(
                                        "TB DEBUG: Discarding message " +
                                                currentMessage + " because we are not subscribed to its type"
                                );
                            }

                            /*if (closed)
                                throw new CursorIsClosedException();*/

                            continue;
                        }
                    } finally {
                        //x5Watch.stop();
                    }
                    //x6Watch.start();
                    //stats.register (currentMessage);

                    ret = true;
                    //x6Watch.stop();
                    break;
                }
                //
                //  Surprisingly, even mx.next () can call the av lnr (on truncation)
                //
                //lnr = lnrTriggered ();
            }


            //x7Watch.start();
            /*if (lnr != null)
                lnr.run ();*/
            //x7Watch.stop();

            //nextWatch.stop();

            if (x != null)
                throw x;

            return (ret);
        }

        @Override
        public void run() {
            //startTime = System.nanoTime();
            boolean                 ret;

            //TickCursor  cursor = createCursor(stream.getDB(),stream);
            //MessageSourceMultiplexer<InstrumentMessage> mx = new TickCursorMultiplexerStub(new SelectionOptions(true, false));
            MessageSourceMultiplexer<InstrumentMessage> mx = new MessageSourceMultiplexer<>(true, false);
            mx.add(new StubMessageSource<>());
            //TickCursor cursor = stream.select(Long.MIN_VALUE, new SelectionOptions(true, false), null, null);
            try {
                while (next(mx)) {
                    // This code is needed just to simulate real message access. So JIT will not optimize to message skipping.
                    if (currentMessage.getNanoTime() < 0) {
                        throw new IllegalStateException();
                    }
                    count++;
                }
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                mx.close();
                //finishTime = System.nanoTime();
            }
        }

        public void runOld() {
            //startTime = System.nanoTime();
            boolean                 ret;

            //TickCursor  cursor = createCursor(stream.getDB(),stream);
            MessageSourceMultiplexer<InstrumentMessage> mx =  null;//new TickCursorImpl.CursorMultiplexer(new SelectionOptions(true, false));
            mx.add(new StubMessageSource<>());
            //TickCursor cursor = stream.select(Long.MIN_VALUE, new SelectionOptions(true, false), null, null);
            try {


                while (mx.syncNext()) {
                    count++;
                    InstrumentMessage currentMessage = mx.syncGetMessage ();


                    if (realTimeNotifications && mx.isRealTimeStarted()) {
                        currentType = StreamConfigurationHelper.REAL_TIME_START_MESSAGE_DESCRIPTOR;
                        ret = inRealtime = true;
                        break;
                    }

                    if (!isSubscribedToAllEntities /*&& !isSubscribed(currentMessage)*/) {
                        if (DebugFlags.DEBUG_MSG_DISCARD) {
                            DebugFlags.discard(
                                    "TB DEBUG: Discarding message " +
                                            currentMessage + " because we are not subscribed to its entity"
                            );
                        }
/*
                        if (closed)
                            throw new CursorIsClosedException();*/

                        continue;
                    }



                    TypedMessageSource source;
                    MessageSource<InstrumentMessage> messageSource = mx.syncGetCurrentSource();
                    /*if (messageSource instanceof PDStreamReader) {
                        source = (PDStreamReader) messageSource;
                    } else {

                    }*/
                    source = (TypedMessageSource) messageSource;

                    if (currentMessage.getClass () == RawMessage.class)
                        currentType = ((RawMessage) currentMessage).type;
                    else
                        currentType = source.getCurrentType ();

                    if (subscribedTypeNames != null &&
                            !subscribedTypeNames.contains(currentType.getName())) {
                        if (DebugFlags.DEBUG_MSG_DISCARD) {
                            DebugFlags.discard(
                                    "TB DEBUG: Discarding message " +
                                            currentMessage + " because we are not subscribed to its type"
                            );
                        }

                        continue;
                    }






                    if (source == null) {
                        throw new IllegalStateException();
                    }

                    //InstrumentMessage message = msm.getMessage();
                    // This code is needed just to simulate real message access. So JIT will not optimize to message skipping.
                    if (currentMessage.getNanoTime() < 0) {
                        throw new IllegalStateException();
                    }

                    if (stopped.get())
                        break;
                }
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                mx.close();
                //finishTime = System.nanoTime();
            }
        }

/*
        public double calcThroughput() {
            long endTime = finishTime == Long.MIN_VALUE ? System.nanoTime() : finishTime;
            double timePass = ((double) (endTime - startTime)) / 1000000000.0;
            long count = this.count;
            double throughput = ((double) (count - prevCount)) / timePass;
            this.prevCount = count;
            startTime = System.nanoTime();
            return throughput;
        }*/

        public long getTotalMessages() {
            return count;
        }
    }

    private static class SecondaryMessageSender extends Thread {
        private final TickLoader liveSender;

        public SecondaryMessageSender(TickLoader liveSender) {
            this.liveSender = liveSender;
        }

        @Override
        public void run() {
            try {
                //Thread.sleep(100);
                sendMessages(NUMBER_OF_SECONDARY_MESSAGES, liveSender);
            } catch (InterruptedException e) {
            }
        }
    }
}
