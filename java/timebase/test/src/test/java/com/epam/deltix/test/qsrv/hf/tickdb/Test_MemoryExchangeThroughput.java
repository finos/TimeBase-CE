package com.epam.deltix.test.qsrv.hf.tickdb;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.TimeStampedMessage;;
import com.epam.deltix.qsrv.SetHome;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import com.epam.deltix.util.collections.CircularBoundedDoubleStateQueue;
import com.epam.deltix.util.collections.FixedSizeStack;
//import org.f1x.io.disruptor.*;
import org.junit.BeforeClass;
import org.junit.Test;
import sun.misc.Unsafe;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.Assert.fail;

/**
 *         Date: 10/18/13
 */
@SuppressWarnings("unsafe")
public class Test_MemoryExchangeThroughput extends TDBTestBase {
    private static final String MESSAGE = "8=FIX.4.4\u00019=83\u000135=5\u000134=1\u000149=XXXXXXXXXX\u000150=XXXXXXXXXX\u000152=20131013-21:10:01.513\u000156=CITIFX\u000157=CITIFX\u000110=056\u0001";
    private static final byte [] MESSAGE_BYTES = getBytes(MESSAGE);
    private static final int MESSAGE_SIZE = MESSAGE_BYTES.length;
    private static final int QUEUE_SIZE = 16*1024;

    private final Executor executor = Executors.newCachedThreadPool();

    public Test_MemoryExchangeThroughput() {
        super(true); // local mode
    }

    @BeforeClass
    public static void setHome() {
        SetHome.check();
    }

    @Test
    public void testArrayBlockingQueue() throws InterruptedException {
        final ArrayBlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(QUEUE_SIZE);
        final FixedSizeStack<byte[]> objectPool = new FixedSizeStack<>(2*QUEUE_SIZE);
        for (int i=0; i < objectPool.capacity(); i++) {
            objectPool.add(new byte[MESSAGE_SIZE]);
        }
        //Start consumer
        class QueueConsumer implements Runnable, MessageCounter {

            volatile long messageCounter;

            @Override
            public long getMessageCount() {
                return messageCounter;
            }

            @Override
            public void run() {
                while (true) {
                    try {
                        byte [] event = queue.take();
                        processPayload(event, 0);
                        synchronized (objectPool) {
                            objectPool.add(event);
                        }
                        messageCounter++;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        QueueConsumer queueConsumer = new QueueConsumer();
        executor.execute(queueConsumer);


        // start producer
        executor.execute(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        byte [] event;
                        synchronized (objectPool) {
                            event = objectPool.remove();
                        }
                        System.arraycopy(MESSAGE_BYTES, 0, event, 0, MESSAGE_SIZE);
                        queue.put(event);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        measureThroughput (queueConsumer, "ArrayBlockingQueue");
    }

    @Test
    public void testFortHillQueue() throws InterruptedException {
        final CircularBoundedDoubleStateQueue<byte[]> queue = new CircularBoundedDoubleStateQueue<byte[]>(QUEUE_SIZE) {
            @Override
            public byte[] newEmptyElement() {
                return new byte[MESSAGE_SIZE];
            }
        };

        //Start consumer
        class QueueConsumer implements Runnable, MessageCounter {

            volatile long messageCounter;

            @Override
            public long getMessageCount() {
                return messageCounter;
            }

            @Override
            public void run() {
                while (true) {
                    try {
                        byte [] event = queue.getReadyElement();
                        processPayload(event, 0);
                        queue.addEmptyElement(event);
                        messageCounter++;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        QueueConsumer queueConsumer = new QueueConsumer();
        executor.execute(queueConsumer);


        // start producer
        executor.execute(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        byte [] event = queue.getEmptyElement();
                        System.arraycopy(MESSAGE_BYTES, 0, event, 0, MESSAGE_SIZE);
                        queue.addReadyElement(event);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        measureThroughput (queueConsumer, "CircularDoubleStateQueue");
    }

    @Test
    public void testStandardDisruptor() throws InterruptedException {
        EventFactory<byte[]> eventFactory = new EventFactory<byte[]>() {
            @Override
            public byte[] newInstance() {
                return new byte[MESSAGE_SIZE];
            }
        };

        Disruptor<byte[]> disruptor = new Disruptor<>(eventFactory, QUEUE_SIZE, executor, ProducerType.SINGLE, new BusySpinWaitStrategy());
        disruptor.handleExceptionsWith(new TestExceptionHandler());

        //Start consumer
        CountingEventHandler handler = new CountingEventHandler();
        disruptor.handleEventsWith(handler);
        final RingBuffer<byte[]> ringBuffer = disruptor.start();

        //Start producer
        executor.execute(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    long sequence = ringBuffer.next();
                    byte [] event = ringBuffer.get(sequence);
                    System.arraycopy(MESSAGE_BYTES, 0, event, 0, MESSAGE_SIZE);
                    ringBuffer.publish(sequence);
                }
            }
        });

        measureThroughput (handler, "Disruptor Producer-Consumer");
    }

    private static void processPayload(byte[] event, int offset) {
        if (event == null || event[offset] == 0) {
            System.err.println("Never!");
            System.exit(-1);
        }
    }

    @Test
    public void testAnonymousStream () throws InterruptedException {
        StreamOptions options = new StreamOptions();
        options.name = "test";
        options.scope = StreamScope.RUNTIME;
        options.bufferOptions = new BufferOptions();
        options.bufferOptions.initialBufferSize = options.bufferOptions.maxBufferSize = QUEUE_SIZE*1024;
        options.bufferOptions.lossless = true;
        options.setFixedType(StreamConfigurationHelper.BINARY_MESSAGE_DESCRIPTOR);
        DXTickStream stream = getTickDb().createAnonymousStream(options);


        LoadingOptions lo = new LoadingOptions(true);
        final TickLoader loader = stream.createLoader (lo);

        SelectionOptions so = new SelectionOptions(true, true);
        final TickCursor cursor = stream.createCursor (so);
        cursor.addEntity(new ConstantIdentityKey("MSFT"));

        Runnable producer = new Runnable() {
            @Override
            public void run() {
                final InstrumentMessage msg = createRawMessage("MSFT");
                while(true) {
                    loader.send (msg); // loader will do memcopy() of the message payload
                }
            }
        };

        class MessageConsumer implements Runnable, MessageCounter {
            volatile long messageCounter;

            @Override
            public void run() {
                while (true) {
                    cursor.next();
                    RawMessage received = (RawMessage) cursor.getMessage();
                    processPayload(received.data, 0);
                    messageCounter++;
                }
            }

            @Override
            public long getMessageCount() {
                return messageCounter;
            }
        };

        MessageConsumer consumer = new MessageConsumer();

        executor.execute(consumer);
        executor.execute(producer);

        measureThroughput(consumer, "Anonymous Stream");
    }

    @Test
    public void testDurableStream () throws InterruptedException {
        StreamOptions options = new StreamOptions();
        options.name = "test";
        options.scope = StreamScope.DURABLE;
        options.bufferOptions = new BufferOptions();
        options.bufferOptions.initialBufferSize = options.bufferOptions.maxBufferSize = QUEUE_SIZE*1024;
        options.bufferOptions.lossless = true;
        options.setFixedType(StreamConfigurationHelper.BINARY_MESSAGE_DESCRIPTOR);
        DXTickStream stream = getTickDb().createStream("test", options);


        LoadingOptions lo = new LoadingOptions(true);
        final TickLoader loader = stream.createLoader (lo);

        SelectionOptions so = new SelectionOptions(true, true);
        final TickCursor cursor = stream.createCursor (so);
        cursor.addEntity(new ConstantIdentityKey("MSFT"));

        Runnable producer = new Runnable() {
            @Override
            public void run() {
                final InstrumentMessage msg = createRawMessage("MSFT");
                while(true) {
                    loader.send (msg); // loader will do memcopy() of the message payload
                }
            }
        };

        class MessageConsumer implements Runnable, MessageCounter {
            volatile long messageCounter;

            @Override
            public void run() {
                while (true) {
                    cursor.next();
                    RawMessage received = (RawMessage) cursor.getMessage();
                    processPayload(received.data, 0);
                    messageCounter++;
                }
            }

            @Override
            public long getMessageCount() {
                return messageCounter;
            }
        };

        MessageConsumer consumer = new MessageConsumer();

        executor.execute(consumer);
        executor.execute(producer);

        measureThroughput(consumer, "Durable Stream");
    }


//    @Test
//    public void testByteRing() throws InterruptedException {
//        ByteRing ring = new ByteRing (QUEUE_SIZE*1024, new BusySpinWaitStrategy());
//
//        CountingMessageProcessor handler = new CountingMessageProcessor();
//        MessageProcessorPool processorPool = new MessageProcessorPool (ring, ring.newBarrier(), new TestExceptionHandler(), handler);
//        ring.addGatingSequences(processorPool.getWorkerSequences());
//        processorPool.start(executor);
//
//        executor.execute(new PlaybackMessageProducer(ring));
//
//        measureThroughput (handler, "ByteRing");
//
//    }

//    static class PlaybackMessageProducer implements Runnable, RingBufferBlockProcessor {
//        private final ByteRing ring;
//        private int current;
//        private final byte [] source;
//
//        PlaybackMessageProducer(ByteRing ring) {
//            this.ring = ring;
//            source = new byte [2*MESSAGE_SIZE];
//            System.arraycopy(MESSAGE_BYTES, 0, source, 0, MESSAGE_SIZE);
//            System.arraycopy(MESSAGE_BYTES, 0, source, MESSAGE_SIZE, MESSAGE_SIZE);
//        }
//
//        private static final int SIZE_OF_INT32 = 4;
//
//        @Override
//        public void run() {
//            while (true) {
//                final long high = ring.next(MESSAGE_SIZE + SIZE_OF_INT32);
//                final long low = high - MESSAGE_SIZE - SIZE_OF_INT32 + 1;
//
//                ring.writeInt(low, MESSAGE_SIZE);
//                ring.write(low, source, current, MESSAGE_SIZE); //TODO:                ring.processBlock(low+SIZE_OF_INT32, MESSAGE_SIZE, this);
//
//                ring.publish(high);
//            }
//        }
//
//        @Override
//        public int process(byte[] buffer, int offset, int length, int ringBufferSize) {
//            if (offset + length <= ringBufferSize) {
//                return write(buffer, offset, length);
//            } else {
//                int wrappedSize = offset + length - ringBufferSize;
//                assert wrappedSize > 0;
//                assert wrappedSize < length;
//                final int numberOfBytesToWrite = length - wrappedSize;
//                int result = write(buffer, offset, numberOfBytesToWrite);
//                if (result == numberOfBytesToWrite)
//                    result += write(buffer, 0, wrappedSize);
//                return result;
//            }
//        }
//
//        @Override
//        public void close() {
//
//        }
//
//        public int write(byte[] buffer, int offset, int length) {
//            assert current >= 0;
//            assert current < source.length;
//            assert current + length < source.length;
//
//            System.arraycopy(source, current, buffer, offset, length);
//
//            current = (current + length) % (source.length/2);
//            return length;
//        }
//    }


    public static InstrumentMessage createRawMessage(String symbol) {
        RawMessage msg = new RawMessage(StreamConfigurationHelper.BINARY_MESSAGE_DESCRIPTOR);
        msg.setSymbol(symbol);
        msg.setTimeStampMs(TimeStampedMessage.TIMESTAMP_UNKNOWN);
        msg.data = MESSAGE_BYTES;
        msg.offset = 0;
        msg.length = msg.data.length;
        Arrays.fill(msg.data, (byte) 1);
        return msg;
    }

    public static void measureThroughput (MessageCounter messageCounter, String testName) throws InterruptedException {
        long lastSeenMessageCount = 0;
        long lastTime = System.currentTimeMillis();
        for (int i=0; i < 10; i++) {
            Thread.sleep(15000);

            final long messageCount = messageCounter.getMessageCount();
            final long now = System.currentTimeMillis();
            long throughput = (messageCount - lastSeenMessageCount) / ((now - lastTime)/1000);
            System.out.println("Test " + testName + " processed " + throughput + " messages/sec");
            lastSeenMessageCount = messageCount;
            lastTime = now;
        }
    }

    public interface MessageCounter {
        long getMessageCount();
    }

    private static class CountingEventHandler implements EventHandler<byte[]>, MessageCounter {
        volatile long messageCount;
        @Override
        public void onEvent(byte[] event, long sequence, boolean endOfBatch) throws Exception {
            processPayload(event, 0);
            messageCount ++;
        }

        @Override
        public long getMessageCount() {
            return messageCount;
        }
    }
//
//    private static class CountingMessageProcessor implements RingBufferBlockProcessor, MessageCounter {
//        private volatile long messageCount;
//
//        @Override
//        public int process(byte[] buffer, int offset, int length, int bufferSize) {
//            processPayload(buffer, offset);
//            messageCount++;
//            return length;
//        }
//
//        @Override
//        public void close() {
//        }
//
//        @Override
//        public long getMessageCount() {
//            return messageCount;
//        }
//    }

    private static class TestExceptionHandler implements ExceptionHandler {

        @Override
        public void handleEventException(Throwable ex, long sequence, Object event) {
            ex.printStackTrace();
            fail (ex.getMessage());
        }

        @Override
        public void handleOnStartException(Throwable ex) {
            ex.printStackTrace();
            fail (ex.getMessage());
        }

        @Override
        public void handleOnShutdownException(Throwable ex) {
            ex.printStackTrace();
            fail (ex.getMessage());
        }
    }

    private static byte [] getBytes(String asciiText) {
        try {
            return asciiText.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Expecting ASCII string", e);
        }
    }

    // Test from http://stackoverflow.com/questions/7969665/ways-to-improve-performance-consistency

    @Test
    public void runPeterLawrey() throws IOException, InterruptedException {
        for (int i = 0; i < 10; i++)
            doTest();
    }

    public static void doTest() throws InterruptedException {
        final ByteBuffer writeBuffer = ByteBuffer.allocateDirect(64 * 1024);
        final ByteBuffer readBuffer = writeBuffer.slice();
        final AtomicInteger readCount = new PaddedAtomicInteger();
        final AtomicInteger writeCount = new PaddedAtomicInteger();

        for(int i=0;i<3;i++)
            performTiming(writeBuffer, readBuffer, readCount, writeCount);
        System.out.println();
    }

    private static void performTiming(ByteBuffer writeBuffer, final ByteBuffer readBuffer, final AtomicInteger readCount, final AtomicInteger writeCount) throws InterruptedException {
        writeBuffer.clear();
        readBuffer.clear();
        readCount.set(0);
        writeCount.set(0);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] bytes = new byte[128];
                while (!Thread.interrupted()) {
                    int rc = readCount.get(), toRead;
                    while ((toRead = writeCount.get() - rc) <= 0) ;
                    for (int i = 0; i < toRead; i++) {
                        byte len = readBuffer.get();
                        if (len == -1) {
                            // rewind.
                            readBuffer.clear();
//                            rc++;
                        } else {
                            int num = readBuffer.getInt();
                            if (num != rc)
                                throw new AssertionError("Expected " + rc + " but got " + num) ;
                            rc++;
                            readBuffer.get(bytes, 0, len - 4);
                        }
                    }
                    readCount.lazySet(rc);
                }
            }
        });
        t.setDaemon(true);
        t.start();
        Thread.yield();
        long start = System.nanoTime();
        int runs = 30 * 1000 * 1000;
        int len = 32;
        byte[] bytes = new byte[len - 4];
        int wc = writeCount.get();
        for (int i = 0; i < runs; i++) {
            if (writeBuffer.remaining() < len + 1) {
                // reader has to catch up.
                while (wc - readCount.get() > 0) ;
                // rewind.
                writeBuffer.put((byte) -1);
                writeBuffer.clear();
            }
            writeBuffer.put((byte) len);
            writeBuffer.putInt(i);
            writeBuffer.put(bytes);
            writeCount.lazySet(++wc);
        }
        // reader has to catch up.
        while (wc - readCount.get() > 0) ;
        t.interrupt();
        t.join();
        long time = System.nanoTime() - start;
        System.out.printf("Message rate was %.1f M/s offsets %d %d %d%n", runs * 1e3 / time
            , addressOf(readBuffer) - addressOf(writeBuffer)
            , addressOf(readCount) - addressOf(writeBuffer)
            , addressOf(writeCount) - addressOf(writeBuffer)
        );
    }

    // assumes -XX:+UseCompressedOops.
    public static long addressOf(Object... o) {
        long offset = UNSAFE.arrayBaseOffset(o.getClass());
        return UNSAFE.getInt(o, offset) * 8L;
    }

    public static final Unsafe UNSAFE = getUnsafe();
    public static Unsafe getUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static class PaddedAtomicInteger extends AtomicInteger {
        public long p2, p3, p4, p5, p6, p7;

        public long sum() {
//            return 0;
            return p2 + p3 + p4 + p5 + p6 + p7;
        }
    }

}

