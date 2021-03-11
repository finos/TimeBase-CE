package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

/**
 *
 */
public class QBDateTimeType extends QBoundType<QDateTimeType> {

    public QBDateTimeType(QDateTimeType qType, Class<?> javaType, QAccessor accessor) {
        super(qType, javaType, accessor);

        if (javaBaseType != long.class)
            throw new IllegalArgumentException("invalid javaType " + javaBaseType);
    }

    @Override
    protected JExpr getNullLiteralImpl() {
        return super.getNullLiteralImpl();
    }

    public void decode(JExpr input, JCompoundStatement addTo) {
        super.decode(input, addTo);
    }

    public void encode(JExpr output, JCompoundStatement addTo) {
         super.encode(output, addTo);
    }


    @Override
    protected JExpr makeConstantExpr(Object obj) {
        return  CTXT.longLiteral(((Number) obj).longValue());
    }

}
