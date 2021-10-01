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

import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.Binary;
import com.epam.deltix.qsrv.hf.pub.md.BinaryDataType;
import com.epam.deltix.qsrv.hf.codec.BinaryCodec;
import com.epam.deltix.util.jcg.*;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

public class QBinaryType extends QType <BinaryDataType> {
    public QBinaryType (BinaryDataType dt) {
        super(dt);
    }
    
    @Override
    public QValue               declareValue (
        String                      comment,
        QVariableContainer          container, 
        QClassRegistry              registry,
        boolean                     setNull
    )
    {
        JExpr           init =
            CTXT.newExpr (Binary.class, CTXT.intLiteral (dt.getCompressionLevel ()));
        
        JVariable       v = container.addVar (comment, true, Binary.class, init);
        
        return (new QBinaryValue (this, container.access (v)));
    }
    
    @Override
    public void             moveNoNullCheck (
        QValue                  from,
        QValue                  to,
        JCompoundStatement      addTo
    )
    {
        addTo.add (to.read ().call ("set", from.read ()));
    }

    @Override
    public int              getEncodedFixedSize() {
        return SIZE_VARIABLE;
    }

    @Override
    public boolean          instanceAllocatesMemory () {
        return (true);
    }
    
    @Override
    public Class <?>        getJavaClass() {
        throw new UnsupportedOperationException (
            "Not implemented for " + getClass ().getSimpleName ()
        );
    }

    @Override
    public JStatement       skip(JExpr input) {
        return (CTXT.staticCall (BinaryCodec.class, "skip", input).asStmt ());
    }

    @Override
    public JExpr            getNullLiteral() {
        return CTXT.nullLiteral ();
    }

    @Override
    protected void          encodeNullImpl (JExpr output, JCompoundStatement addTo) {
        addTo.add (CTXT.staticCall (BinaryCodec.class, "writeNull", output));
    }

    @Override
    public JStatement       decode (JExpr input, QValue value) {
        return (value.read ().call ("decode", input).asStmt ());
    }

    @Override
    public void             encode (QValue value, JExpr output, JCompoundStatement addTo) {
        addTo.add (value.read ().call ("encode", output));
    }        
}