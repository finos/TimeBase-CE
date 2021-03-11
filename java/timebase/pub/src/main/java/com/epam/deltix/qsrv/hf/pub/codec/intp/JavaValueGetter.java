package com.epam.deltix.qsrv.hf.pub.codec.intp;

import java.lang.reflect.Field;

/**
 * Implementation, which uses Java-reflection
 */
public class JavaValueGetter implements ValueGetter {
    private final Field f;

    public JavaValueGetter(Field f) {
        this.f = f;
    }

    @Override
    public boolean getBoolean(Object obj) throws IllegalAccessException {
        return f.getBoolean(obj);
    }

    @Override
    public char getChar(Object obj) throws IllegalAccessException {
        return f.getChar(obj);
    }

    @Override
    public byte getByte(Object obj) throws IllegalAccessException {
        return f.getByte(obj);
    }

    @Override
    public short getShort(Object obj) throws IllegalAccessException {
        return f.getShort(obj);
    }

    @Override
    public int getInt(Object obj) throws IllegalAccessException {
        return f.getInt(obj);
    }

    @Override
    public long getLong(Object obj) throws IllegalAccessException {
        return f.getLong(obj);
    }

    @Override
    public float getFloat(Object obj) throws IllegalAccessException {
        return f.getFloat(obj);
    }

    @Override
    public double getDouble(Object obj) throws IllegalAccessException {
        return f.getDouble(obj);
    }

    @Override
    public Object get(Object obj) throws IllegalAccessException {
        return f.get(obj);
    }
}
