package com.epam.deltix.qsrv.hf.tickdb.tests.reports;

import java.util.Collection;

public interface Metric<T> {

    Metric<T> addValue(T value);

    String valueToString(T value);

    Collection<T> values();

}
