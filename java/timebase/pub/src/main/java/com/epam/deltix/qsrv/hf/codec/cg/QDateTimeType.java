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

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.jcg.*;

import java.util.Date;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.*;

/**
 *
 */
public class QDateTimeType extends QPrimitiveType <DateTimeDataType> {
    public static final QDateTimeType       NON_NULLABLE =
        new QDateTimeType (new DateTimeDataType (false));

    public static final QDateTimeType       NULLABLE =
        new QDateTimeType (new DateTimeDataType (true));

    public static final JExpr               NULL =
        CTXT.longLiteral (DateTimeDataType.NULL);

    protected QDateTimeType (DateTimeDataType dt) {
        super (dt);
    }

    @Override
    public Class <?>            getJavaClass () {
        return (long.class);
    }

    @Override
    protected JExpr makeConstantExpr(Object obj) {
        long            v;

        if (obj instanceof Long)
            v = ((Long) obj);
        else if (obj instanceof Date)
            v = ((Date) obj).getTime ();
        else
            throw new IllegalArgumentException ("Illegal: " + obj);

        return (CTXT.longLiteral (v));
    }

    @Override
    protected JExpr getNullLiteral() {
        return CTXT.staticVarRef(DateTimeDataType.class, "NULL");
    }

    @Override
    public int                  getEncodedFixedSize () {
        return (8);
    }

    @Override
    protected JExpr decodeExpr(JExpr input) {
        return input.call("readLong");
    }

    @Override
    protected void encodeExpr(JExpr output, JExpr value, JCompoundStatement addTo) {
        addTo.add(output.call("writeLong", value));
    }
}
