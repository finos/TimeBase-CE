package com.epam.deltix.qsrv.hf.pub.codec.intp;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * Implementation, which uses Java-reflection
 */
public class JavaValueSetter implements ValueSetter {
    private final Field f;

    public JavaValueSetter(Field f) {
        this.f = f;
    }

    @Override
    public void setBoolean(Object obj, boolean value) throws IllegalAccessException {
        f.setBoolean(obj, value);
    }

    @Override
    public void setChar(Object obj, char value) throws IllegalAccessException {
        f.setChar(obj, value);
    }

    @Override
    public void setByte(Object obj, byte value) throws IllegalAccessException {
        f.setByte(obj, value);
    }

    @Override
    public void setShort(Object obj, short value) throws IllegalAccessException {
        f.setShort(obj, value);
    }

    @Override
    public void setInt(Object obj, int value) throws IllegalAccessException {
        f.setInt(obj, value);
    }

    @Override
    public void setLong(Object obj, long value) throws IllegalAccessException {
        f.setLong(obj, value);
    }

    @Override
    public void setFloat(Object obj, float value) throws IllegalAccessException {
        f.setFloat(obj, value);
    }

    @Override
    public void setDouble(Object obj, double value) throws IllegalAccessException {
        f.setDouble(obj, value);
    }

    @Override
    public void set(Object obj, Object value) throws IllegalAccessException {
        f.set(obj, value);
    }

    @Override
    public Object get(Object obj) throws IllegalAccessException, InvocationTargetException {
        return this.f.get(obj);
    }


}