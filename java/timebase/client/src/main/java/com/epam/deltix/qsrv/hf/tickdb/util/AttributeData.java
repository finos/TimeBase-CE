package com.epam.deltix.qsrv.hf.tickdb.util;

import java.util.Arrays;

/**
 *
 */
public class AttributeData {

    // attribute values:
    //   first dimension - symbol index, second - attribute value as double
    public double[][]       values;

    // timestamps
    //   first dimension - symbol index, second - timestamp of the value in epoch time
    public long[][]         timestamps;

    @Override
    public String toString() {
        return "AttributeData{" +
            "values=" + Arrays.deepToString(values) +
            ", timestamps=" + Arrays.deepToString(timestamps) +
            '}';
    }
}
