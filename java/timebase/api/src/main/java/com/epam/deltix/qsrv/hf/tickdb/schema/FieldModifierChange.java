package com.epam.deltix.qsrv.hf.tickdb.schema;

import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.md.StaticDataField;

import javax.xml.bind.annotation.XmlElement;

/**
 Indicates static to non-static change and visa-versa
 */
public class FieldModifierChange extends AbstractFieldChange {

    protected FieldModifierChange() { } // for jaxb

    @XmlElement
    private CreateFieldChange create;

    @XmlElement
    private DeleteFieldChange delete;
    
    public FieldModifierChange(DataField source,
                               DataField target, boolean tHasImpact) {
        super(source, target);
        this.create = new CreateFieldChange(target, tHasImpact);
        this.delete = new DeleteFieldChange(source);
    }

    public Impact getChangeImpact() {
        if (source instanceof StaticDataField)
            return create.getChangeImpact();
        else
            return delete.getChangeImpact();
    }

    public boolean hasErrors() {
       return create.hasErrors();
    }

    public void setInitialValue(String value) {
        create.setInitialValue(value);
        resolution = create.resolution;
    }

    public String getInitialValue() {
        return create.getInitialValue();
    }
}
