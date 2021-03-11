package com.epam.deltix.qsrv.hf.pub.codec.intp;

import java.lang.reflect.InvocationTargetException;

/**
 * Generic interface having setter methods for all major unboxed types
 */
public interface ValueSetter {

    void setBoolean(Object obj, boolean value) throws IllegalAccessException, InvocationTargetException;

    void setChar(Object obj, char value) throws IllegalAccessException, InvocationTargetException;

    void setByte(Object obj, byte value) throws IllegalAccessException, InvocationTargetException;

    void setShort(Object obj, short value) throws IllegalAccessException, InvocationTargetException;

    void setInt(Object obj, int value) throws IllegalAccessException, InvocationTargetException;

    void setLong(Object obj, long value) throws IllegalAccessException, InvocationTargetException;

    void setFloat(Object obj, float value) throws IllegalAccessException, InvocationTargetException;

    void setDouble(Object obj, double value) throws IllegalAccessException, InvocationTargetException;

    void set(Object obj, Object value) throws IllegalAccessException, InvocationTargetException;

    Object get(Object ogj) throws IllegalAccessException, InvocationTargetException;
}
