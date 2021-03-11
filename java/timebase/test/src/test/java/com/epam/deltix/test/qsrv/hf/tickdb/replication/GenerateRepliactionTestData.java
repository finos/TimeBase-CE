package com.epam.deltix.test.qsrv.hf.tickdb.replication;

import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.qsrv.test.messages.TradeMessage;

/**
 * @author Alexei Osipov
 */
public class GenerateRepliactionTestData {
    public static void main(String[] args) throws InterruptedException {

        DXTickDB db1 = TickDBFactory.connect("localhost", 8011, false);
        db1.open(false);

        DXTickDB db2 = TickDBFactory.connect("localhost", 8022, false);
        db2.open(true);

        String srcStreamKey = "replication_src";
        DXTickStream srcStream = StreamTestHelpers.createTestStream(db1, srcStreamKey, true);

        String dstStreamKey = "replication_dst";
        DXTickStream dstStream = StreamTestHelpers.createTestStream(db2, dstStreamKey, true);


        StreamTestHelpers.MessageGenerator loaderRunnable = StreamTestHelpers.createDefaultLoaderRunnable(srcStream);

        Thread loaderThread = new Thread(loaderRunnable);

        Thread readerThread = new Thread(new ReaderRunnable(dstStream));

        loaderThread.start();
        readerThread.start();


        loaderThread.join();
        readerThread.join();



    }

    private static class ReaderRunnable implements Runnable {
        private final DXTickStream dstStream;

        public ReaderRunnable(DXTickStream dstStream) {
            this.dstStream = dstStream;
        }

        @Override
        public void run() {
            SelectionOptions so = new SelectionOptions();
            so.live = true;
            TickCursor cursor = dstStream.select(Long.MIN_VALUE, so);
            long count = 0;
            long diff = 0;
            while (cursor.next()) {
                TradeMessage msg = (TradeMessage) cursor.getMessage();
                long val = (long) msg.getNetPriceChange();

                long expected = count + diff;
                long currentDiff = val - expected;
                if (currentDiff != 0) {
                    System.err.println("Value mismatch! Got " + val + " expected: " + expected + " diff: " + currentDiff);
                    diff = val - count;

                }
                count++;

                if (count % 100_000 == 0) {
                    System.out.println("Got " + count);
                }
            }
        }
    }
}
