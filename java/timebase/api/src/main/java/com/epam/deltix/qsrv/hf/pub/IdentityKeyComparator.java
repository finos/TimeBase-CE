package com.epam.deltix.qsrv.hf.pub;

import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.util.lang.Util;
import java.util.Comparator;

/**
 *
 */
public class IdentityKeyComparator
    implements Comparator <IdentityKey>
{
    private final boolean       symbolAscending;

    public IdentityKeyComparator (boolean symbolAscending) {
        this.symbolAscending = symbolAscending;
    }

    public int      compare (IdentityKey a, IdentityKey b) {
        int     dif;

        if ((dif = Util.compare (a.getSymbol (), b.getSymbol (), false)) != 0)
            return (symbolAscending ? dif : -dif);

        return (0);
    }

//    private int     ctype (IdentityKey a, IdentityKey b) {
//        return (a.getInstrumentType().ordinal () - b.getInstrumentType().ordinal ());
//    }

    public static final IdentityKeyComparator   DEFAULT_INSTANCE =
        new IdentityKeyComparator (true);

}
