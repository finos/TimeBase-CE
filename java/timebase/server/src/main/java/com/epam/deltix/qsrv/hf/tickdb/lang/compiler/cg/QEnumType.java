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

import com.epam.deltix.qsrv.hf.pub.md.EnumDataType;
import com.epam.deltix.util.jcg.*;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

public class QEnumType extends QType <EnumDataType> {
    public static final JExpr       NULL = CTXT.intLiteral (-1);
    
    public QEnumType (EnumDataType dt) {
        super(dt);
    }

    @Override
    public int          getEncodedFixedSize() {
        return dt.descriptor.computeStorageSize ();
    }

    @Override
    public Class <?>    getJavaClass() {
        final int size = getEncodedFixedSize ();
        
        switch (size) {
            case 1: return byte.class;                
            case 2: return short.class;
            case 4: return int.class;
            case 8: return long.class;
            default: throw new IllegalStateException ("unexpected size " + size);
        }
    }

    @Override
    public JExpr        getNullLiteral() {
        final int size = getEncodedFixedSize ();

        switch (size) {
            case 1: return NULL.cast(byte.class);
            case 2: return NULL.cast(short.class);
            case 4: return NULL;
            case 8: return NULL.cast(long.class);
            default: return NULL;
        }
    }

    @Override
    protected void      encodeNullImpl (JExpr output, JCompoundStatement addTo) {
        addTo.add (
            output.call (
                QIntegerType.getEncodeMethod (getEncodedFixedSize ()), 
                NULL
            )
        );
    }

    @Override
    protected JExpr     makeConstantExpr(Object obj) {
        return (CTXT.longLiteral (((Number) obj).longValue ()).cast (getJavaClass ()));        
    }

    @Override
    public JStatement   decode (JExpr input, QValue value) {
        return (
            value.write (
                input.call (QIntegerType.getDecodeMethod (getEncodedFixedSize ()))
            )
        );
    }

    @Override
    public void         encode (QValue value, JExpr output, JCompoundStatement addTo) {
        addTo.add (
            output.call (
                QIntegerType.getEncodeMethod (getEncodedFixedSize ()), 
                value.read ()
            )
        );
    }        
}