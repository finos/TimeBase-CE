package com.epam.deltix.qsrv.hf.tickdb.comm;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class DefaultExceptionResolver implements ExceptionResolver {

    public static final DefaultExceptionResolver INSTANCE = new DefaultExceptionResolver();

    @Override
    public Throwable        create(Class<?> c, String message, Throwable cause) {
        Constructor<?> two = null;
        try {
            two = c.getConstructor(String.class, Throwable.class);
        } catch (NoSuchMethodException e) {
            // ignore
        }

        Constructor<?> one = null;
        try {
            one = c.getConstructor(String.class);
        } catch (NoSuchMethodException e) {
            // ignore
        }

        if (two == null && one == null)
            throw new IllegalStateException("Cannot find public constructors for the " + c.getName());

        Throwable x;
        try {
            if (cause != null && two != null)
                x = (Throwable) two.newInstance(message, cause);
            else
                x = (Throwable) one.newInstance(message);

        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        return x;
    }
}
