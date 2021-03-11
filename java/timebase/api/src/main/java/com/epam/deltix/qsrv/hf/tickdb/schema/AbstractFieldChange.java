package com.epam.deltix.qsrv.hf.tickdb.schema;

import com.epam.deltix.qsrv.hf.pub.md.*;

import javax.xml.bind.annotation.XmlElement;

public abstract class AbstractFieldChange implements SchemaChange {

    protected AbstractFieldChange() { } // for jaxb

    @XmlElement
    protected DataField source;
    @XmlElement
    protected DataField target;
    
    @XmlElement
    protected ErrorResolution   resolution;

    @XmlElement
    protected FieldAttribute    attribute;

    protected AbstractFieldChange(DataField source, DataField target) {
        this.source = source;
        this.target = target;
    }

    protected AbstractFieldChange(DataField source, DataField target, FieldAttribute attr) {
        this.source = source;
        this.target = target;
        this.attribute = attr;
    }

    public DataField        getSource() {
        return source;
    }

    public DataField        getTarget() {
        return target;
    }

    public FieldAttribute   getAttribute() {
        return attribute;
    }

    public void             setAttribute(FieldAttribute attribute) {
        this.attribute = attribute;
    }

    public abstract boolean hasErrors();

    public ErrorResolution  getResolution() {
        return resolution;
    }

    public static String valueOf(DataField field, FieldAttribute attr) {

        switch (attr) {
            case Title:
                return field.getTitle();
            case Name:
                return field.getName();
            case Description:
                return field.getDescription();
            case DataType:
                return field.getType().getEncoding();
            case PrimaryKey:
                return String.valueOf(((NonStaticDataField) field).isPk());
            case StaticValue:
                return ((StaticDataField)field).getStaticValue();
            case Relation:
                return ((NonStaticDataField)field).getRelativeTo();

            default:
                throw new IllegalArgumentException("Undefined Field Attribute: " + attr);

        }
    }

}
