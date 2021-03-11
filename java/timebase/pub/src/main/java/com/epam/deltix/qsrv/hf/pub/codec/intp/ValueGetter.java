package com.epam.deltix.qsrv.hf.pub.codec.intp;

import java.lang.reflect.InvocationTargetException;

/**
 * Generic interface having getter methods for all major unboxed types
 */
public interface ValueGetter {

    boolean getBoolean(Object obj) throws IllegalAccessException, InvocationTargetException;

    char getChar(Object obj) throws IllegalAccessException, InvocationTargetException;

    byte getByte(Object obj) throws IllegalAccessException, InvocationTargetException;

    short getShort(Object obj) throws IllegalAccessException, InvocationTargetException;

    int getInt(Object obj) throws IllegalAccessException, InvocationTargetException;

    long getLong(Object obj) throws IllegalAccessException, InvocationTargetException;

    float getFloat(Object obj) throws IllegalAccessException, InvocationTargetException;

    double getDouble(Object obj) throws IllegalAccessException, InvocationTargetException;

    Object get(Object obj) throws IllegalAccessException, InvocationTargetException;
}

