package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.codec.cg.ObjectPool;
import com.epam.deltix.util.lang.Util;

/**
 *
 */
public class ReflectionObjectPool extends ObjectPool<Object> {
    private final Class<?> clazz;

    public ReflectionObjectPool(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Object newItem() {
        return Util.newInstanceNoX(clazz);
    }
}
