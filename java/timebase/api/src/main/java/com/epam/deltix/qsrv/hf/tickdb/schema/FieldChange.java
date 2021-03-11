package com.epam.deltix.qsrv.hf.tickdb.schema;

import com.epam.deltix.qsrv.hf.pub.md.DataField;

public class FieldChange extends AbstractFieldChange {

    public FieldChange() { } // for JAXB

    public FieldChange(DataField source, DataField target, FieldAttribute attr) {
        super(source, target, attr);
    }

    @Override
    public boolean          hasErrors() {
        return false;
    }

    @Override
    public Impact           getChangeImpact() {
        return Impact.None;
    }

    @Override
    public String toString() {
        return "\"" + attribute.toString() + "\" changed from [" +
                valueOf(source, attribute) + "] to [" + valueOf(target, attribute) + "]";
    }
}
