package com.epam.deltix.test.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.pub.ExchangeCodec;
import com.epam.deltix.qsrv.hf.pub.codec.AlphanumericCodec;
import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.lang.Util;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Andy
 *         Date: Nov 10, 2009 11:37:35 AM
 */
@Category(JUnitCategories.TickDBCodecs.class)
@RunWith(Theories.class)
public class Test_ExchangeCodec {

    @DataPoint public static String ABCDEFGHIJ = "ABCDEFGHIJ";
    @DataPoint public static String DIGITS = "0123456789";
    @DataPoint public static String CHARS =  "!\\\"#$%&'()";

    @DataPoint public static String NYSE = "NYSE";
    @DataPoint public static String A = "A";
    @DataPoint public static String P = "P";
    @DataPoint public static String Z = "Z";
    @DataPoint public static String UQFD_UTDF = "UQFD/UTDF";
    @DataPoint public static String CQS_CTS = "CQS/CTS";
    @DataPoint public static String EXCLAIM = " ! ";
    private static final int MIN_CHAR = 0x20;
    private static final int MAX_CHAR = 0x5F;

    private final AlphanumericCodec codec = new AlphanumericCodec(ExchangeCodec.MAX_LEN);

    @Theory
    public void exchangeField (String exchange) {
       Assert.assertTrue("exchange is too large", exchange.length() <= 10);

       for (char ch : exchange.toCharArray()) {
           Assert.assertTrue("illegal char: " + ch, ch >= MIN_CHAR);
           Assert.assertTrue("illegal char: " + ch, ch <= MAX_CHAR);
       }

        assertIsomorphic (exchange);
     }

    @Test
    public void testLongMixedCase () {
        try {
            long exchangeCode = ExchangeCodec.codeToLong("Bloomberg_TradeBook");
            String exchange = ExchangeCodec.longToCode(exchangeCode);
            fail ("Expected java.lang.IllegalArgumentException: 'Bloomberg_TradeBook' is longer then 10");
        } catch (IllegalArgumentException expected) {

        }

        long exchangeCode = ExchangeCodec.codeToLongSafely("Bloomberg_TradeBook");
        assertEquals ("BLOOMBERG_", ExchangeCodec.longToCode(exchangeCode));
    }

    private void assertIsomorphic(String original) {
        long encoded1 = codec.encodeToLong(original);
        Assert.assertTrue("encoded as zero", encoded1 != 0);

        long encoded2 = codec.encodeToLong(original);
        assertEquals("orignal was encoded as two different numbers", encoded1, encoded2);


        CharSequence decoded = codec.decodeFromLong(encoded1);
        if ( ! Util.equals(original, decoded)) {
            //long encoded3 = ExchangeCodec.codeToLong(original);
            //Assert.assertEquals("orignal was encoded as two different numbers", encoded1, encoded3);

            fail("Reconstructed string [" + decoded + "] is not identical to original: [" + original + ']');
        }
    }
                                               

    @Test
    public void testAllCombinations () {
        testAllCombinations(4);
    }

    private void testAllCombinations(int maxLength) {
        char [] buffer = new char [maxLength];

        for (int len =1; len <= maxLength; len++)
            tryCombinations (buffer, 0, len);
    }

    private void tryCombinations(char[] buffer, int pos, int length) {

        boolean complete = (pos == length - 1);

        for (char ci = MIN_CHAR; ci <= MAX_CHAR; ci++) {
            buffer[pos] = ci;

            if (complete)
                assertIsomorphic (new String (buffer, 0, length));
            else
                tryCombinations(buffer, pos+1, length);
            
        }
    }
}
