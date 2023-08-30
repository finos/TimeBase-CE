/*
 * Copyright 2023 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JStatement;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

/**
 *
 */
public final class QFloatType extends QNumericType <FloatDataType> {
    static final int KIND_FLOAT = 4;
    static final int KIND_DOUBLE = 8;

    private static int getKind (FloatDataType dt) {
        return (dt.isFloat () ? KIND_FLOAT : KIND_DOUBLE);
    }

    public QFloatType (FloatDataType dt) {
        super (dt, getKind (dt), dt.getMinNotNull (), dt.getMaxNotNull ());
    }

    public boolean              isFloat () {
        return (kind == KIND_FLOAT);
    }

    @Override
    public Class <?>            getJavaClass () {
        if (dt.getScale() == FloatDataType.SCALE_DECIMAL64) {
            return long.class;
        }
        switch (dt.getScale ()) {
            case FloatDataType.FIXED_FLOAT:
                return float.class;
            case FloatDataType.SCALE_DECIMAL64:
                return long.class;
            case FloatDataType.FIXED_DOUBLE:
            default:
                return double.class;
        }
    }

    @Override
    public JExpr                getLiteral (Number value) {
        switch (dt.getScale()) {
            case FloatDataType.FIXED_FLOAT:
                return CTXT.floatLiteral(value.floatValue());
            case FloatDataType.SCALE_DECIMAL64:
                if (value instanceof Decimal64) {
                    return CTXT.longLiteral(Decimal64.toUnderlying((Decimal64) value));
                }
                return CTXT.longLiteral(fromNumber(value.doubleValue()));
            default:
                return CTXT.doubleLiteral(value.doubleValue());
        }
    }

    @Override
    public JExpr makeConstantExpr(Object obj) {
        if (obj instanceof String) {
            String s = (String) obj;
            switch (dt.getScale()) {
                case FloatDataType.FIXED_FLOAT:
                    return CTXT.floatLiteral(Float.parseFloat(s));
                case FloatDataType.SCALE_DECIMAL64:
                    return CTXT.longLiteral(Decimal64Utils.parse(s));
                default:
                    return CTXT.doubleLiteral(Double.parseDouble(s));
            }
        } else {
            return super.makeConstantExpr(obj);
        }
    }

    //     JExpr getLiteral(String literal)

    @Override
    public JExpr                getNullLiteral () {
        switch (dt.getScale()) {
            case FloatDataType.FIXED_FLOAT:
                return CTXT.staticVarRef(Float.class, "NaN");
            case FloatDataType.SCALE_DECIMAL64:
                return CTXT.staticVarRef(Decimal64Utils.class, "NULL");
            default:
                return (CTXT.staticVarRef(Double.class, "NaN"));
        }
    }

    @Override
    public JExpr                checkNull (JExpr e, boolean eq) {
        JExpr isNull;

        switch (dt.getScale()) {
            case FloatDataType.FIXED_FLOAT:
                isNull = CTXT.staticCall(Float.class, "isNaN", e);
                break;
            case FloatDataType.SCALE_DECIMAL64:
                isNull = CTXT.binExpr(e, "==", CTXT.staticVarRef(Decimal64Utils.class, "NULL"));
                break;
            default:
                isNull = CTXT.staticCall(Double.class, "isNaN", e);
                break;
        }

        return (eq ? isNull : isNull.not());
    }

    @Override
    public JExpr checkNan(JExpr e, boolean eq) {
        JExpr isNan;

        switch (dt.getScale()) {
            case FloatDataType.FIXED_FLOAT:
                isNan = CTXT.staticCall(Float.class, "isNaN", e);
                break;
            case FloatDataType.SCALE_DECIMAL64:
                isNan = CTXT.staticCall(Decimal64Utils.class, "isNaN", e);
                break;
            default:
                isNan = CTXT.staticCall(Double.class, "isNaN", e);
                break;
        }

        return (eq ? isNan : isNan.not());
    }

    @Override
    public int                  getEncodedFixedSize () {
        switch (dt.getScale ()) {
            case FloatDataType.FIXED_FLOAT:
                return (4);

            case FloatDataType.FIXED_DOUBLE:
                return (8);

            case FloatDataType.SCALE_DECIMAL64:
                return (8);

            default:
                return (SIZE_VARIABLE);
        }
    }

    @Override
    public JStatement       skip (JExpr input) {
        final String function;

        switch (dt.getScale ()) {
            case FloatDataType.FIXED_FLOAT:
            case FloatDataType.FIXED_DOUBLE:
            case FloatDataType.SCALE_DECIMAL64:
                function = "readLong";
                break;

            default:
                function = "readScaledDouble";
                break;
        }

        return (input.call (function).asStmt ());
    }

    @Override
    protected void          encodeNullImpl(JExpr output, JCompoundStatement addTo) {
        final JExpr     writeExpr;
        final int       scale = dt.getScale();
        final JExpr     value = getNullLiteral ();

        switch (scale) {
            case FloatDataType.FIXED_FLOAT:
                writeExpr = output.call ("writeFloat", value);
                break;

            case FloatDataType.FIXED_DOUBLE:
                writeExpr = output.call ("writeDouble", value);
                break;

            case FloatDataType.SCALE_AUTO:
                writeExpr = output.call ("writeScaledDouble", value);
                break;

            case FloatDataType.SCALE_DECIMAL64:
                writeExpr = output.call ("writeLong", value);
                break;

            default:
                writeExpr = output.call ("writeScaledDouble", value, CTXT.intLiteral (scale));
                break;
        }

        addTo.add (writeExpr);
    }

    @Override
    public JStatement           decode (JExpr input, QValue value) {
        final String function;
        switch (dt.getScale ()) {
            case FloatDataType.FIXED_FLOAT:
                function = "readFloat";
                break;

            case FloatDataType.FIXED_DOUBLE:
                function = "readDouble";
                break;

            case FloatDataType.SCALE_DECIMAL64:
                function = "readLong";
                break;

            default:
                function = "readScaledDouble";
                break;
        }

        return (value.write (input.call (function)));
    }

    @Override
    public void                 encode (
        QValue                      value, 
        JExpr                       output,
        JCompoundStatement          addTo
    )
    {
        JExpr                   e = value.read ();
        final JExpr             writeExpr;
        final int               scale = dt.getScale();

        switch (scale) {
            case FloatDataType.FIXED_FLOAT:
                writeExpr = output.call ("writeFloat", e);
                break;

            case FloatDataType.FIXED_DOUBLE:
                writeExpr = output.call ("writeDouble", e);
                break;

            case FloatDataType.SCALE_AUTO:
                writeExpr = output.call ("writeScaledDouble", e);
                break;

            case FloatDataType.SCALE_DECIMAL64:
                writeExpr = output.call ("writeLong", e);
                break;

            default:
                writeExpr = output.call ("writeScaledDouble", e, CTXT.intLiteral (scale));
                break;
        }

        addTo.add (writeExpr);
    }

    @Decimal
    private static long fromNumber(Number number) {
        if (number instanceof Integer || number instanceof Short || number instanceof Byte) {
            return Decimal64Utils.fromInt(number.intValue());
        } else if (number instanceof Long) {
            return Decimal64Utils.fromLong(number.longValue());
        } else {
            return Decimal64Utils.fromDouble(number.doubleValue());
        }
    }
}