package com.epam.deltix.util.vsocket;

import com.epam.deltix.util.memory.MemoryDataOutput;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;

/**
 * @author Alexei Osipov
 */
public class AioLatencyTest {
    public static void main(String[] args) throws IOException, InterruptedException {
        final AsynchronousServerSocketChannel serverChannel =
                AsynchronousServerSocketChannel.open()
                        //.setOption(StandardSocketOptions.SO_SNDBUF, 16 * 1024 * 1024)
                        .bind(new InetSocketAddress(5000));

        serverChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel,Void>() {
            public void completed(AsynchronousSocketChannel ch, Void att) {
                // accept the next connection
                serverChannel.accept(null, this);

                // handle this connection
                handleServerConnect(ch);
            }
            public void failed(Throwable exc, Void att) {
                exc.printStackTrace();
            }
        });

        AsynchronousSocketChannel clientChannel = AsynchronousSocketChannel.open();
        InetSocketAddress hostAddress = new InetSocketAddress("localhost", 5000);
        clientChannel.setOption(StandardSocketOptions.SO_RCVBUF, 16 * 1024 * 1024);
        clientChannel.connect(hostAddress, null, new CompletionHandler<Void, Void>() {
            @Override
            public void completed(Void result, Void attachment) {
                //doForConnectedClinet(clientChannel);
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                exc.printStackTrace();
            }
        });

        CountDownLatch countDownLatch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(countDownLatch::countDown));
        countDownLatch.await();

        serverChannel.close();
    }

    private static void handleServerConnect(AsynchronousSocketChannel ch) {
        try {
            ch.setOption(StandardSocketOptions.SO_SNDBUF, 16 * 1024 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int flushLimit = 128 * 1024;
        ByteBuffer writeBuffer = ByteBuffer.allocate(flushLimit * 2);
        ByteBuffer readBuffer = ByteBuffer.allocate(flushLimit * 2);

        MessageGenerator mg = new MessageGenerator();
        MessageGenerator2 mg2 = new MessageGenerator2();


        CompletionHandler<Integer, Void> readHandler = new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer result, Void attachment) {
                if (writeBuffer.remaining() > 0) {

                }
                while (readBuffer.remaining() >= Long.BYTES) {
                    long value = readBuffer.getLong();
                    writeBuffer.putLong(value);
                }
                boolean hadDataToSend = writeBuffer.position() > 0;
                writeBuffer.flip();
                if (hadDataToSend) {
                    ch.write(writeBuffer, null, this);
                }

                ch.read(readBuffer, null, this);
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                exc.printStackTrace();
            }
        };
        ch.read(readBuffer, null, readHandler);
    }

    private static class MessageGenerator {
        //Random rng = new Random(0);
        long messageIndex = 0;

        void writeMessage(MemoryDataOutput mdo) {
            messageIndex ++;
            int size = 50 + Long.BYTES;
            mdo.writeInt(size);
            mdo.writeLong(messageIndex);
            for (int i = 0; i < 50; i++) {
                mdo.writeByte(i);
            }
        }
    }

    private static class MessageGenerator2 {
        //Random rng = new Random(0);
        long messageIndex = 0;

        void writeMessage(ByteBuffer buf) {
            messageIndex ++;
            int size = 50 + Long.BYTES;
            buf.putInt(size);
            buf.putLong(messageIndex);
            for (int i = 0; i < 50; i++) {
                buf.put((byte) i);
            }
        }
    }

    private static class WriteContext {
    }

    private static class IntegerWriteContextCompletionHandler implements CompletionHandler<Integer, WriteContext> {
        private final ByteBuffer writeBuffer;
        private final MemoryDataOutput mdo;
        private final int flushLimit;
        private final MessageGenerator mg;
        private final AsynchronousSocketChannel ch;
        private final MessageGenerator2 mg2;

        public IntegerWriteContextCompletionHandler(ByteBuffer writeBuffer, MemoryDataOutput mdo, int flushLimit, MessageGenerator mg, AsynchronousSocketChannel ch, MessageGenerator2 mg2) {
            this.writeBuffer = writeBuffer;
            this.mdo = mdo;
            this.flushLimit = flushLimit;
            this.mg = mg;
            this.ch = ch;
            this.mg2 = mg2;
        }

        @Override
        public void completed(Integer result, WriteContext attachment) {
            //prepareForSend(attachment);
            prepareForSend2(attachment);
        }

        private void prepareForSend2(WriteContext attachment) {
            if (!ch.isOpen()) {
                System.err.println("Channel is closed");
                return;
            }
            writeBuffer.mark();
            int remaining = writeBuffer.remaining();
            if (remaining > 0) {
                // Some data left in buffer
                if (remaining < 512) {
                    // Amount of data is small - compact buffer
                    writeBuffer.compact();
                } else {
                    int oldLimit = writeBuffer.limit();
                    writeBuffer.limit(writeBuffer.capacity());
                    writeBuffer.position(oldLimit);
                }
            } else {
                // Buffer was fully written
                writeBuffer.clear();
            }
            while (writeBuffer.position() < flushLimit) {
                mg2.writeMessage(writeBuffer);
            }
            writeBuffer.flip();
            ch.write(writeBuffer, attachment, this);
        }

        @Override
        public void failed(Throwable exc, WriteContext attachment) {
            exc.printStackTrace();
        }
    }
}
