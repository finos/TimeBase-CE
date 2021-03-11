package com.epam.deltix.test.qsrv.hf.tickdb;

import org.junit.BeforeClass;

import java.io.IOException;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

/**
 * @author BazylevD
 * Tests interpreted codecs. See Test_Decoder4Truncated for details.</p>
 */
@Category(TickDBFast.class)
public class Test_Decoder4TruncatedIntp extends Test_Decoder4Truncated {

    @BeforeClass
    public static void setUpClass() throws IOException, InterruptedException {
        System.setProperty("use.codecs", "interpreted");
    }
}
