package com.epam.deltix.qsrv.hf.tickdb.schema;

import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.md.StaticDataField;

import javax.xml.bind.annotation.XmlElement;

public class CreateFieldChange extends AbstractFieldChange {

    @XmlElement
    private boolean hasImpact = false;

    protected CreateFieldChange() {} // for jaxb

    public CreateFieldChange(StaticDataField field) {
        this(field, false);
    }

    public CreateFieldChange(DataField field, boolean hasImpact) {
        super(null, field);
        this.hasImpact = hasImpact;
    }

    public Impact getChangeImpact() {
        if (getTarget() instanceof StaticDataField)
            return Impact.None;

        if (getInitialValue() == null)
            return getTarget().getType().isNullable() && !hasImpact ? Impact.None : Impact.DataConvert;
        
        return Impact.DataConvert;
    }

    public boolean hasErrors() {
        if (!getTarget().getType().isNullable())
            return getInitialValue() == null;

        return resolution == null;
    }

    public void setInitialValue(String value) {
        this.resolution = ErrorResolution.resolve(value);
    }

    public String getInitialValue() {
        return resolution != null ? resolution.defaultValue : null;
    }
}
