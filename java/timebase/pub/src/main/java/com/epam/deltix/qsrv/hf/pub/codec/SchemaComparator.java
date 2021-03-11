package com.epam.deltix.qsrv.hf.pub.codec;

import com.epam.deltix.qsrv.hf.pub.md.*;

/**
 *  Utilities for comparing two record descriptors in order to determine
 *  whether messages serialized with one can be deserialized with another.
 */
public abstract class SchemaComparator {
    /**
     *  Detect only those changes that cause structural incompatibility
     *  between two schemas, ignoring name changes, and anything else
     *  unless it is <i>guaranteed</i> to make the schemas incompatible.
     */
    public static final long    DETECT_ONLY_STRUCTURAL_INCOMPATIBILITY = ~0L;

    /**
     *  Detect all changes that affect non-static fields, except annotations
     *  such as titles or primary keys (ignore nothing).
     */
    public static final long    DETECT_ALL_CHANGES = 0;

    /**
     *  Ignore changed field names, as long as their types and encodings
     *  remain compatible.
     */
    public static final long    IGNORE_FIELD_NAME_CHANGE =  1L << 0;

    /**
     *  Ignore changed class names, as long as their internal structure
     *  remains compatible.
     */
    public static final long    IGNORE_CLASS_NAME_CHANGE =  1L << 1;

    /**
     *  Ignore changed enum constant names.
     */
    public static final long    IGNORE_ENUM_NAME_CHANGE =   1L << 2;

    /**
     *  Ignore the extention of enumerated types.
     */
    public static final long    IGNORE_ENUM_EXTENSION =     1L << 3;

    /**
     *  Ignore the reduction of enumerated types.
     */
    public static final long    IGNORE_ENUM_REDUCTION =     1L << 4;

    /**
     *  Ignore the narrowing of numeric value ranges.
     */
    public static final long    IGNORE_RANGE_NARROWING =    1L << 5;

    /**
     *  Ignore changed bound class names.
     */
    public static final long    IGNORE_BINDING_CHANGE =     1L << 6;

    /**
     *  Ignore changes in class hierarchy, as long as all fields
     *  are in the same order.
     */
    public static final long    IGNORE_HIERARCHY_CHANGE =   1L << 7;
    
    public static class Listener {
        public void         numberOfFieldsChanged (int oldNum, int newNum) {            
        }

        public void         fieldTypeChanged (
            int                 idx,
            DataField           oldField, 
            DataField           newField
        )
        {            
        }
    }

    public static final Listener    DUMMY_LISTENER = new Listener ();

    /**
     *  Compare two record descriptors in order to determine whether messages
     *  serialized with one can be deserialized with another. Most changes
     *  make serialized data at least potentially incompatible.
     *  Exceptions include changed field titles or extended value ranges.
     *  Some changes make data completely incompatible, such as changed sequence
     *  of data types, or incompatible encoding changes.
     *  This utility always ignores static fields, because it is concerned with
     *  serialized data only. Also, this utility ignores the "primary key"
     *  attribute for the same reasons.
     *
     *  @param options      A bitwise combination of IGNORE_ options.
     *  @param old          Old record descriptor (presumably describing some
     *                      data that is already serialized).
     *  @param nw           New look of the same record descriptor, considered
     *                      for potential deserialization of the same data.
     *  @param listener     Gets notified about discrepancies. Can be null.
     * 
     *  @return             Whether any relevant changes have been observed,
     *                      except those ignored according to the specified
     *                      flags.
     */
    public static boolean       schemaChanged (
        long                        options,
        RecordClassDescriptor       old,
        RecordClassDescriptor       nw,
        Listener                    listener
    )
    {
        // Avoid null checks
        if (listener == null)
            listener = DUMMY_LISTENER;

        RecordLayout                oldLayout = new RecordLayout (old);
        RecordLayout                newLayout = new RecordLayout (nw);
        NonStaticFieldLayout []     oldFields = oldLayout.getNonStaticFields ();
        NonStaticFieldLayout []     newFields = newLayout.getNonStaticFields ();
        int                         numFields = oldFields.length;
        int                         newNumFields = newFields.length;
        boolean                     ret = false;
        
        if (numFields != newNumFields) {
            listener.numberOfFieldsChanged (numFields, newFields.length);
            ret = true;

            //  Try and compare fields anyway
            if (numFields > newNumFields)
                numFields = newNumFields;
        }

        for (int ii = 0; ii < numFields; ii++) {
            NonStaticFieldLayout    oldField = oldFields [ii];
            NonStaticFieldLayout    newField = newFields [ii];
            DataField               oldDataField = oldField.getField ();
            DataField               newDataField = newField.getField ();

            //  Check structure first - nothing is important if the structure
            //  is incompatible.
            DataType                oldType = oldField.getType ();
            DataType                newType = newField.getType ();

            if (oldType.getClass () != newType.getClass ()) { // TODO: encoding check
                listener.fieldTypeChanged (ii, oldDataField, newDataField);
                ret = true;
            }
/*
            if (old.)
            if ((options & IGNORE_HIERARCHY_CHANGE) == 0 &&
                oldField.getOwner ().
 */
        }

        return (ret);
    }

    /**
     *  Same as {@link #schemaChanged(long, deltix.qsrv.hf.pub.md.RecordClassDescriptor, deltix.qsrv.hf.pub.md.RecordClassDescriptor, deltix.qsrv.hf.pub.codec.SchemaComparator.Listener) }
     *  invoked with a null listener.
     */
    public static boolean       schemaChanged (
        long                        options,
        RecordClassDescriptor       old,
        RecordClassDescriptor       nw
    )
    {
        return (schemaChanged (options, old, nw, null));
    }

    /**
     *  Same as {@link #schemaChanged(long, deltix.qsrv.hf.pub.md.RecordClassDescriptor, deltix.qsrv.hf.pub.md.RecordClassDescriptor, deltix.qsrv.hf.pub.codec.SchemaComparator.Listener) }
     *  invoked with all ignore options, i.e. it will return true only if the
     *  schema change is guaranteed
     */
    public static boolean       schemaChanged (
        RecordClassDescriptor       old,
        RecordClassDescriptor       nw,
        Listener                    listener
    )
    {
        return (schemaChanged (0, old, nw, listener));
    }
}
