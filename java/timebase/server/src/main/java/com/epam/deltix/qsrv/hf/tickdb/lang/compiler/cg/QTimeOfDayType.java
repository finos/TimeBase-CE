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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.qsrv.hf.pub.md.TimeOfDayDataType;
import com.epam.deltix.util.jcg.*;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

public class QTimeOfDayType extends QType <TimeOfDayDataType> {
    public QTimeOfDayType (TimeOfDayDataType dt) {
        super(dt);
    }

    @Override
    public JStatement   decode (JExpr input, QValue value) {
        return (value.write (input.call ("readInt")));        
    }

    @Override
    public void         encode (QValue value, JExpr output, JCompoundStatement addTo) {
        addTo.add (output.call("writeInt", value.read ()));
    }
   
    @Override
    public Class <?>    getJavaClass() {
        return int.class;
    }

    @Override
    public JExpr        getNullLiteral () {
        return CTXT.staticVarRef (TimeOfDayDataType.class, "NULL");
    }

    @Override
    public JExpr        makeConstantExpr (Object obj) {
        return CTXT.intLiteral (((Number) obj).intValue ());
    }

    @Override
    protected void      encodeNullImpl (JExpr output, JCompoundStatement addTo) {
        addTo.add (output.call ("writeInt", getNullLiteral ()));
    }

    @Override
    public int          getEncodedFixedSize() {
        return 4;
    }
}