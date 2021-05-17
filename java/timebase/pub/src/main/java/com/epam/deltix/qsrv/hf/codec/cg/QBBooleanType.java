/*
 * Copyright 2021 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.qsrv.hf.pub.md.BooleanDataType;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JStatement;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

/**
 *
 */
public class QBBooleanType extends QBoundType<QBooleanType> {
    private final boolean makeException;

    public QBBooleanType(QBooleanType qType, Class<?> javaType, QAccessor accessor, QVariableContainerLookup lookupContainer) {
        super(qType, javaType, accessor);
        // #12059 temporarily convert NULL to false on decoding
        makeException = !lookupContainer.isEncoding&& javaBaseType == boolean.class && !qType.isNullable();

        if (javaBaseType != boolean.class && javaType != byte.class && !(accessor instanceof QAccessMethod))
            throw new IllegalArgumentException("invalid javaType " + javaBaseType);
    }

    @Override
    protected JExpr readIsNullImpl(boolean eq) {
        if (accessor instanceof QAccessMethod)
            return ((QAccessMethod) accessor).haser(eq);
        else if (javaBaseType == byte.class)
            return CTXT.binExpr(accessor.read(), eq ? "==" : "!=", getNullLiteral());
        else
            throw new UnsupportedOperationException("unexpected bound type " + javaBaseType);
    }

    @Override
    public void decode(JExpr input, JCompoundStatement addTo) {
        if (accessor instanceof QAccessMethod)
            super.decode(input, addTo);
        else if (javaBaseType == byte.class)
            addTo.add(accessor.write(input.call("readByte")));
        else
            super.decode(input, addTo);
    }

    @Override
    public void encode(JExpr output, JCompoundStatement addTo) {
        if (javaBaseType == byte.class && !(accessor instanceof QAccessMethod))
            addTo.add(output.call("writeByte", accessor.read()));
        else
            super.encode(output, addTo);
    }

    @Override
    protected boolean hasNullLiteralImpl() {
        if (accessor instanceof QAccessMethod)
            return javaBaseType == byte.class || ((QAccessMethod) accessor).hasSmartProperties;
        return javaBaseType == byte.class;
    }

    @Override
    public JExpr getNullLiteral() {
        return (javaBaseType == null) ? CTXT.intLiteral(-1) : CTXT.staticVarRef(BooleanDataType.class, "NULL");
    }

    @Override
    protected JExpr makeConstantExpr(Object obj) {
        if (javaBaseType == boolean.class)
            return super.makeConstantExpr(obj);
        else {
            final JExpr expr = CTXT.intLiteral(obj == null ? -1 : (Boolean) obj ? 1 : 0);
            return (javaBaseType == byte.class) ? expr.cast(byte.class) : expr;
        }
    }

    @Override
    public JStatement writeNull() {
        // #12059 temporarily convert NULL to false on decoding
        if (makeException)
            return accessor.write(CTXT.booleanLiteral(false));
        else
            return super.writeNull();
    }
}
