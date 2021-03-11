package com.epam.deltix.qsrv.hf.tickdb.schema;

import com.epam.deltix.qsrv.hf.pub.md.StaticDataField;

public class FieldValueChange extends FieldChange {

    protected FieldValueChange() { } // for jaxb

    public FieldValueChange(StaticDataField source, StaticDataField target) {
        super(source, target, FieldAttribute.StaticValue);
    }

    @Override
    public boolean hasErrors() {
        return false;
    }

    @Override
    public Impact getChangeImpact() {
        return Impact.None;
    }
}
