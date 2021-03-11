package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

/**
 *
 */
public final class FieldAccessExpression extends Expression {
    public final TypeIdentifier     typeId;
    public final FieldIdentifier    fieldId;

    public FieldAccessExpression (long location, TypeIdentifier typeId, FieldIdentifier fieldId) {
        super (location);
        this.typeId = typeId;
        this.fieldId = fieldId;
    }

    public FieldAccessExpression (TypeIdentifier typeId, FieldIdentifier fieldId) {
        this (NO_LOCATION, typeId, fieldId);
    }

    @Override
    protected void              print (int outerPriority, StringBuilder s) {
        typeId.print (s);
        s.append (":");
        fieldId.print (s);
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            typeId.equals (((FieldAccessExpression) obj).typeId) &&
            fieldId.equals (((FieldAccessExpression) obj).fieldId)
        );
    }

    @Override
    public int                      hashCode () {
        return ((super.hashCode () * 41 + typeId.hashCode ()) * 31 + fieldId.hashCode ());
    }
}
