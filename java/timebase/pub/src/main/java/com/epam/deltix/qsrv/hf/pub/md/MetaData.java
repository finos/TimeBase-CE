package com.epam.deltix.qsrv.hf.pub.md;

/**
 *
 */
public interface MetaData<T extends ClassDescriptor> extends ClassSet<T> {

    ClassDescriptor                 getClassDescriptor (String name);
    void                            setClassDescriptors (ClassDescriptor ... cd);
    ClassDescriptor []              getClassDescriptors ();
    
    ClassDescriptor []              selectClassDescriptors (
            int                                     options,
            String                                  namePattern
        );
}