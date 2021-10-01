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

import com.epam.deltix.qsrv.hf.codec.BindValidator;
import com.epam.deltix.qsrv.hf.pub.NullValueException;
import com.epam.deltix.qsrv.hf.codec.BinaryCodec;
import com.epam.deltix.util.collections.SmallArrays;
import com.epam.deltix.util.collections.generated.ByteArrayList;
import com.epam.deltix.util.jcg.*;
import com.epam.deltix.containers.BinaryArray;
import com.epam.deltix.containers.interfaces.BinaryArrayReadOnly;

import java.util.Arrays;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;
import static java.lang.reflect.Modifier.FINAL;

/**
 *
 */
public class QBBinaryType extends QBoundType<QBinaryType> {
    private static final JExpr ZERO_INTEGER = CTXT.intLiteral(0);
    private JExpr codec = null;

    private JExpr manager = null;

    public QBBinaryType(QBinaryType qType, Class<?> javaType, QAccessor accessor, QVariableContainerLookup lookupContainer) {
        super(qType, javaType, accessor);

        if (!BindValidator.isBinaryArray(javaBaseType))
            throw new IllegalArgumentException("unexpected type " + javaBaseType.getName());

        initHelperMembers(lookupContainer);
    }

    private void initHelperMembers(QVariableContainerLookup lookupContainer) {
        if (!lookupContainer.isEncoding) {
            manager = lookupContainer.access(lookupContainer.lookupVar(CodecGenerator.MANAGER_NAME));

            final String name = "binaryCodec";
            JVariable var = lookupContainer.lookupVar(name);
            if (var == null)
                var = lookupContainer.addVar(BinaryCodec.class, name, CTXT.newExpr(BinaryCodec.class));

            codec = lookupContainer.access(var);
        }
    }

    @Override
    protected JExpr readIsNullImpl(boolean eq) {
        if (javaBaseType == ByteArrayList.class) {
            JExpr expr = CTXT.binExpr(super.readIsNullImpl(true), "||", accessor.read().call("isEmpty"));
            return eq ? expr : expr.not();
        } if (javaBaseType.isAssignableFrom(BinaryArrayReadOnly.class)) {
            JExpr expr = CTXT.binExpr(super.readIsNullImpl(true), "||", CTXT.binExpr(accessor.read().call("size"), "==", ZERO_INTEGER));
            return eq ? expr : expr.not();
        } else
            throw new UnsupportedOperationException("unexpected bound type " + javaBaseType);
    }

    @Override
    public void decode(JExpr input, JCompoundStatement addTo) {
        final JTryStatement tryStmt = CTXT.tryStmt();
        addTo.add(tryStmt);

        final JCompoundStatement catchStmt = tryStmt.addCatch(NullValueException.class, "e");
        if (qType.isNullable())
            catchStmt.add(accessor instanceof QAccessMethod ? writeNullify() : writeNull());
        else
            catchStmt.add(QCGHelpers.throwISX(String.format("cannot write null to not nullable field '%s'", accessor.getFieldName())));
        final JCompoundStatement stmt = tryStmt.tryStmt();
        stmt.add(codec.call("readHeader", input));

        final JLocalVariable len = stmt.addVar(FINAL, int.class, "len", codec.call("getLength"));
        if (javaBaseType == ByteArrayList.class) {
            stmt.add(accessor.write(manager.call("use", CTXT.classLiteral(javaBaseType) ,len).cast(javaBaseType)));
            stmt.add(accessor.read().call("setSizeUnsafe", len));
            stmt.add(
                    codec.call("getBinary", ZERO_INTEGER, ZERO_INTEGER, len, accessor.read().call("getInternalBuffer"), ZERO_INTEGER)
            );
        }
        else if (javaBaseType.isAssignableFrom(BinaryArrayReadOnly.class)) {
            stmt.add(accessor.write(manager.call("use", CTXT.classLiteral(BinaryArray.class), len).cast(javaBaseType)));
            stmt.add(accessor.read().cast(BinaryArray.class).call("clear"));
            stmt.add(
                    codec.call("getBinary", ZERO_INTEGER, len, accessor.read().cast(BinaryArray.class), ZERO_INTEGER)
            );
        }
        else
            throw new UnsupportedOperationException("unexpected bound type " + javaBaseType);

        addTo.add(codec.call("skip"));
    }

    @Override
    public void encode(JExpr output, JCompoundStatement addTo) {
        final JExpr expr;
        if (javaBaseType == ByteArrayList.class)
            // BinaryCodec.write(msg.bal1.getInternalBuffer(), 0, msg.bal1.size(), out0, 0);
            expr = CTXT.staticCall(BinaryCodec.class, "write",
                accessor.read().call("getInternalBuffer"),
                ZERO_INTEGER,
                accessor.read().call("size"),
                output,
                ZERO_INTEGER);
        else if (javaBaseType == byte[].class)
            // BinaryCodec.write(msg.b1, 0, msg.b1.Length, out0, 0);
            expr = CTXT.staticCall(BinaryCodec.class, "write",
                accessor.read(),
                ZERO_INTEGER,
                CTXT.arrayLength(accessor.read()),
                output,
                ZERO_INTEGER);
        else if (javaBaseType.isAssignableFrom(BinaryArrayReadOnly.class))
            // BinaryCodec.write(msg.b1, 0, msg.b1.Length, out0, 0);
            expr = CTXT.staticCall(BinaryCodec.class, "write",
                    accessor.read(),
                    ZERO_INTEGER,
                    accessor.read().call("size"),
                    output,
                    ZERO_INTEGER);
        else
            throw new UnsupportedOperationException("unexpected bound type " + javaBaseType);

        final JStatement elseStmt;
        if (qType.isNullable()) {
            final JCompoundStatement stmt = CTXT.compStmt();
            qType.encodeNull(output, stmt);
            elseStmt = stmt;
        } else
            elseStmt = QCGHelpers.throwISX(String.format("cannot write null to not nullable field '%s'", accessor.getFieldName()));

        addTo.add(
            CTXT.ifStmt(
                readIsNull(true),
                elseStmt,
                expr.asStmt()
            )
        );
    }
}