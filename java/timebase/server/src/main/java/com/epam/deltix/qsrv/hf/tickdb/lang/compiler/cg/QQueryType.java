package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.util.jcg.JStatement;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.*;

/**
 *
 */
//TMP - extends Primitive. Need to clean up all.
public class QQueryType extends QType <QueryDataType> {
    public QQueryType (QueryDataType dt) {
        super (dt);
    }

    @Override
    public Class <?>    getJavaClass () {
        return (InstrumentMessageSource.class);
    }

    @Override
    public JExpr        getNullLiteral () {
        return (CTXT.nullLiteral ());
    }

    @Override
    public int          getEncodedFixedSize () {
        throw new UnsupportedOperationException ("Not serializable.");
    }

    @Override
    public JStatement   decode (JExpr input, QValue value) {
        throw new UnsupportedOperationException ("Not serializable.");
    }

    @Override
    public void         encode (QValue value, JExpr output, JCompoundStatement addTo) {
        throw new UnsupportedOperationException ("Not serializable.");
    }        
}
