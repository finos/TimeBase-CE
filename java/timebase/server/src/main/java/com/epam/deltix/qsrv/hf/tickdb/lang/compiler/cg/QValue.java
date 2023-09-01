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

import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JStatement;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

/**
 *  Represents a value stored as a constant, variable or object field.
 */
public abstract class QValue {
    public final QType          type;

    
    protected QValue (QType type) {
        this.type = type;
    }

    /**
     *  Reads the value. The type of what is read is QType-dependent.
     */
    public abstract JExpr       read ();

    /**
     *  Writes the value. The type of the argument is QType-dependent.
     */
    public JStatement           write (JExpr arg) {
        return (read ().assign (arg));
    }

    /**
     *  Generates code to test for null.
     */
    public JExpr                readIsNull (boolean eq) {
        return (type.checkNull (read (), eq));
    }

    public JStatement           writeNull () {
        return (write (type.getNullLiteral ()));
    }
    
    public final void           encode (JExpr out, JCompoundStatement addTo) {
        type.encode (this, out, addTo);
    }

    public final void           encodeNull (JExpr out, JCompoundStatement addTo) {
        type.encodeNull(out, addTo);
    }
    
    public final JStatement     decode (JExpr in) {
        return (type.decode (in, this));
    }
       
    public JStatement           decodeRelative(
        JExpr                       input,
        QValue                      base
    )
    {
        JCompoundStatement  cs = CTXT.compStmt ();
        
        JStatement          dec = decode (input);
        
        cs.add (dec);
        
        cs.add (
            CTXT.ifStmt (
                base.readIsNull (false), 
                write (CTXT.binExpr (read (), "+", base.read ()))
            )
        );
        
        return (cs);
    }    
}