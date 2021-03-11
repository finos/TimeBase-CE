package com.epam.deltix.qsrv.hf.tickdb.schema;

import com.epam.deltix.qsrv.hf.pub.md.StaticDataField;

public class StaticFieldChange extends FieldTypeChange {

    protected StaticFieldChange() { } // for jaxb

    public StaticFieldChange(StaticDataField source, StaticDataField target) {
        super(source, target);
    }

    public Impact getChangeImpact() {
        return Impact.None;
    }

    public boolean hasErrors() {
        return false;
    }

    public boolean isDefaultValueRequired() {
        return false;
    }
}