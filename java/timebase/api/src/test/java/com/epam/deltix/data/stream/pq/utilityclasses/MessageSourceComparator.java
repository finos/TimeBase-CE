package com.epam.deltix.data.stream.pq.utilityclasses;

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.TimeStampedMessage;

import java.util.Comparator;

/**
 * @author Alexei Osipov
 */
public class MessageSourceComparator<T extends TimeStampedMessage> implements Comparator<MessageSource<T>> {
    @Override
    public int compare(MessageSource<T> o1, MessageSource<T> o2) {
        TimeStampedMessage m1 = o1.getMessage();
        TimeStampedMessage m2 = o2.getMessage();
        long time1 = m1.getNanoTime();
        long time2 = m2.getNanoTime();
        return Long.compare(time1, time2);
    }
}
