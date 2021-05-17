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

import com.epam.deltix.util.jcg.JContext;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JStatement;
import com.epam.deltix.util.jcg.scg.JavaSrcGenContext;

/**
 *
 */
public class QCGHelpers {

    public static final JContext       CTXT = new JavaSrcGenContext();
    
    public static JExpr        objtoex (Object obj) {
        if (obj instanceof JExpr)
            return ((JExpr) obj);

        if (obj instanceof Boolean)
            return (CTXT.booleanLiteral ((Boolean) obj));

        if (obj instanceof Integer)
            return (CTXT.intLiteral ((Integer) obj));

        if (obj instanceof Long)
            return (CTXT.longLiteral ((Long) obj));

        if (obj instanceof Short)
            return (CTXT.intLiteral ((Short) obj));

        if (obj instanceof Byte)
            return (CTXT.intLiteral ((Byte) obj));

        if (obj instanceof Double)
            return (CTXT.doubleLiteral ((Double) obj));
        
        if (obj instanceof Float)
            return (CTXT.floatLiteral ((Float) obj));

        return (CTXT.stringLiteral (obj.toString ()));
    }

    static JExpr []     objtoex (Object ... args) {
        int         n = args.length;
        JExpr []    ret = new JExpr [n];

        for (int ii = 0; ii < n; ii++)
            ret [ii] = objtoex (args [ii]);

        return (ret);
    }

    static JStatement   throwIAX (String msg) {
        return throwIAX(objtoex(msg));
    }

    static JStatement   throwIAX (JExpr msgExpr) {
        return (
            CTXT.newExpr(
                IllegalArgumentException.class,
                msgExpr
            ).throwStmt()
        );
    }

    static JStatement   throwISX (Object ... args) {
        return (
            CTXT.newExpr (
                IllegalStateException.class,
                CTXT.sum (objtoex (args))
            ).throwStmt ()
        );
    }
}
