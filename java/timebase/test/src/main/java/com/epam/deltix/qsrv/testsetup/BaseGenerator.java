package com.epam.deltix.qsrv.testsetup;

import com.epam.deltix.streaming.MessageSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

@SuppressFBWarnings(value = "PREDICTABLE_RANDOM", justification = "Random is used for internal tests, not cryptography")
public abstract class BaseGenerator<T> implements MessageSource<T> {
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