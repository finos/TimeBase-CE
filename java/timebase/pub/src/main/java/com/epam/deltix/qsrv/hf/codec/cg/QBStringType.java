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

import com.epam.deltix.qsrv.hf.codec.BinaryUtils;
import com.epam.deltix.qsrv.hf.codec.CodecUtils;
import com.epam.deltix.qsrv.hf.pub.md.VarcharDataType;
import com.epam.deltix.util.collections.generated.ByteArrayList;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

/**
 *
 */
public class QBStringType extends QBoundType<QStringType> {

    public QBStringType(QStringType qType, Class<?> javaType, QAccessor accessor, QVariableContainerLookup lookupContainer) {
        super(qType, javaType, accessor);
    }

    @Override
    public void decode(JExpr input, JCompoundStatement addTo) {
        if (javaBaseType == String.class)
            super.decode(input, addTo);
        else if (javaBaseType == CharSequence.class)
            addTo.add(accessor.write( CTXT.call("readCharSequence", input)));
        else if(javaBaseType == ByteArrayList.class) {
            addTo.add(
                    accessor.write(
                            CTXT.staticCall(
                                    BinaryUtils.class,
                                    "assign",
                                    accessor.read(),
                                    CTXT.call("readCharSequence", input)
                            )
                    )
            );
        } else
            throw new IllegalArgumentException("unexpected type " + javaBaseType);
    }

    @Override
    public void encode(JExpr output, JCompoundStatement addTo) {
        if (javaBaseType == String.class) {
            addTo.add(CTXT.staticCall(CodecUtils.class, "setString", accessor.read(), output,
                    qType.dt.getEncodingType() == VarcharDataType.INLINE_VARSIZE ? CTXT.trueLiteral() : CTXT.falseLiteral()));
        } else if(javaBaseType == ByteArrayList.class) {
            addTo.add(
                    output.call(
                            "writeString",
                            CTXT.staticCall(
                                    BinaryUtils.class,
                                    "toStringBuilder",
                                    accessor.read()
                            )
                    )
            );
        } else
            super.encode(output, addTo);
    }

    @Override
    protected JExpr makeConstantExpr(Object obj) {
            return super.makeConstantExpr(obj);
    }
}