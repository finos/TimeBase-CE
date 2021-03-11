package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

/**
 *
 */
public abstract class QPrimitiveType<T extends DataType> extends QType<T> {
    protected QPrimitiveType(T dt) {
        super(dt);
    }

    protected void encodeExpr(JExpr output, JExpr value, JCompoundStatement addTo) {
        throw new UnsupportedOperationException("Not implemented for " + getClass ().getSimpleName ());
    }

    protected JExpr decodeExpr(JExpr input) {
        throw new UnsupportedOperationException("Not implemented for " + getClass ().getSimpleName ());
    }

    public abstract Class <?>   getJavaClass ();

    /**
     *
     * @return constant, which is the canonical NULL-value for the given DataType
     */
    protected abstract JExpr getNullLiteral();

    public JExpr                checkNull (JExpr e, boolean eq) {
        return (CTXT.binExpr (e, eq ? "==" : "!=", getNullLiteral ()));
    }

    @Override
    protected void encodeNullImpl(JExpr output, JCompoundStatement addTo) {
        encodeExpr(output, getNullLiteral(), addTo);
    }
}
