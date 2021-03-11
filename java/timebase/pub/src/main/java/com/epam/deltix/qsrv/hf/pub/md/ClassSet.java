package com.epam.deltix.qsrv.hf.pub.md;

public interface ClassSet<T extends ClassDescriptor> {

    @SuppressWarnings(value = {"unchecked", "varargs"})
    void                    addContentClasses (T ...  cds);

    T[]                     getContentClasses ();

    ClassDescriptor[]       getClasses();
}