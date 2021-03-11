package com.epam.deltix.qsrv.hf.tickdb.impl.disruptorqueue;

import com.epam.deltix.qsrv.hf.tickdb.pub.Messages;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.data.stream.MessageEncoder;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.impl.TransientStreamImpl;
import com.epam.deltix.qsrv.hf.tickdb.impl.queue.QueueMessageReader;
import com.epam.deltix.qsrv.hf.tickdb.pub.BufferOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.timebase.messages.service.DataLossMessage;
import com.epam.deltix.util.concurrent.UnavailableResourceException;
import com.epam.deltix.util.concurrent.UncheckedInterruptedException;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;
import com.epam.deltix.util.time.TimeKeeper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Alexei Osipov
 */
public class Test_DisruptorMessageQueue {

    private boolean lossless = true;
    private static final int EXPECTED_MESSAGE_COUNT = 1000000;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /*private static final String TWO_BYTE_MESSAGE_DESCRIPTOR_GUID = "___:TwoByteMessage:1";
    private final static String TWO_BYTE_MESSAGE_NAME = TwoByteMessage.class.getName();
    private static final RecordClassDescriptor TWO_BYTE_MESSAGE_DESCRIPTOR = new RecordClassDescriptor(
        TWO_BYTE_MESSAGE_DESCRIPTOR_GUID, TWO_BYTE_MESSAGE_NAME, TWO_BYTE_MESSAGE_NAME, false, null,
        new NonStaticDataField("value1", "value1", new IntegerDataType(IntegerDataType.ENCODING_INT8, false)),
        new NonStaticDataField("value2", "value2", new IntegerDataType(IntegerDataType.ENCODING_INT8, false))
    );*/

    @Test
    // TODO: Test BOTH lossy and lossless
    public void testDataCorrectnessSyncP1C1() throws ExecutionException, InterruptedException {
        TransientStreamImpl stream = new TransientStreamImpl(null, null, new StreamOptions());
        DisruptorMessageQueue queue = new DisruptorMessageQueue(stream, lossless);

        QueueMessageReader reader = queue.getMessageReader(null, false, false);

        MessageChannel<InstrumentMessage> writer = queue.getWriter(new TwoByteMessageEncoder());

        Runnable producer = new FixedMessageCountProducer(writer, EXPECTED_MESSAGE_COUNT);
        SyncMessageConsumer consumer = new SyncMessageConsumer(reader);


        Future<?> producerTaskComplete = executor.submit(producer);
        Future<?> consumerFuture = executor.submit(consumer);


        //measureThroughput(consumer, "Sync Disruptor");


        producerTaskComplete.get();
        Thread.sleep(1); // Let consumer finish
        consumerFuture.cancel(true);
        long messageCount = consumer.getMessageCount();
        System.out.println(messageCount);
        executor.shutdownNow();
        if (lossless) {
            Assert.assertEquals(EXPECTED_MESSAGE_COUNT, messageCount);
        }
    }

    @Test
    public void testResubscribeCorrectnessSync() throws ExecutionException, InterruptedException, IOException {
        TransientStreamImpl stream = new TransientStreamImpl(null, null, new StreamOptions());
        DisruptorMessageQueue queue = new DisruptorMessageQueue(stream, lossless);

        MessageChannel<InstrumentMessage> writer = queue.getWriter(new TwoByteMessageEncoder());
        QueueMessageReader reader1 = queue.getMessageReader(null, false, false);

        // Send some messages
        for (int i = 0; i < 10; i++) {
            TwoByteMessage msg = makeTestMessage(i);
            writer.send(msg);
        }

        // Receive messages with first reader
        for (int i = 0; i < 10; i++) {
            Assert.assertTrue(reader1.read());
            validateCurrentMessageCorrectness(reader1);
        }
        // Close first reader
        reader1.close();

        // Add new reader
        QueueMessageReader reader2 = queue.getMessageReader(null, false, false);
        // Send some more messages
        for (int i = 0; i < 10; i++) {
            TwoByteMessage msg = makeTestMessage(i);
            writer.send(msg);
        }
        // Receive messages with second reader
        for (int i = 0; i < 10; i++) {
            Assert.assertTrue(reader2.read());
            validateCurrentMessageCorrectness(reader2);
        }
    }

    @Test
    public void testResubscribeCorrectnessAsync() throws ExecutionException, InterruptedException, IOException {
        TransientStreamImpl stream = new TransientStreamImpl(null, null, new StreamOptions());
        DisruptorMessageQueue queue = new DisruptorMessageQueue(stream, lossless);

        MessageChannel<InstrumentMessage> writer = queue.getWriter(new TwoByteMessageEncoder());
        QueueMessageReader reader = queue.getMessageReader(null, false, false);
        CountingDownAvailabilityListener r1Listener = new CountingDownAvailabilityListener();
        reader.setAvailabilityListener(r1Listener);

        assertNoDataAvailable(reader);

        // Send some messages
        for (int i = 0; i < 10; i++) {
            TwoByteMessage msg = makeTestMessage(i);
            writer.send(msg);
        }

        // Receive messages with first reader
        r1Listener.await();
        //Assert.assertEquals(r1Listener.getCount(), 1);
        for (int i = 0; i < 10; i++) {
            Assert.assertTrue(reader.read());
            validateCurrentMessageCorrectness(reader);
        }
        assertNoDataAvailable(reader);
        // Close first reader
        reader.close();

        // Add new reader
        final QueueMessageReader reader2 = queue.getMessageReader(null, false, false);
        CountingDownAvailabilityListener r2Listener = new CountingDownAvailabilityListener();
        reader2.setAvailabilityListener(r2Listener);

        assertNoDataAvailable(reader2);

        // Send some more messages
        for (int i = 0; i < 10; i++) {
            TwoByteMessage msg = makeTestMessage(i);
            writer.send(msg);
        }
        // Receive messages with second reader
        r2Listener.await();
        //Assert.assertEquals(r2Listener.getCount(), 1);
        for (int i = 0; i < 10; i++) {
            Assert.assertTrue(reader2.read());
            validateCurrentMessageCorrectness(reader2);
        }
        assertNoDataAvailable(reader2);
    }

    @Test
    public void testMessageLossEvent() throws ExecutionException, InterruptedException, IOException {
        StreamOptions options = new StreamOptions();
        RecordClassSet recordClassSet = new RecordClassSet();
        recordClassSet.addContentClasses(Messages.DATA_LOSS_MESSAGE_DESCRIPTOR);
        options.setMetaData(true, recordClassSet);
        options.bufferOptions = new BufferOptions();
        options.bufferOptions.maxBufferSize = 1024;

        TransientStreamImpl stream = new TransientStreamImpl(null, null, options);
        DisruptorMessageQueue queue = new DisruptorMessageQueue(stream, false);

        MessageChannel<InstrumentMessage> writer = queue.getWriter(new TwoByteMessageEncoder());
        QueueMessageReader reader = queue.getMessageReader(null, true, false);

        // Send some messages. Ensure that we send more messages than buffer's capacity.
        for (int i = 0; i < 1000; i++) {
            TwoByteMessage msg = makeTestMessage(i);
            writer.send(msg);
        }

        Assert.assertTrue(reader.read());
        MemoryDataInput input = reader.getInput();
        Assert.assertTrue(input.getAvail() > TwoByteMessage.MESSAGE_SIZE); // We expect DataLoss message

        // Receive messages with first reader
        for (int i = 0; i < 10; i++) {
            Assert.assertTrue(reader.read());
            validateCurrentMessageCorrectness(reader);
        }
    }

    private static final String MESSAGE_LOSS_RCD_NAME = DataLossMessage.class.getName();

    private static boolean isMessageLossSignal(RawMessage msg) {
        return MESSAGE_LOSS_RCD_NAME.equals(msg.type.getName());
    }


    private static void validateCurrentMessageCorrectness(QueueMessageReader reader) {
        if (reader.getTimestamp() > TimeKeeper.currentTime) {
            throw new IllegalStateException("Wrong time");
        }
        /*if (reader.getTimestamp() < TimeKeeper.currentTime - TimeUnit.MINUTES.toMillis(1)) {
            throw new IllegalStateException("Wrong time (too early");
        }*/
        if (reader.getNanoTime() > TimeKeeper.currentTimeNanos) {
            throw new IllegalStateException("Wrong time nanos");
        }
        /*if (reader.getNanoTime() < TimeKeeper.currentTime - TimeUnit.MINUTES.toNanos(1)) {
            throw new IllegalStateException("Wrong time nanos (too early");
        }*/
        MemoryDataInput input = reader.getInput();
        Assert.assertTrue(input.getAvail() == 2);
        byte val1 = input.readByte();
        byte val2 = input.readByte();
        /*if (((val + 256) & 0xFF) != (messageCounter & 0xFF)) {
            throw new IllegalStateException("Invalid payload");
        }*/
        int value1 = byteToInt(val1);
        int value2 = byteToInt(val2);
        if (value1 + value2 != 255) {
            throw new IllegalStateException("Invalid payload");
        }
    }

    private static TwoByteMessage makeTestMessage(int i) {
        TwoByteMessage msg = new TwoByteMessage();
        // Write two values. Make them always sum up to 255 (0xFF).
        int x = i % 256;
        msg.value1 = intToByte(x);
        msg.value2 = intToByte(0xFF - x);
        return msg;
    }

    private void assertNoDataAvailable(QueueMessageReader reader) throws IOException {
        try {
            reader.read();
            Assert.fail("No exception");
        } catch (UnavailableResourceException e) {
            return;
        } catch (Exception e) {
            Assert.fail("Wrong exception: " + e);
        }
    }

    private static int byteToInt(byte x) {
        return (x + 256) & 0xFF;
    }

    private static byte intToByte(int x) {
        assert x >= 0;
        return (byte) (x & 0xFF);
    }

    private static final class TwoByteMessage extends InstrumentMessage {
        public static final int MESSAGE_SIZE = 2;

        private byte value1;
        private byte value2;
    }

    private static class TwoByteMessageEncoder implements MessageEncoder<InstrumentMessage> {
        @Override
        public boolean encode(InstrumentMessage message, MemoryDataOutput out) {
            TwoByteMessage m = (TwoByteMessage) message;
            out.writeByte(m.value1);
            out.writeByte(m.value2);
            return true;
        }

        @Override
        public int getContentOffset() {
            throw new IllegalStateException();
        }

        @Override
        public int getTypeIndex() {
            throw new IllegalStateException();
        }
    }

    private static class CountingDownAvailabilityListener implements Runnable {
        private final AtomicInteger counter = new AtomicInteger(0);
        private final CountDownLatch countdown = new CountDownLatch(1);

        @Override
        public void run() {
            counter.incrementAndGet();
            countdown.countDown();
        }

        public int getCount() {
            return counter.get();
        }

        public void await() throws InterruptedException {
            countdown.await();
        }
    }

    private static class FixedMessageCountProducer implements Runnable {
        private final MessageChannel<InstrumentMessage> writer;
        private final int messageCount;

        FixedMessageCountProducer(MessageChannel<InstrumentMessage> writer, int messageCount) {
            this.writer = writer;
            this.messageCount = messageCount;
        }

        @Override
        public void run() {
            for (int i = 0; i < messageCount; i++) {
                TwoByteMessage msg = makeTestMessage(i);
                writer.send(msg);
            }
        }
    }

    private static class SyncMessageConsumer implements Runnable {
        volatile long messageCounter;
        final QueueMessageReader reader;

        SyncMessageConsumer(QueueMessageReader reader) {
            this.reader = reader;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    if (reader.read()) {
                        validateCurrentMessageCorrectness(reader);
                        messageCounter++;
                    } else {
                        throw new IllegalStateException();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (UncheckedInterruptedException e) {
                    // We were asked to stop
                    return;
                }
            }
        }

        public long getMessageCount() {
            return messageCounter;
        }
    }
}