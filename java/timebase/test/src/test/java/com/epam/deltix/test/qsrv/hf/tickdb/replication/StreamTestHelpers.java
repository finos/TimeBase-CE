package com.epam.deltix.test.qsrv.hf.tickdb.replication;

import com.epam.deltix.gflog.Log;
import com.epam.deltix.gflog.LogFactory;

import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamScope;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickLoader;
import com.epam.deltix.qsrv.test.messages.AggressorSide;
import com.epam.deltix.qsrv.test.messages.MarketEventType;
import com.epam.deltix.qsrv.test.messages.TradeMessage;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author Alexei Osipov
 */
public class StreamTestHelpers {

    public static MessageGenerator createDefaultLoaderRunnable(DXTickStream stream) {
        TickLoader loader = stream.createLoader();

        long endTime = System.currentTimeMillis();
        long duration = TimeUnit.DAYS.toMillis(365);
        long startTime = endTime - duration;

        return new MessageGenerator(loader, Long.MAX_VALUE, getDefatulSymbols(), startTime, endTime, null);
    }

    public static String[] getDefatulSymbols() {
        return new String[]{"AAA", "BBB", "CCC", "DDD"};
    }

    public static DXTickStream createTestStream(DXTickDB db, String streamKey, boolean useExisting) {
        DXTickStream oldStream = db.getStream(streamKey);
        if (oldStream != null) {
            if (useExisting) {
                return oldStream;
            }
            oldStream.delete();
        }

        RecordClassDescriptor mcd = StreamConfigurationHelper.mkMarketMessageDescriptor(null, false);
        RecordClassDescriptor descriptor = StreamConfigurationHelper.mkTradeMessageDescriptor(
                mcd, null, null, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO);

        StreamOptions options = StreamOptions.fixedType(StreamScope.DURABLE, streamKey, streamKey, 0, descriptor);
        options.version = "5.0";
        DXTickStream stream = db.createStream(streamKey, options);
        return stream;
    }



    public static class MessageGenerator implements Runnable {
        private static final Log LOGGER = LogFactory.getLog(MessageGenerator.class);

        private final TickLoader loader;

        private final long total;
        private final String[] symbols;
        private final long startTime;
        private final long endTime;
        private final String space;

        final TradeMessage trade = new TradeMessage();

        final Random random = new Random();

        private MessageGenerator(TickLoader loader, long total, String[] symbols, long startTime, long endTime, String space) {
            this.loader = loader;
            this.total = total;
            this.symbols = symbols;
            this.startTime = startTime;
            this.endTime = endTime;
            this.space = space;
        }

        @Override
        public void run() {
            try {
                long timeIntervalMs = endTime - startTime;
                long endNanoTime = TimeUnit.MILLISECONDS.toNanos(endTime);
                int averageIntervalNanos;
                if (total < Long.MAX_VALUE) {
                    averageIntervalNanos = (int) (TimeUnit.MILLISECONDS.toNanos(timeIntervalMs) / total);
                } else {
                    averageIntervalNanos = (int) TimeUnit.MILLISECONDS.toNanos(10);
                }

                long count = 0;

                long t0 = System.currentTimeMillis();

                long nextNanoTime = TimeUnit.MILLISECONDS.toNanos(startTime);
                while (count < total && nextNanoTime <= endNanoTime && !Thread.currentThread().isInterrupted()) {
//                trade.setSymbol(symbols[(int) (count % symbols.length)]);
                    //trade.setNanoTime(System.nanoTime());
                    //trade.timestamp = System.nanoTime();
//                trade.setNanoTime(nextNanoTime);
                    fillTrade(count, nextNanoTime);
                    loader.send(trade);

                    nextNanoTime += random.nextInt(averageIntervalNanos);
                    count++;
                }


                long t1 = System.currentTimeMillis();
                double s = (t1 - t0) * 0.001;
                System.out.printf(
                        "%s: Write %,d messages in %,.3fs; speed: %,.0f msg/s\n",
                        space,
                        count,
                        s,
                        count / s
                );

                loader.close();
            } catch (Throwable e) {
                LOGGER.error("Error from loader: %s").with(e);
            } finally {
                loader.close();
            }
        }

        private void fillTrade(long count, long nextNanoTime) {
            int index = (int) (count % symbols.length);
            trade.setSymbol(symbols[index]);
            trade.setNanoTime(nextNanoTime);

            trade.setPrice(random.nextDouble() * 1000);
            trade.setSize(random.nextDouble() * 10000);
            trade.setAggressorSide(AggressorSide.values()[random.nextInt(AggressorSide.values().length)]);
            trade.setEventType(MarketEventType.values()[random.nextInt(MarketEventType.values().length)]);
            trade.setCondition("SAMPLE");
            trade.setNetPriceChange(count); // We store message index here

            trade.setCurrencyCode((short) random.nextInt(1000));
            trade.setSourceId(random.nextLong());
        }
    }
}
