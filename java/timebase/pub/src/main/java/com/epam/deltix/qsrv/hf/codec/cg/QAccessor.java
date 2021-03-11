package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JStatement;

/**
 * Provides read/write methods to access Java variable, constant, parameter or object field,
 * which {@link QValue} is bound to.
 * <p>
 * In the moment it is used only for QBoundType implementations, which in turn are used in QPrimitiveValue
 * </p>
 */
public interface QAccessor {

    public JExpr read();

    public JStatement write(JExpr arg);

    public String getFieldDescription();

    public String getFieldName();

    public JStatement writeNullify(JExpr nullExpr);

    public String getSchemaFieldName();

    public Class getFieldType();
}
