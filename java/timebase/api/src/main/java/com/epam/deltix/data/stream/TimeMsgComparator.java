package com.epam.deltix.data.stream;

import com.epam.deltix.timebase.messages.TimeStampedMessage;
import com.epam.deltix.util.lang.MathUtil;
import java.util.Comparator;

/**
 *
 */
public class TimeMsgComparator 
    implements Comparator <TimeStampedMessage>
{
    private TimeMsgComparator () { }
    
    public static final Comparator <TimeStampedMessage> INSTANCE =
        new TimeMsgComparator ();
    
    public int      compare (
        TimeStampedMessage o1,
        TimeStampedMessage o2
    )
    {
        return (MathUtil.compare (o1.getNanoTime(), o2.getNanoTime()));
    }
}
