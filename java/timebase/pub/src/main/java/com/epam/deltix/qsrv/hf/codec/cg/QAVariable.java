package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JStatement;

import java.lang.reflect.Field;

/**
 * Access to a local variable or a public field
 */
public class QAVariable extends QAConstant {
    private final String fieldName;
    private final String fieldDescription;
    private final String schemaFieldName;
    private final Field field;

    public QAVariable(JExpr reference, Field field, String schemaFieldName) {
        super(reference);
        this.field = field;
        this.fieldName = field.getName();
        this.fieldDescription = field.toString();
        this.schemaFieldName = schemaFieldName;
    }

    @Override
    public JStatement write(JExpr arg) {
        return reference.assign(arg);
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public String getSchemaFieldName () {
        return schemaFieldName;
    }

    @Override
    public String getFieldDescription() {
        return fieldDescription;
    }

    @Override
    public Class getFieldType () {
        // using for cast in set static value
        return field.getType();
    }
}
