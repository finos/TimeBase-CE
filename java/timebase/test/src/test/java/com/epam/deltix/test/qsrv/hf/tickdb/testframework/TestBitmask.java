package com.epam.deltix.test.qsrv.hf.tickdb.testframework;

import com.epam.deltix.timebase.messages.Bitmask;
import com.epam.deltix.timebase.messages.SchemaElement;

/**
 *
 */
@Bitmask
@SchemaElement(
        title = "Bitmask Test Type"
)
public enum TestBitmask {
    BIT0,
    BIT1,
    BIT2,
    BIT3
}
