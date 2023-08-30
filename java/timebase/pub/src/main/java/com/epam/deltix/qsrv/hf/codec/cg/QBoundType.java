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
package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JStatement;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

/**
 * Implements a part of QPrimitiveValue functionality depending on Java/.NET type to which a value is bound to.
 * <p>
 * This includes:
 * encoding/decoding,
 * readIsNull/writeNull,
 * writeObject
 * </p>
 */
public class QBoundType<T extends QPrimitiveType> {
    protected final T qType;
    protected final QAccessor accessor;
    private final Class<?> javaType;
    protected final Class<?> javaBaseType;

    public QBoundType(T qType, Class<?> javaType, QAccessor accessor) {
        this.qType = qType;
        this.accessor = accessor;
        this.javaType = javaType;
        this.javaBaseType = javaType;
    }

    public void decode(JExpr input, JCompoundStatement addTo) {
        addTo.add(accessor.write(qType.decodeExpr(input)));
    }

    public void encode(JExpr output, JCompoundStatement addTo) {
        qType.encodeExpr(output, getEncodeValue(qType.getNullLiteral()), addTo);
    }

    protected final JExpr getEncodeValue(JExpr nullLiteral) {
        return accessor.read();
    }

    public Class<?> getJavaClass() {
        return javaType;
    }

    public Class<?> getJavaBaseType() {
        return javaBaseType;
    }

    public final boolean hasNullLiteral() {
        return hasNullLiteralImpl();
    }

    // should use only bound type null-value and not take into account value == null (for .NET nullable)
    protected boolean hasNullLiteralImpl() {
        return true;
    }

    public JExpr getNullLiteral() {
        return getNullLiteralImpl();
    }

    protected JExpr getNullLiteralImpl() {
        return qType.getNullLiteral();
    }

    public final JExpr getLiteral(Object obj) {
        throw new UnsupportedOperationException();
    }

    public final JExpr readIsNull(boolean eq) {
        JExpr expr;
        try {
            expr = readIsNullImpl(eq);
        } catch (UnsupportedOperationException e) {
                throw e;
        }

        return expr;
    }

    // should use only bound type null-value and not take into account value == null (for .NET nullable)
    protected JExpr readIsNullImpl(boolean eq) {
        return CTXT.binExpr(accessor.read(), eq ? "==" : "!=", getNullLiteral());
    }

    public JStatement writeNull() {
        if (!qType.isNullable())
            throw new UnsupportedOperationException("cannot write null, because type is not nullable");
        else
            return writeNullNoCheck();
    }

    JStatement writeNullNoCheck() {
        return accessor.write(getNullLiteral());
    }

    JStatement writeNullify() {
        return accessor.writeNullify(getNullLiteral());
    }

    public static void move(
            QBoundType from,
            QBoundType to,
            JCompoundStatement addTo
    ) {
        addTo.add(to.accessor.write(from.accessor.read()));
    }

    public JStatement writeObject(Object obj) {
        if (!qType.isNullable() && obj == null)
            throw new IllegalArgumentException("null value, but type is not nullable");
        else
            return accessor.write(makeConstantExpr(obj).cast(accessor.getFieldType()));
    }

    protected JExpr makeConstantExpr(Object obj) {
        return qType.makeConstantExpr(obj);
    }

    public boolean hasConstraint() {
        return qType.hasConstraint();
    }

    public JExpr readIsConstraintViolated() {
        throw new UnsupportedOperationException(
            "Not implemented for " + getClass().getSimpleName()
        );
    }

}