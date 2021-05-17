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

import com.epam.deltix.qsrv.hf.pub.ValueOutOfRangeException;
import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.util.jcg.*;
import com.epam.deltix.util.lang.MathUtil;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.*;

/**
 *
 */
public abstract class QNumericType <T extends DataType> extends QType <T> {
    public final int            kind;
    public final Number         min;
    public final Number         max;

    public static int           compare (Number a, Number b) {
        Class <?>       ac = a.getClass ();
        Class <?>       bc = b.getClass ();

        if (ac == Float.class && bc == Float.class)
            return (MathUtil.compare (a.floatValue (), b.floatValue ()));
        else if (ac == Float.class || ac == Double.class ||
                 bc == Float.class || bc == Double.class)
            return (MathUtil.compare (a.doubleValue (), b.doubleValue ()));
        else
            return (MathUtil.compare (a.longValue (), b.longValue ()));
    }

    protected QNumericType (T dt, int kind, Number min, Number max) {
        super (dt);
        this.kind = kind;
        this.min = min;
        this.max = max;
    }

    public abstract JExpr       getLiteral (Number value);

    @Override
    public JExpr                makeConstantExpr(Object obj) {
        return getLiteral ((Number) obj);
    }

    @Override
    public final void           move (
        QValue                      from,
        QValue                      to,
        JCompoundStatement          addTo
    )
    {
        QNumericType        fromType = (QNumericType) from.type;
        QNumericType        toType = (QNumericType) to.type;
        boolean             minCheckRequired = compare (fromType.min, toType.min) < 0;
        boolean             maxCheckRequired = compare (fromType.max, toType.max) > 0;     
        
        JExpr               e = from.read ();
        JCompoundStatement  actionIfNotNull = CTXT.compStmt ();
        
        if (minCheckRequired || maxCheckRequired) {
            JExpr               emin;
            JExpr               emax;
            JExpr               minTest;
            JExpr               maxTest;

            if (minCheckRequired) {
                emin = fromType.getLiteral (toType.min);
                minTest = CTXT.binExpr (e, "<", emin);                
            }
            else {
                emin = CTXT.nullLiteral ();
                minTest = null;
            }

            if (maxCheckRequired) {
                emax = fromType.getLiteral (toType.max);
                maxTest = CTXT.binExpr (e, ">", emax);
            }
            else {
                emax = CTXT.nullLiteral ();
                maxTest = null;
            }

            JStatement          ts = 
                CTXT.newExpr (ValueOutOfRangeException.class, e, emin, emax).throwStmt ();

            if (minCheckRequired && maxCheckRequired)
                actionIfNotNull.add (
                    CTXT.ifStmt (
                        CTXT.binExpr (minTest, "||", maxTest),
                        ts
                    )
                );
            else if (minCheckRequired)
                actionIfNotNull.add (CTXT.ifStmt (minTest, ts));
            else
                actionIfNotNull.add (CTXT.ifStmt (maxTest, ts));            
        }
        
        actionIfNotNull.add (to.write (e));
        
        addTo.add (wrapWithNullCheck (actionIfNotNull, from, to));
    }
}
