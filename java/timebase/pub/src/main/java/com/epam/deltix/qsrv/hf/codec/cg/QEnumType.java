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

import com.epam.deltix.qsrv.hf.pub.md.EnumDataType;
import com.epam.deltix.util.jcg.*;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

public class QEnumType extends QPrimitiveType <EnumDataType> {

    public QEnumType(EnumDataType dt) {
        super(dt);
    }

    @Override
    public int getEncodedFixedSize() {
        return dt.descriptor.computeStorageSize();
    }

    @Override
    public Class<?> getJavaClass() {
        throw new UnsupportedOperationException (
            "Not implemented for " + getClass ().getSimpleName ()
        );
    }

    @Override
    protected JExpr getNullLiteral() {
        return CTXT.nullLiteral();
    }

    @Override
    protected void encodeNullImpl(JExpr output, JCompoundStatement addTo) {
        addTo.add(output.call(getFunction(dt.descriptor.computeStorageSize(), false), CTXT.intLiteral(-1)));
    }

    static String getFunction(int size, boolean isRead) {
        switch (size) {
            case 1:
                return isRead ? "readByte" : "writeByte";
            case 2:
                return isRead ? "readShort" : "writeShort";
            case 4:
                return isRead ? "readInt" : "writeInt";
            case 8:
                return isRead ? "readLong" : "writeLong";
            default:
                throw new IllegalStateException("unexpected size " + size);
        }
    }
}
