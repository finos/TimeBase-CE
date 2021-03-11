package com.epam.deltix.test.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.pub.CurrencyCodec;
import org.junit.*;
import static org.junit.Assert.*;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.UHFFramework;

/**
 *
 */

@Category(UHFFramework.class)
public class Test_CurrencyCodec {
    private void        roundTrip (String ccy) {
        short   x = CurrencyCodec.codeToShort (ccy);
        String  s = CurrencyCodec.intToCode (x);

        assertEquals (ccy.toUpperCase (), s);
    }

    private void        checkFail (String badCcy) {
        try {
            CurrencyCodec.codeToInt (badCcy);
            assertTrue ("No exception", false);
        } catch (NumberFormatException x) {
            assertEquals (badCcy, x.getMessage ());
        }
    }

    @Test
    public void         smoke () {        
        assertEquals ("XXX", CurrencyCodec.intToCode (CurrencyCodec.XXX));
        assertEquals ("USD", CurrencyCodec.intToCode (CurrencyCodec.USD));
        assertEquals ("197", CurrencyCodec.intToCode (197));

        roundTrip ("XXX");
        roundTrip ("usD");
        roundTrip ("AbC");
        roundTrip ("109");

        checkFail ("");
        checkFail ("A");
        checkFail ("1t6");
    }
}
