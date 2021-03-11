package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.qsrv.hf.pub.md.TimeOfDayDataType;
import com.epam.deltix.util.jcg.*;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

public class QTimeOfDayType extends QType <TimeOfDayDataType> {
    public QTimeOfDayType (TimeOfDayDataType dt) {
        super(dt);
    }

    @Override
    public JStatement   decode (JExpr input, QValue value) {
        return (value.write (input.call ("readInt")));        
    }

    @Override
    public void         encode (QValue value, JExpr output, JCompoundStatement addTo) {
        addTo.add (output.call("writeInt", value.read ()));
    }
   
    @Override
    public Class <?>    getJavaClass() {
        return int.class;
    }

    @Override
    public JExpr        getNullLiteral () {
        return CTXT.staticVarRef (TimeOfDayDataType.class, "NULL");
    }

    @Override
    public JExpr        makeConstantExpr (Object obj) {
        return CTXT.intLiteral (((Number) obj).intValue ());
    }

    @Override
    protected void      encodeNullImpl (JExpr output, JCompoundStatement addTo) {
        addTo.add (output.call ("writeInt", getNullLiteral ()));
    }

    @Override
    public int          getEncodedFixedSize() {
        return 4;
    }
}
