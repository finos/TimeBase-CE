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

import com.epam.deltix.qsrv.hf.pub.codec.NestedObjectCodec;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.Instance;
import com.epam.deltix.util.jcg.*;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

/**
 *
 */
public class QObjectType extends QType <ClassDataType> {
    public QObjectType (ClassDataType dt) {
        super (dt);
    }
    
    @Override
    public QValue               declareValue (
        String                      comment,
        QVariableContainer          container, 
        QClassRegistry              registry,
        boolean                     setNull
    )
    {
        JExpr           init = CTXT.newExpr (Instance.class);        
        JVariable       v = container.addVar (comment, true, Instance.class, init);
        
        return (new QObjectValue (this, container.access (v)));
    }

    @Override
    public JStatement       skip (JExpr input) {
        return (CTXT.staticCall (NestedObjectCodec.class, "skip", input).asStmt ());
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
        throw new UnsupportedOperationException ();
    }

    @Override
    public JExpr            getNullLiteral() {
        return CTXT.nullLiteral ();
    }

    @Override
    protected void          encodeNullImpl (JExpr output, JCompoundStatement addTo) {
        throw new UnsupportedOperationException ();
        //addTo.add (CTXT.staticCall (BinaryCodec.class, "writeNull", output));
    }

    @Override
    public JStatement       decode (JExpr input, QValue value) {
        throw new UnsupportedOperationException ();
        //return (value.read ().call ("decode", input).asStmt ());
    }

    @Override
    public void             encode (QValue value, JExpr output, JCompoundStatement addTo) {
        throw new UnsupportedOperationException ();
        //addTo.add (value.read ().call ("encode", output));
    }        
}
