package com.epam.deltix.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;

/**
 * Class factory. Used by bound codecs.
 * <p>
 * An implementation is responsible for handling both user-defined and "pre-existent" types.
 * Delegate to <code>TypeLoaderImpl.DEFAULT_INSTANCE</code> handling of types you don't know about.
 * </p>
 *
 */
public interface TypeLoader {
    /** 
     * @return Class for type specified by given descriptor (never null).
     * Implementation of this function must return the same class for identical class descriptors (i.e. must be "pure function").
     * @throws ClassNotFoundException
     */
    public Class<?> load(ClassDescriptor cd) throws ClassNotFoundException;
}
