package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.dfp.Decimal64;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

/**
 * Bound Float DataType
 */
public class QBFloatType extends QBNumericType<QFloatType> {

    public QBFloatType(QFloatType qType, Class<?> javaType, QAccessor accessor) {
        super(qType, javaType, accessor);
    }

    @Override
    protected JExpr readIsNullImpl(boolean eq) {
        final JExpr value = accessor.read();
        JExpr checkNull;

        if (javaBaseType == float.class)
            checkNull = CTXT.staticCall (Float.class, "isNaN", value);
        else if (javaBaseType == double.class)
            checkNull = CTXT.staticCall (Double.class, "isNaN", value);
        else if (javaBaseType == long.class)
            checkNull = CTXT.staticCall (Decimal64Utils.class, "isNaN", value);
        else if (javaBaseType == Decimal64.class)
            //checkNull = CTXT.binExpr(CTXT.binExpr(value, " == ", getNullLiteral()), " || ", value.call("isNaN"));
            checkNull = CTXT.binExpr(value, " == ", getNullLiteral());
        else
            throw new IllegalArgumentException("unknown java type:" + javaBaseType);

        return eq ? checkNull : checkNull.not();
    }

    public void decodeRelative(JExpr input, QBoundType base, JExpr isBaseNull, JCompoundStatement addTo) {
        final JExpr readExpr = qType.decodeExpr(input);

        addTo.add(
                accessor.write(
                        CTXT.condExpr(
                                isBaseNull != null ? isBaseNull : base.readIsNull(true),
                                readExpr,
                                CTXT.binExpr(readExpr, "+", base.accessor.read()))
                )
        );
    }



    @Override
    public JExpr                getNullLiteral () {
        if (javaBaseType == float.class)
            return (CTXT.staticVarRef (Float.class, "NaN"));
        else if (javaBaseType == double.class)
            return (CTXT.staticVarRef (Double.class, "NaN"));
        else if (javaBaseType == long.class)
            return (CTXT.staticVarRef (Decimal64Utils.class, "NaN"));
        else if (javaBaseType == Decimal64.class)
            return CTXT.nullLiteral();

        throw new RuntimeException ("unknown bound type = " + javaBaseType);

    }

    @Override
    public void decode(JExpr input, JCompoundStatement addTo) {
        if (javaBaseType == long.class) {
            addTo.add(
                    accessor.write(input.call("readLong"))
            );
        } else if (javaBaseType == Decimal64.class) {
            addTo.add(
                    accessor.write(CTXT.staticCall (Decimal64.class, "fromUnderlying", input.call("readLong")))
            );
        } else {
            addTo.add(accessor.write(qType.decodeExpr(input)));
        }
    }

    @Override
    public void encode(JExpr output, JCompoundStatement addTo) {
        if (javaBaseType == long.class) {
            JExpr expr = output.call("writeLong", getEncodeValue(qType.getNullLiteral()));
            addTo.add(expr);
        } else {
            qType.encodeExpr(output, getEncodeValue(qType.getNullLiteral()), addTo);
        }
    }

    public void encodeRelative(QBoundType base, JExpr isBaseNull, JExpr output, JCompoundStatement addTo) {
        final JExpr rawValue;
        final JExpr augmentedValue;
        rawValue = accessor.read();
        augmentedValue = CTXT.binExpr(accessor.read(), "-", base.accessor.read());

        final JExpr value = CTXT.condExpr(CTXT.binExpr(isBaseNull, "||", readIsNull(true)),
                rawValue, augmentedValue
        );

        qType.encodeExpr(output, value, addTo);
    }

}
