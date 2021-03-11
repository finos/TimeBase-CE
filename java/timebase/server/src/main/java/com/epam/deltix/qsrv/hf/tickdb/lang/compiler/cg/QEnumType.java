package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.qsrv.hf.pub.md.EnumDataType;
import com.epam.deltix.util.jcg.*;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

public class QEnumType extends QType <EnumDataType> {
    public static final JExpr       NULL = CTXT.intLiteral (-1);
    
    public QEnumType (EnumDataType dt) {
        super(dt);
    }

    @Override
    public int          getEncodedFixedSize() {
        return dt.descriptor.computeStorageSize ();
    }

    @Override
    public Class <?>    getJavaClass() {
        final int size = getEncodedFixedSize ();
        
        switch (size) {
            case 1: return byte.class;                
            case 2: return short.class;
            case 4: return int.class;
            case 8: return long.class;
            default: throw new IllegalStateException ("unexpected size " + size);
        }
    }

    @Override
    public JExpr        getNullLiteral() {
        return NULL;
    }

    @Override
    protected void      encodeNullImpl (JExpr output, JCompoundStatement addTo) {
        addTo.add (
            output.call (
                QIntegerType.getEncodeMethod (getEncodedFixedSize ()), 
                NULL
            )
        );
    }

    @Override
    protected JExpr     makeConstantExpr(Object obj) {
        return (CTXT.longLiteral (((Number) obj).longValue ()).cast (getJavaClass ()));        
    }

    @Override
    public JStatement   decode (JExpr input, QValue value) {
        return (
            value.write (
                input.call (QIntegerType.getDecodeMethod (getEncodedFixedSize ()))
            )
        );
    }

    @Override
    public void         encode (QValue value, JExpr output, JCompoundStatement addTo) {
        addTo.add (
            output.call (
                QIntegerType.getEncodeMethod (getEncodedFixedSize ()), 
                value.read ()
            )
        );
    }        
}
