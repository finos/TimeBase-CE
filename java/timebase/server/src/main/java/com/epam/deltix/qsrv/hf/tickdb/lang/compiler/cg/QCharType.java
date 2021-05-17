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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.util.jcg.JStatement;
import com.epam.deltix.qsrv.hf.pub.md.CharDataType;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

public class QCharType extends QType <CharDataType> {

    public QCharType(CharDataType dt) {
        super(dt);
    }

    @Override
    public Class <?>            getJavaClass() {
        return char.class;
    }

    @Override
    public JExpr                getNullLiteral() {
        return CTXT.staticVarRef (CharDataType.class, "NULL");
    }

    @Override
    public JExpr                makeConstantExpr (Object obj) {
        return CTXT.charLiteral ((Character) obj);
    }

    @Override
    public int                  getEncodedFixedSize () {
        return 2;
    }

    @Override
    public JStatement           decode (JExpr input, QValue value) {
        return (value.write (input.call ("readChar")));
    }

    @Override
    public void                 encode (
        QValue                      value, 
        JExpr                       output,
        JCompoundStatement          addTo
    )
    {
        addTo.add (output.call ("writeChar", value.read ()));
    }
}
