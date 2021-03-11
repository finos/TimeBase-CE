package com.epam.deltix.qsrv.hf.stream;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.time.Interval;

/**
 * Message file metadata container.
 */
public class MessageFileHeader {

    private final ClassSet              classes;
    public final int                    version;
    public Interval                     periodicity;

    public MessageFileHeader(int version, ClassSet set, Interval periodicity) {
        this.version = version;
        this.classes = set;
        this.periodicity = periodicity;
    }

    public MessageFileHeader(int version, RecordClassDescriptor[] descriptors, Interval periodicity) {
        this.version = version;
        this.periodicity = periodicity;
        (this.classes = new ClassSet()).addContentClasses(descriptors);
    }

    public static MessageFileHeader                         migrate(MessageFileHeader header) {
        return header;
//        try {
//            return new MessageFileHeader(header.version, new SchemaUpdater(new ClassMappings()).update(header.classes), header.periodicity);
//        } catch (ClassNotFoundException | Introspector.IntrospectionException e) {
//            throw new RuntimeException(e);
//        }
    }

    public RecordClassDescriptor[]      getTypes() {
        return classes.getContentClasses();
    }
    
    public ClassDescriptor[]            getAllTypes() {
        return classes.getClasses();
    }
}
