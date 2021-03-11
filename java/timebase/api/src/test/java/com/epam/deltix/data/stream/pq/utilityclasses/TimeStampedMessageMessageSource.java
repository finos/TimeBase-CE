package com.epam.deltix.data.stream.pq.utilityclasses;

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.TimeStampedMessage;

import java.util.Random;

/**
 * @author Alexei Osipov
 */
public class TimeStampedMessageMessageSource implements MessageSource<TimeStampedMessage> {
    private final Random random;
    private final int step;
    private final InstrumentMessage message = new InstrumentMessage();
    private final long multiplier;

    public TimeStampedMessageMessageSource(long baseTimestampForSource, Random random, int step, long multiplier) {
        this.multiplier = multiplier;
        this.message.setNanoTime(baseTimestampForSource);

        this.random = random;
        this.step = step;
    }

    @Override
    public InstrumentMessage getMessage() {
        return message;
    }

    @Override
    public boolean next() {
        long prevTime = message.getNanoTime();
        long newTime = getNextRandomTime(prevTime, random, step, multiplier);
        message.setNanoTime(newTime);
        return true;
    }

    @Override
    public boolean isAtEnd() {
        return false;
    }

    @Override
    public void close() {
    }

    @Override
    public String toString() {
        return "TSM{" +
                "ts=" + message.getNanoTime() +
                '}';
    }

    public static long getNextRandomTime(long prevTime, Random random, int step) {
        return prevTime + random.nextInt(step);  // Value 0 is included
    }

    public static long getNextRandomTime(long prevTime, Random random, int step, long multiplier) {
        return prevTime + random.nextInt(step) * multiplier;  // Value 0 is included
    }
}
