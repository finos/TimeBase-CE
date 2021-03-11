package com.epam.deltix.qsrv.hf.pub;

/**
 * User-defined value is out of required range.
 */
public class ValueOutOfRangeException extends RuntimeException {
    public final Object     value;
    public final Object     min;
    public final Object     max;

    public ValueOutOfRangeException (Object value, Object min, Object max) {
        super (
            value + " is out of allowed range: [" +
            (min == null ? "" : min) + "src/main " +
            (max == null ? "" : max) + "]"
        );

        this.value = value;
        this.min = min;
        this.max = max;
    }
}
