package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.test.messages.BestBidOfferMessage;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TestServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.memory.MemoryDataOutput;
import org.junit.*;

import java.io.File;
import java.math.*;
import java.util.*;

import static com.epam.deltix.qsrv.hf.tickdb.TDBRunner.getTemporaryLocation;

/**
 * User: alex
 * Date: Oct 8, 2010
 */
public class Test_LoaderPerformance {

    final int       TOTAL     = 100000000;
    final int       DF        = 0;
    final int       SYMBOLS   = 1000;
    final String    ENCODING  = FloatDataType.ENCODING_FIXED_DOUBLE;

    protected static TDBRunner runner;

    @BeforeClass
    public static void start() throws Throwable {

        String location = getTemporaryLocation();
        DataCacheOptions options = new DataCacheOptions(Integer.MAX_VALUE, 2L << 30);
        runner = new TDBRunner(true, true, location, new TestServer(options, new File(location)));
        runner.startup();
    }

    @AfterClass
    public static void stop() throws Throwable {
        runner.shutdown();
        runner = null;
    }

    @Test
    public void testNumbers() {
        Random rnd = new Random(2010);
        MemoryDataOutput out1 = new MemoryDataOutput(64);
        MemoryDataOutput out2 = new MemoryDataOutput(64);

        long total = 0;
        double[] values = new double[10000];
        for (int i = 0; i < values.length; i++) {
            BigDecimal v = new BigDecimal(rnd.nextDouble());
            values[i] = v.round(new MathContext(5)).doubleValue() + rnd.nextInt(1000);
        }

        for (double value : values) {
            long start = System.currentTimeMillis();
            for (int j = 0; j < 10000; j++) {
                out1.reset();
                out1.oldWriteScaledDouble(value);
            }
            total += System.currentTimeMillis() - start;
        }
        System.out.printf ("oldWriteScaledDouble() overall time = %,.3fs; \n", total * 0.001);

        total = 0;
        for (double value : values) {
            long start = System.currentTimeMillis();
            for (int j = 0; j < 10000; j++) {
                out1.reset();
                out1.writeScaledDouble(value);
            }
            total += System.currentTimeMillis() - start;
        }
        System.out.printf ("writeScaledDouble() overall time = %,.3fs; \n", total * 0.001);

        total = 0;
        for (double value : values) {
            long start = System.currentTimeMillis();
            for (int j = 0; j < 10000; j++) {
                out1.reset();
                out1.writeDouble(value);
            }
            total += System.currentTimeMillis() - start;
        }

        System.out.printf ("writeDouble() overall time = %,.3fs; \n", total * 0.001);
    }

    public void test() {
        RecordClassDescriptor marketMsgDescriptor =
                StreamConfigurationHelper.mkMarketMessageDescriptor (840);

        RecordClassDescriptor bbo = StreamConfigurationHelper.mkBBOMessageDescriptor(marketMsgDescriptor,
                true, "", 840, ENCODING, ENCODING);
        RecordClassDescriptor trade = StreamConfigurationHelper.mkTradeMessageDescriptor(marketMsgDescriptor,
                "", 840, ENCODING, ENCODING);

        DXTickStream stream = runner.getTickDb().getStream("test");
        if (stream == null)
            stream = runner.getTickDb().createStream("test",
                    StreamOptions.polymorphic(StreamScope.DURABLE, "test", null, DF, bbo, trade));

        StringBuffer ch = new StringBuffer("AAAAAA");
        String[] symbols = new String[SYMBOLS];
        for (int i = 0; i < symbols.length; i++) {
            symbols[i] = ch.toString();
            increment(ch, 4);
        }

        //L2Generator generator = new L2Generator(new GregorianCalendar(2000, 1, 1), 1000, symbols);
        DataGenerator generator = new DataGenerator(new GregorianCalendar(2000, 1, 1), 1, symbols);
        LoadingOptions options = new LoadingOptions();
        //options.channelPerformance = ChannelPerformance.MIN_LATENCY;
        TickLoader loader = stream.createLoader(options);

        long                            t0 = System.currentTimeMillis ();
        long                            count = 0;

        while (generator.next() && count < TOTAL) {
            loader.send(generator.getMessage());
            count++;
            if (count % (TOTAL / 10) == 0) {
                System.out.printf("Send  %,3d messages\n", count);
            }
        }

        long                            t1 = System.currentTimeMillis ();
        double                          s = (t1 - t0) * 0.001;
        System.out.printf (
                "%,d messages in %,.3fs; speed: %,.0f msg/s\n",
                count,
                s,
                count / s
        );

        loader.close();

        long                            t2 = System.currentTimeMillis ();
        System.out.printf ("Flushing time = %,.3fs\n", (t2 - t1) * 0.001);

        s = (t2 - t0) * 0.001;
        System.out.printf (
                "Overall: %,d messages in %,.3fs; speed: %,.0f msg/s\n",
                count,
                s,
                count / s
        );

        stream.delete();
    }

    protected void increment(StringBuffer symbol, int index) {
        if (symbol.charAt(index) == (int)'Z') {
            increment(symbol, index - 1);
            symbol.setCharAt(index, 'A');
        }
        else
            symbol.setCharAt(index, (char) (symbol.charAt(index) + 1));
    }

    public static void main (String [] args) throws Throwable {
        Test_LoaderPerformance.start();
        new Test_LoaderPerformance().test();
        Test_LoaderPerformance.stop();
    }
}

class DataGenerator extends BaseGenerator<InstrumentMessage> {
    protected int count;
    protected TradeMessage trade = new TradeMessage();
    protected BestBidOfferMessage bbo1 = new BestBidOfferMessage();
    protected BestBidOfferMessage bbo2 = new BestBidOfferMessage();

    public DataGenerator(GregorianCalendar calendar, int interval, String ... symbols) {
        super(calendar, interval, symbols);

        trade.setPrice(nextDouble());
        trade.setSize(rnd.nextInt(1000));
        trade.setCurrencyCode((short)840);

        bbo1.setBidPrice(nextDouble());
        bbo1.setBidSize(rnd.nextInt(1000));

        bbo2.setOfferPrice(nextDouble());
        bbo2.setOfferSize(rnd.nextInt(1000));
    }

    public boolean next() {

        InstrumentMessage message;

        if (count % 10 == 0) {
            message = trade;
        } else {
            if (count % 2 == 0)
                message = bbo1;
            else
                message = bbo2;
        }

        message.setSymbol(symbols.next());
        message.setTimeStampMs(symbols.isReseted() ? getNextTime() : getActualTime());

        current = message;
        count++;
        return true;
    }

    public boolean isAtEnd() {
        return false;
    }
}


abstract class BaseGenerator<T> implements MessageSource<T> {
    protected final Random rnd = new Random(2011);
    protected GregorianCalendar calendar;
    private int interval;
    protected T current;
    protected CycleIterator<String> symbols;

    protected BaseGenerator(GregorianCalendar calendar, int interval, String ... tickers) {
        this.calendar = calendar;
        this.interval = interval;
        setSymbols(tickers);
    }

    protected double nextDouble() {
        BigDecimal v = new BigDecimal(rnd.nextDouble());
        return v.round(new MathContext(5)).doubleValue() + rnd.nextInt(1000);
    }

    public T getMessage() {
        return current;
    }

    protected long getNextTime() {
        calendar.add(Calendar.MILLISECOND, interval);
        return calendar.getTimeInMillis();
    }

    public long getActualTime() {
        return calendar.getTimeInMillis();
    }

    public void setSymbols(String ... tickers) {
        this.symbols = new CycleIterator<String>(Arrays.asList(tickers));
    }

    public void close() {
    }
}

class CycleIterator<T> {
    private List<T> data;
    private Iterator<T> it;
    private boolean reseted;

    public CycleIterator(List<T> data) {
        this.data = data;
    }

    public boolean isReseted() {
        return reseted;
    }

    public T next() {
        reseted = it != null && !it.hasNext();

        if (it == null || !it.hasNext())
            it = data.iterator();

        return it.next();
    }
}
class RandomIterator<T> {
    private List<T>     data;
    private int         count = 0;
    private Random      rnd;

    public RandomIterator(List<T> data) {
        this.data = data;
        this.rnd = new Random(2010);
    }

    public boolean isReseted() {
        return count % data.size() == 0;
    }

    public T next() {
        int index = rnd.nextInt(data.size());
        count++;
        return data.get(index);
    }
}