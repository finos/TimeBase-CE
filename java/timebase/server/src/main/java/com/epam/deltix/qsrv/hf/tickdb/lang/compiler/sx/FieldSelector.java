package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.DataFieldRef;

/**
 *
 */
public class FieldSelector extends CompiledExpression <DataType> {
    public final DataFieldRef          fieldRef;

    public FieldSelector (DataFieldRef fieldRef) {
        super (fieldRef.field.getType ().nullableInstance (true));
        this.fieldRef = fieldRef;
        this.name = fieldRef.field.getName ();
    }

    @Override
    protected void                  print (StringBuilder out) {
        out.append (fieldRef.field.getName ());
    }

    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return
            super.equals (obj) &&
            fieldRef.equals (((FieldSelector) obj).fieldRef);
    }

    @Override
    public int                      hashCode () {
        return super.hashCode () + fieldRef.hashCode ();
    }


}
