package com.epam.deltix.test.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.JUnitCategories;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.epam.deltix.qsrv.hf.pub.md.DateTimeDataType.ENCODING_MILLISECONDS;
import static com.epam.deltix.qsrv.hf.pub.md.DateTimeDataType.ENCODING_NANOSECONDS;
import static org.junit.Assert.assertEquals;


/**
 *
 */
@Category(JUnitCategories.TickDBCodecs.class)
public class Test_DateTimeDataType {


    @Test
    public void testNullEncoding() {
        DataType dt = new DateTimeDataType(false, null);

        assertEquals(ENCODING_MILLISECONDS, dt.getEncoding());
    }

    @Test
    public void testEmptyEncoding() {
        DataType dt = new DateTimeDataType(false, "");

        assertEquals(ENCODING_MILLISECONDS, dt.getEncoding());
    }

    @Test
    public void testMsConvertedEncoding() {
        DataType dt = new DateTimeDataType(false, "ms");

        assertEquals(ENCODING_MILLISECONDS, dt.getEncoding());
    }

    @Test
    public void testNanosConvertedEncoding() {
        DataType dt = new DateTimeDataType(false, "ns");

        assertEquals(ENCODING_NANOSECONDS, dt.getEncoding());
    }
}
