package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.lang.Util;

/**
 *
 */
public class EnumValueRef {
    public final EnumClassDescriptor       parent;
    public final EnumValue                 field;

    public EnumValueRef (EnumClassDescriptor parent, EnumValue field) {
        this.parent = parent;
        this.field = field;
    }

    @Override
    public boolean          equals (Object obj) {
        if (this == obj)
            return (true);

        if (!(obj instanceof EnumValueRef))
            return (false);

        final EnumValueRef      other = (EnumValueRef) obj;

        return (parent.equals (other.parent) && Util.xequals (field.symbol, other.field.symbol));
    }

    @Override
    public int              hashCode () {
        return (parent.hashCode () + Util.hashCode (field.symbol));
    }
}
