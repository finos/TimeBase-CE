package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JStatement;

/**
 *
 */
public class QAConstant implements QAccessor {
    protected final JExpr reference;

    //protected final boolean isDotNetNullable = false;

    public QAConstant(JExpr reference) {
        this.reference = reference;
    }

    @Override
    public JExpr read() {
        return reference;
    }

    @Override
    public JStatement write(JExpr arg) {
        throw new UnsupportedOperationException("Write to constant");
    }

    @Override
    public String getFieldName() {
        throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public String getFieldDescription() {
        throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public JStatement writeNullify(JExpr expr){
        throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public String getSchemaFieldName () {
        throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public Class getFieldType () {
        throw new UnsupportedOperationException(getClass().getName());
    }
}
