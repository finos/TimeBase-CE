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
import com.epam.deltix.util.jcg.*;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.*;

/**
 *
 */
public class QBooleanType extends QPrimitiveType <BooleanDataType> {
    public static final QBooleanType     NON_NULLABLE =
        new QBooleanType (new BooleanDataType (false));

    public static final QBooleanType     NULLABLE =
        new QBooleanType (new BooleanDataType (true));

    public static final JExpr      N_TRUE = CTXT.intLiteral (1).cast(byte.class);
    public static final JExpr      N_FALSE = CTXT.intLiteral (0).cast(byte.class);
    public static final JExpr      NULL = CTXT.staticVarRef(BooleanDataType.class, "NULL").cast(byte.class);

    protected QBooleanType (BooleanDataType dt) {
        super (dt);
    }

    @Override
    public Class <?>            getJavaClass () {
        return (isNullable () ? byte.class : boolean.class);
    }

    public JExpr                getLiteral (boolean test) {
        return (
            isNullable () ?
                test ? N_TRUE : N_FALSE :
                CTXT.booleanLiteral (test)
        );
    }

    @Override
    public JExpr                getNullLiteral () {
        return (NULL);
    }

    @Override
    public int                  getEncodedFixedSize () {
        return (1);
    }

    @Override
    protected void encodeNullImpl(JExpr output, JCompoundStatement addTo) {
        addTo.add (output.call ("writeByte", getNullLiteral ()));
    }

    @Override
    protected JExpr decodeExpr(JExpr input) {
        if (isNullable ())
            return input.call("readByte");
        else
            return input.call("readBoolean");
    }

    @Override
    protected void encodeExpr(JExpr output, JExpr value, JCompoundStatement addTo) {
        if (isNullable ())
            addTo.add(output.call("writeByte", value));
        else
            addTo.add(output.call("writeBoolean", value));
    }

    @Override
    public JExpr makeConstantExpr(Object obj) {
        // TODO: should I use getLiteral here?
        return CTXT.booleanLiteral((Boolean) obj);
    }
}
