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
public class AioServTest {
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
                handle(ch);
            }
            public void failed(Throwable exc, Void att) {
                exc.printStackTrace();
            }
        });

        CountDownLatch countDownLatch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(countDownLatch::countDown));
        countDownLatch.await();

        serverChannel.close();
    }

    private static void handle(AsynchronousSocketChannel ch) {
        try {
            ch.setOption(StandardSocketOptions.SO_SNDBUF, 16 * 1024 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int flushLimit = 128 * 1024;
        MemoryDataOutput mdo = new MemoryDataOutput(2 * flushLimit);
        ByteBuffer writeBuffer = ByteBuffer.wrap(mdo.getBuffer());

        MessageGenerator mg = new MessageGenerator();
        MessageGenerator2 mg2 = new MessageGenerator2();

        /*while (mdo.getPosition() < flushLimit) {
            mg.writeMessage(mdo);
        }
        writeBuffer.limit(mdo.getPosition());*/
        IntegerWriteContextCompletionHandler handler = new IntegerWriteContextCompletionHandler(writeBuffer, mdo, flushLimit, mg, ch, mg2);
        //handler.prepareForSend(new WriteContext());
        handler.prepareForSend(new WriteContext());
    }

    private static class MessageGenerator {
        //Random rng = new Random(0);

        long messageIndex = 0;

        void writeMessage(MemoryDataOutput mdo) {
            messageIndex++;
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

        void writeMessage(ByteBuffer buf) {
            int size = 50;
            buf.putInt(size);
            for (int i = 0; i < size; i++) {
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
            prepareForSend(attachment);
        }

        private void prepareForSend(WriteContext attachment) {
            if (!ch.isOpen()) {
                System.err.println("Channel is closed");
                return;
            }
            int remaining = writeBuffer.remaining();
            if (remaining > 0) {
                // Some data left in buffer
                if (remaining < 512) {
                    // Amount of data is small - compact buffer
                    writeBuffer.compact();
                    mdo.seek(remaining);
                }
            } else {
                // Buffer was fully written
                writeBuffer.clear();
                mdo.reset();
            }
            while (mdo.getPosition() < flushLimit) {
                mg.writeMessage(mdo);
            }
            writeBuffer.limit(mdo.getPosition());
            ch.write(writeBuffer, attachment, this);
        }


        @Override
        public void failed(Throwable exc, WriteContext attachment) {
            exc.printStackTrace();
        }
    }
}