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

import com.epam.deltix.qsrv.hf.pub.codec.AlphanumericCodec;
import com.epam.deltix.qsrv.hf.pub.md.VarcharDataType;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

public class QAlphanumericType extends QPrimitiveType <VarcharDataType> {

    public QAlphanumericType(VarcharDataType dt) {
        super(dt);
        if (dt.getEncodingType() != VarcharDataType.ALPHANUMERIC)
            throw new IllegalArgumentException("invalid encoding " + dt.getEncoding());
    }

    @Override
    public int getEncodedFixedSize() {
        return SIZE_VARIABLE;
    }

    @Override
    public void skip(JExpr input, JCompoundStatement addTo) {
        addTo.add(CTXT.staticCall(AlphanumericCodec.class, "skip", input, CTXT.intLiteral(dt.getLength())));
    }

    @Override
    protected void encodeNullImpl(JExpr output, JCompoundStatement addTo) {
        addTo.add(CTXT.staticCall(AlphanumericCodec.class, "writeNull", output, CTXT.intLiteral(dt.getLength())));
    }

    @Override
    public Class<?> getJavaClass() {
        throw new UnsupportedOperationException("Not implemented for QAlphanumericType");
    }

    @Override
    protected JExpr getNullLiteral() {
        throw new UnsupportedOperationException("Not implemented for QAlphanumericType");
    }
}