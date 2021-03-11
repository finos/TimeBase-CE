package com.epam.deltix.qsrv.hf.tickdb.schema;

import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundEncoder;

public class FieldPositionChange extends AbstractFieldChange {

    protected FieldPositionChange() { } // for JAXB

    public FieldPositionChange(DataField source, DataField target) {
        super(source, target);
    }

    public boolean hasErrors() {
        return false;
    }

    public Impact getChangeImpact() {
        return Impact.DataConvert;
    }

    @Override
    public String toString() {
        return "Field " + source.getName() + " position changed.";
    }
}
