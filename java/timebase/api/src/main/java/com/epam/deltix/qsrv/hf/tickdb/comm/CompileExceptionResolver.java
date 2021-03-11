package com.epam.deltix.qsrv.hf.tickdb.comm;

import com.epam.deltix.util.parsers.CompilationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class CompileExceptionResolver extends DefaultExceptionResolver {

    private long errorLocation;

    public CompileExceptionResolver(long errorLocation) {
        this.errorLocation = errorLocation;
    }

    @Override
    public Throwable create(Class<?> clazz, String message, Throwable cause) {

        if (CompilationException.class.isAssignableFrom(clazz)) {
            Constructor<?> c = null;
            try {
                c = clazz.getConstructor(String.class, long.class);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }

            Throwable x;
            try {
               x = (Throwable) c.newInstance(message, errorLocation);

            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }

            return x;
        }

        return super.create(clazz, message, cause);
    }
}
