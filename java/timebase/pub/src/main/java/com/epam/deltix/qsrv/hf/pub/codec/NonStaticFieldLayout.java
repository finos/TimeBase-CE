package com.epam.deltix.qsrv.hf.pub.codec;

import com.epam.deltix.qsrv.hf.pub.md.*;

/**
 *
 */
public class NonStaticFieldLayout
    extends FieldLayout <NonStaticDataField> 
    implements NonStaticFieldInfo
{
    int                                 fixedOffset = RecordLayout.VARIABLE_SIZE;
    int                                 ordinal = -1;

    public int                          ownBaseIndex = -1;
    public NonStaticFieldLayout         relativeTo = null;

    public NonStaticFieldLayout (NonStaticDataField field) {
        super (null, field);
    }

    public NonStaticFieldLayout (RecordClassDescriptor owner, NonStaticDataField field) {
        super (owner, field);
    }

    public NonStaticFieldLayout (NonStaticFieldLayout parent, NonStaticDataField field) {
        super (parent.getOwner(), field);
        this.fieldType = parent.getGenericClass();
    }

    public int                          getOffset () {
        if (fixedOffset < 0)
            throw new IllegalStateException (this + " is not randomly accessible");
        
        return (fixedOffset);
    }

    public int                          getOrdinal () {
        return (ordinal);
    }

    public boolean                      isPrimaryKey () {
        return (getField ().isPk ());
    }
    
    // getters for Velocity template

    public int getOwnBaseIndex() {
        return ownBaseIndex;
    }

    public NonStaticFieldLayout getRelativeTo() {
        return relativeTo;
    }
}
