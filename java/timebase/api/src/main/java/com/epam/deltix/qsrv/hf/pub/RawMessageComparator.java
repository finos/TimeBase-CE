package com.epam.deltix.qsrv.hf.pub;

import com.epam.deltix.util.lang.MathUtil;
import com.epam.deltix.util.lang.Util;
import java.util.Comparator;

public class RawMessageComparator implements Comparator <RawMessage> {
    public static final RawMessageComparator     ALL_COMPARING_INSTANCE =
        new RawMessageComparator (true, true);
    
    private boolean         compareTime;
    private boolean         compareEntity;
    private boolean         compareType;

    public RawMessageComparator (boolean compareTime, boolean compareEntity) {
        this(compareTime, compareEntity, true);
    }

    public RawMessageComparator(boolean compareTime, boolean compareEntity, boolean compareType) {
        this.compareTime = compareTime;
        this.compareEntity = compareEntity;
        this.compareType = compareType;
    }

    public int      compare (RawMessage o1, RawMessage o2) {
        long         dif;
        
        if (compareTime) {
            dif = MathUtil.sign (o1.getTimeStampMs() - o2.getTimeStampMs());
            
            if (dif != 0)
                return (int)(dif);
            
            dif = o1.getNanoTime() - o2.getNanoTime();
            
            if (dif != 0)
                return (int)(dif);
        }
        
        if (compareEntity) {
            dif = Util.compare (o1.getSymbol(), o2.getSymbol(), false);
            
            if (dif != 0)
                return (int)(dif);
        }

        if (compareType) {
            dif = o1.type.compareTo (o2.type);

            if (dif != 0)
                return (int)(dif);
        }

        return (Util.arraycomp (o1.data, o1.offset, o1.length, o2.data, o2.offset, o2.length));
    }  
}
