package com.epam.deltix.test.qsrv.hf.tickdb;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

@Category(TickDBFast.class)
public class Test_ReverseCursorMax extends Test_ReverseCursor {
    static {
        distribution_factor = 0;
    }
}
