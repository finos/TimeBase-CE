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
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.*;

/**
 *
 */
//TMP - extends Primitive. Need to clean up all.
public class QQueryType extends QType <QueryDataType> {
    public QQueryType (QueryDataType dt) {
        super (dt);
    }

    @Override
    public Class <?>    getJavaClass () {
        return (InstrumentMessageSource.class);
    }

    @Override
    public JExpr        getNullLiteral () {
        return (CTXT.nullLiteral ());
    }

    @Override
    public int          getEncodedFixedSize () {
        throw new UnsupportedOperationException ("Not serializable.");
    }

    @Override
    public JStatement   decode (JExpr input, QValue value) {
        throw new UnsupportedOperationException ("Not serializable.");
    }

    @Override
    public void         encode (QValue value, JExpr output, JCompoundStatement addTo) {
        throw new UnsupportedOperationException ("Not serializable.");
    }        
}