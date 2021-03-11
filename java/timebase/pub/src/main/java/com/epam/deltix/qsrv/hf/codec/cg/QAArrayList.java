package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JStatement;

/**
 *
 */
public class QAArrayList implements QAccessor {
    protected final JExpr reference;
    protected final JExpr argIdx;
    protected final String boxedType;
    private final String fieldName;
    private final String schemaFieldName;

    public QAArrayList(JExpr reference, JExpr argIdx, String boxedType, String fieldName, String schemaFieldName) {
        this.reference = reference;
        this.argIdx = argIdx;
        this.boxedType = boxedType;
        this.fieldName = fieldName;
        this.schemaFieldName = schemaFieldName;
    }

    @Override
    public JExpr read() {
        return reference.call("get" + boxedType, argIdx);
    }

    @Override
    public JStatement write(JExpr arg) {
        return reference.call("set", argIdx, arg).asStmt();
    }

    @Override
    public String getFieldName() {
        return fieldName;
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
        return schemaFieldName;
    }

    @Override
    public Class getFieldType () {
        throw new UnsupportedOperationException(getClass().getName());
    }
}
