package com.epam.deltix.util.security;

import com.epam.deltix.util.lang.Disposable;

public interface DataFilter<T> extends Disposable {

    boolean accept(T obj);
}