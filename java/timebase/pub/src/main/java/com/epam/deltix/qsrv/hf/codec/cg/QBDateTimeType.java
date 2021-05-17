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
