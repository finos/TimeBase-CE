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

import com.epam.deltix.qsrv.hf.codec.CodecUtils;
import com.epam.deltix.qsrv.hf.pub.codec.AlphanumericCodec;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JVariable;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

/**
 *
 */
public class QBAlphanumericType extends QBoundType<QAlphanumericType> {
    private JExpr codec = null;

    public QBAlphanumericType(QAlphanumericType qType, Class<?> javaType, QAccessor accessor, QVariableContainerLookup lookupContainer) {
        super(qType, javaType, accessor);

        initHelperMembers(lookupContainer);
    }

    private void initHelperMembers(QVariableContainerLookup lookupContainer) {
        final int len = qType.dt.getLength();
        final String name = "codec_" + len;
        JVariable var = lookupContainer.lookupVar(name);
        if (var == null) {
            var = lookupContainer.addVar(AlphanumericCodec.class, name, CTXT.newExpr(AlphanumericCodec.class, CTXT.intLiteral(len)));
        }
        codec = lookupContainer.access(var);
    }

    @Override
    public void decode(JExpr input, JCompoundStatement addTo) {
        if (codec == null)
            throw new IllegalStateException("codec variable was not set");

        final JExpr value;
        if (javaBaseType == long.class)
            value = codec.call("readLong", input);
        else if (javaBaseType == String.class || javaBaseType == CharSequence.class)
            value = CTXT.staticCall(CodecUtils.class, "getString", codec.call("readCharSequence", input));
        else
            throw new IllegalStateException("unexpected bound type " + javaBaseType);

        addTo.add(accessor.write(value));
    }

    @Override
    public void encode(JExpr output, JCompoundStatement addTo) {
        if (codec == null)
            throw new IllegalStateException("codec variable was not set");

        final String function;
        if (javaBaseType == long.class)
            function = "writeLong";
        else if (javaBaseType == String.class || javaBaseType == CharSequence.class)
            function = "writeCharSequence";
        else
            throw new IllegalStateException("unexpected bound type " + javaBaseType);

        addTo.add(codec.call(function, getEncodeValue(getNullLiteral()), output));
    }

    @Override
    protected JExpr getNullLiteralImpl() {
        return getNullLiteral4BoundType();
    }

    private JExpr getNullLiteral4BoundType() {
        if (javaBaseType == String.class || javaBaseType == CharSequence.class )
            return CTXT.nullLiteral();
        else if (javaBaseType  == long.class)
            return CTXT.staticVarRef(IntegerDataType.class, "INT64_NULL");
        else
            throw new UnsupportedOperationException("unexpected bound type " + javaBaseType.getName());
    }

    @Override
    protected JExpr makeConstantExpr(Object obj) {
        if (javaBaseType == String.class || javaBaseType == CharSequence.class)
            return CTXT.stringLiteral((String) obj);
        else if (javaBaseType == long.class) {
            if (obj instanceof String)
                return codec.call("encodeToLong", CTXT.stringLiteral((String) obj));
            else
                throw new UnsupportedOperationException();
        } else
            throw new UnsupportedOperationException("unexpected bound type " + javaBaseType.getName());
    }
}
