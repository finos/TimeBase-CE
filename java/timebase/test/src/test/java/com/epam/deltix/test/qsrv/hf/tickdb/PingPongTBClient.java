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
package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.pub.ExchangeCodec;
import com.epam.deltix.qsrv.test.messages.BestBidOfferMessage;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.lang.Disposable;

import java.util.concurrent.atomic.AtomicLong;

public class PingPongTBClient implements Disposable {

    private static final String HOST = "localhost";
    private static final int PORT = 8011;
    private static final boolean USE_SSL = false;
    private static final String STREAM = "test";

    private Tracer tracer;
    private Producer producer;
    private Consumer consumer;

    public synchronized void start() {
        DXTickDB timebase = openTimebase();
        DXTickStream stream = timebase.getStream(STREAM);

        tracer = new Tracer();
        tracer.start();

        producer = createProducer(stream, tracer);
        producer.start();

        consumer = createConsumer(stream, tracer);
        consumer.start();
    }

    public synchronized void close() {
        consumer.close();
        producer.close();
        tracer.close();
    }

    private static DXTickDB openTimebase() {
        DXTickDB timebase = TickDBFactory.connect(HOST, PORT, USE_SSL);
        timebase.open(false);
        return timebase;
    }

    private static Producer createProducer(DXTickStream stream, Tracer tracer) {
        TickLoader loader = stream.createLoader();
        return new Producer(loader, tracer);
    }

    private static Consumer createConsumer(DXTickStream stream, Tracer tracer) {
        SelectionOptions options = new SelectionOptions(true, true);
        TickCursor cursor = stream.select(TimeConstants.USE_CURRENT_TIME, options);
        return new Consumer(cursor, tracer);
    }

    private static abstract class Worker extends Thread implements Disposable {

        protected volatile boolean active = true;

        @Override
        public void run() {
            while (active)
                work();
        }

        @Override
        public void close() {
            active = false;
            waitForWorkerFinished();
        }

        protected void waitForWorkerFinished() {
            try {
                join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        protected abstract void work();

    }

    private static class Producer extends Worker {

        private static final int SYMBOL_COUNT = 1 << 10;
        private static final InstrumentMessage[] MESSAGES = createMessages();

        private final TickLoader loader;
        private final Tracer tracer;

        public Producer(TickLoader loader, Tracer tracer) {
            this.loader = loader;
            this.tracer = tracer;
        }

        @Override
        protected void work() {
            for (InstrumentMessage message : MESSAGES)
                sendMessage(message);
        }

        @Override
        public void close() {
            super.close();
            loader.close();
        }

        private void sendMessage(InstrumentMessage message) {
            loader.send(message);
            tracer.incrementLoaded();
        }

        private static InstrumentMessage[] createMessages() {
            InstrumentMessage[] messages = new InstrumentMessage[SYMBOL_COUNT];
            for (int i = 0; i < SYMBOL_COUNT; i++)
                messages[i] = createMessage("SYMBOL#" + i);

            return messages;
        }

        private static InstrumentMessage createMessage(String symbol) {
            BestBidOfferMessage message = new BestBidOfferMessage();
            message.setBidExchangeId(ExchangeCodec.codeToLong("CME"));
            message.setOfferExchangeId(ExchangeCodec.codeToLong("CBOT"));
            message.setCurrencyCode((short)ExchangeCodec.codeToLong("USD"));
            message.setBidPrice(100.0);
            message.setOfferPrice(200.0);
            message.setBidSize(100000.0);
            message.setOfferSize(10000.0);
            message.setSymbol(symbol);

            return message;
        }

    }

    private static class Consumer extends Worker {

        private final TickCursor cursor;
        private final Tracer tracer;

        public Consumer(TickCursor cursor, Tracer tracer) {
            this.cursor = cursor;
            this.tracer = tracer;
        }

        @Override
        protected void work() {
            cursor.next();
            tracer.incrementRead();
        }

        @Override
        public void close() {
            super.close();
            cursor.close();
        }
    }

    private static class Tracer extends Worker {

        private static final int PERIOD_MILLIS = 10000;

        private final PaddedAtomicLong loaded = new PaddedAtomicLong();
        private final PaddedAtomicLong read = new PaddedAtomicLong();

        private long lastTime;
        private long lastLoaded;
        private long lastRead;

        @Override
        public void run() {
            lastTime = System.currentTimeMillis();
            super.run();
        }

        @Override
        protected void work() {
            sleep();

            long loaded = this.loaded.get();
            long deltaLoaded = loaded - lastLoaded;
            lastLoaded = loaded;

            long read = this.read.get();
            long deltaRead = read - lastRead;
            lastRead = read;


            long time = System.currentTimeMillis();
            long deltaTime = time - lastTime;
            lastTime = time;

            System.out.println("Period: " + deltaTime + " ms. Loaded: " + deltaLoaded + ". Read: " + deltaRead);
        }

        @Override
        public void close() {
            active = false;
            interrupt();
            waitForWorkerFinished();
        }

        private void sleep() {
            try {
                sleep(PERIOD_MILLIS);
            } catch (InterruptedException e) {
                if (active)
                    throw new RuntimeException(e);
            }
        }

        public void incrementLoaded() {
            loaded.set(loaded.get() + 1);
        }

        public void incrementRead() {
            read.set(read.get() + 1);
        }

    }

    private static class PaddedAtomicLong extends AtomicLong {

        public volatile long p1, p2, p3, p4, p5, p6, p7, p8 = 8L;

        public long preventFalseSharing() {
            return p1 + p2 + p3 + p4 + p5 + p6 + p7 + p8;
        }

    }

    public static void main(String[] args) {
        PingPongTBClient client = new PingPongTBClient();
        client.start();

        Thread closer = createClientCloser(client);
        Runtime.getRuntime().addShutdownHook(closer);
    }

    private static Thread createClientCloser(final PingPongTBClient client) {
        return new Thread() {
            @Override
            public void run() {
                client.close();
            }
        };
    }

}
