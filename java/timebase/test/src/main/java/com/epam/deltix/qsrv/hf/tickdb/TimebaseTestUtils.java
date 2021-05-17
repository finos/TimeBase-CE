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
package com.epam.deltix.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.test.messages.*;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.stream.MessageFileHeader;
import com.epam.deltix.qsrv.hf.stream.Protocol;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.TickDBShell;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.GMT;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;

/**
 * Description: deltix.qsrv.hf.tickdb.TimebaseTestUtils
 * Date: Mar 19, 2010
 *
 * @author Nickolay Dul
 */
public final class TimebaseTestUtils {


    public static void recreateTimebase(String location) {
        DXTickDB db = TickDBFactory.create(location);
        try {
            db.format();
        } finally {
            Util.close(db);
        }
    }

    public static void createFileStream(String timebasePath, String streamKey, String messageFilePath) throws Throwable {
        DXTickDB db = TickDBFactory.create(timebasePath);
        db.open(false);
        db.createFileStream(streamKey, messageFilePath);
        db.close();
    }

    public static void importFromFile(String timebasePath, String streamKey, int df, String messageFilePath) throws Throwable {
        DXTickDB db = TickDBFactory.create(timebasePath);
        db.open(false);

        DXTickStream first = db.createStream(streamKey, streamKey, null, df);
        MessageFileHeader header = Protocol.readHeader(new File(messageFilePath));
        first.setPolymorphic (header.getTypes());
        TickDBShell.loadMessageFile(new File(messageFilePath), first);

        db.close();
    }

    public static void populateStream(DXTickStream stream, MessageSource<InstrumentMessage> source) {
        TickLoader loader = stream.createLoader(new LoadingOptions(false));
        try {
            while (source.next()) {
                loader.send(source.getMessage());
            }
        } finally {
            Util.close(loader);
        }
    }

    public static BestBidOfferMessage createBBO(IdentityKey instrument, long timestamp,
                                                                             double bidPrice, double bidSize, double offerPrice, double offerSize) {
        BestBidOfferMessage bbo = new BestBidOfferMessage();

        bbo.setSymbol(instrument.getSymbol().toString());

        bbo.setTimeStampMs(timestamp);
        bbo.setBidPrice(bidPrice);
        bbo.setBidSize(bidSize);
        bbo.setOfferPrice(offerPrice);
        bbo.setOfferSize(offerSize);

        return bbo;
    }

    public static TradeMessage createTrade(IdentityKey instrument, long timestamp,
                                                                        double price, double size) {
        TradeMessage trade = new TradeMessage();

        trade.setSymbol(instrument.getSymbol().toString());

        trade.setTimeStampMs(timestamp);
        trade.setPrice(price);
        trade.setSize(size);

        return trade;
    }

    public static BarMessage createBar(IdentityKey instrument, long timestamp,
                                                                    double open, double close, double high, double low, double volume) {
        BarMessage bar = new BarMessage();

        bar.setSymbol(instrument.getSymbol().toString());

        bar.setTimeStampMs(timestamp);
        bar.setOpen(open);
        bar.setClose(close);
        bar.setHigh(high);
        bar.setLow(low);
        bar.setVolume(volume);

        return bar;
    }

    public static MarketMessage createTestMessage(Random rnd,
                                                                               int type,
                                                                               String symbol,
                                                                               long timestamp) {
        Class<? extends MarketMessage> messageType;
        switch (type) {
            case 0:
                messageType = BestBidOfferMessage.class;
                break;
            case 1:
                messageType = TradeMessage.class;
                break;
            case 2:
                messageType = BarMessage.class;
                break;
            default:
                throw new IllegalStateException("Unknown type: " + type);
        }
        return createTestMessage(rnd, messageType, symbol, timestamp);
    }

    public static MarketMessage createTestMessage(Random rnd,
                                                                               Class<? extends MarketMessage> type,
                                                                               String symbol,
                                                                               long timestamp) {
        MarketMessage message;
        if (BestBidOfferMessage.class.isAssignableFrom(type)) {
            message = new BestBidOfferMessage();
            BestBidOfferMessage bbo = (BestBidOfferMessage) message;
            switch (0) {
                case 0:
                    bbo.setBidPrice(rnd.nextDouble()*100);
                    bbo.setBidSize(rnd.nextInt(1000));
                    bbo.setOfferPrice(rnd.nextDouble()*100);
                    bbo.setOfferSize(rnd.nextInt(1000));
                    break;
                case 1:
                    bbo.setBidPrice(rnd.nextDouble()*100);
                    bbo.setBidSize(rnd.nextInt(1000));
                    break;
                default:
                    bbo.setOfferPrice(rnd.nextDouble()*100);
                    bbo.setOfferSize(rnd.nextInt(1000));
            }
        } else if (TradeMessage.class.isAssignableFrom(type)) {
            message = new TradeMessage();
            TradeMessage trade = (TradeMessage) message;
            trade.setPrice(rnd.nextDouble()*100);
            trade.setSize(rnd.nextInt(1000));
        } else if (BarMessage.class.isAssignableFrom(type)) {
            message = new BarMessage();
            BarMessage bar = (BarMessage) message;
            bar.setHigh(rnd.nextDouble()*100);
            bar.setOpen(bar.getHigh() - rnd.nextDouble()*10);
            bar.setClose(bar.getHigh() - rnd.nextDouble()*10);
            bar.setLow(Math.min(bar.getOpen(), bar.getClose()) - rnd.nextDouble()*10);
            bar.setVolume(rnd.nextInt(10000));
        } else
            throw new IllegalStateException("Unknown message type: " + type);

        message.setTimeStampMs(timestamp);
        message.setSymbol(symbol);
        message.setCurrencyCode((short)840);
        return message;
    }

    public static void printCursor(MessageSource<InstrumentMessage> cur, PrintWriter out) {
        while (cur.next()) {
            printMessage(cur.getMessage(), out);
        }
    }

    public static void printMessage(InstrumentMessage msg, PrintWriter out) {
        out.println(messageToString(msg));
    }

    public static String messageToString(InstrumentMessage msg) {
        if (msg instanceof RawMessage) {
            return msg.toString();
        }

        StringBuilder builder = new StringBuilder(200);
        builder.append(
            String.format("%s,%s,%s,%d",
                          msg.getClass().getSimpleName(),
                    msg.getSymbol(),
                          GMT.formatDateTimeMillis(msg.getTimeStampMs()),
                    msg.getTimeStampMs())
        );

        if (msg instanceof MarketMessage) {
            MarketMessage mm = (MarketMessage) msg;
            builder.append(String.format(",%s", mm.getCurrencyCode()));

            if (msg instanceof BestBidOfferMessage) {
                BestBidOfferMessage bbo = (BestBidOfferMessage) msg;
                builder.append(
                  String.format(",%.2f,%.2f,%s,%.2f,%.2f,%s",
                          bbo.getBidPrice(),
                          bbo.getBidSize(),
                                ExchangeCodec.longToCode(bbo.getBidExchangeId()),
                          bbo.getOfferPrice(),
                          bbo.getOfferSize(),
                                ExchangeCodec.longToCode(bbo.getOfferExchangeId()))
                );
            } else if (msg instanceof TradeMessage) {
                TradeMessage trade = (TradeMessage) msg;
                builder.append(
                        String.format(",%s,%.2f,%.2f",
                                      ExchangeCodec.longToCode(trade.getExchangeId()),
                                trade.getPrice(),
                                trade.getSize())
                );
            } else if (msg instanceof BarMessage) {
                BarMessage bar = (BarMessage) msg;
                builder.append(
                        String.format(",%s,%.2f,%.2f,%.2f,%.2f,%.2f",
                                      ExchangeCodec.longToCode(bar.getExchangeId()),
                                bar.getOpen(),
                                bar.getHigh(),
                                bar.getLow(),
                                bar.getClose(),
                                bar.getVolume())
                );
            }
        }
        return builder.toString();
    }

    ////////////////////////////// HELPER CLASSES ///////////////////////

    public static class IterableMessageSource<T> implements MessageSource<T> {
        private final Iterable<? extends T> source;
        private Iterator<? extends T> iterator;

        @SafeVarargs
        @SuppressWarnings("varargs")
        public IterableMessageSource(T... messages) {
            this(new ArrayList<T>(Arrays.asList(messages)));
        }

        public IterableMessageSource(Iterable<? extends T> source) {
            this.source = source;
            reset();
        }

        public void reset() {
            iterator = source.iterator();
        }

        @Override
        public T getMessage() {
            return iterator.next();
        }

        @Override
        public boolean next() {
            return iterator.hasNext();
        }

        @Override
        public boolean isAtEnd() {
            return !iterator.hasNext();
        }

        @Override
        public void close() {
        }
    }

    @SuppressFBWarnings(value = "PREDICTABLE_RANDOM", justification = "Random is used for fuzzy tests, not cryptography")
    public static class CycleMarketMessageSource implements MessageSource<InstrumentMessage> {
        private final int maxCycleCount;
        private final long start;
        private final long interval;

        private final int[] types;
        private final String[] symbols;

        private final Random rnd = new Random();

        private InstrumentMessage message;
        private int counter;

        public CycleMarketMessageSource(int maxCycleCount, long start, long interval,
                                        int[] types, String[] symbols) {
            this.maxCycleCount = maxCycleCount;
            this.start = start;
            this.interval = interval;
            this.types = types;
            this.symbols = symbols;
        }

        public void reset() {
            message = null;
            counter = 0;
        }

        @Override
        public InstrumentMessage getMessage() {
            return message;
        }

        @Override
        public boolean next() {
            int cycleCount = counter / (types.length * symbols.length);
            if (cycleCount > maxCycleCount)
                return false;

            int cycleIndex = counter % (types.length * symbols.length);
            int typeIndex = cycleIndex / (symbols.length);
            int entityTypeIndex = (cycleIndex % (symbols.length)) / symbols.length;
            int symbolIndex = (cycleIndex % (symbols.length)) % symbols.length;

            long time = start + counter * interval;
            message = createTestMessage(rnd, types[typeIndex], symbols[symbolIndex], time);

            counter++;
            return true;
        }

        @Override
        public boolean isAtEnd() {
            return false;
        }

        @Override
        public void close() {
        }
    }

}
