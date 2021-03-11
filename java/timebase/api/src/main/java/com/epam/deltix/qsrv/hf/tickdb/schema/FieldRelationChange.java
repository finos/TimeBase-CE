package com.epam.deltix.qsrv.hf.tickdb.schema;

import com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField;

public class FieldRelationChange extends FieldChange {

    protected FieldRelationChange() { } // for jaxb

    public FieldRelationChange(NonStaticDataField source, NonStaticDataField target) {
        super(source, target, FieldAttribute.Relation);
    }

    @Override
    public Impact getChangeImpact() {
        return Impact.DataLoss;
    }

    @Override
    public boolean hasErrors() {
        return false;
    }
}