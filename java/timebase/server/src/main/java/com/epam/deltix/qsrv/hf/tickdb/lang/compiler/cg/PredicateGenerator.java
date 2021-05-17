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

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.jcg.*;
import com.epam.deltix.util.io.*;
import com.epam.deltix.util.jcg.scg.*;
import java.io.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.*;
import com.epam.deltix.util.lang.*;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.*;
import static java.lang.reflect.Modifier.*;

/**
 *
 */
public class PredicateGenerator {
    private static final String                 PREDICATE = "Predicate";
    private static final String                 MESSAGE = "msg";
    private static final String                 TYPE_IDX = "typeIdx";
    
    private JClass                              pjclass;
    private final RecordClassDescriptor []      inputTypes;
    
    public PredicateGenerator (
        RecordClassDescriptor []        inputTypes,
        CompiledExpression              e
    )
    {
        this.inputTypes = inputTypes;
        
        pjclass = 
            CTXT.newClass (
                PUBLIC | FINAL, 
                null, PREDICATE, 
                MessagePredicate.class
            );
        
        JMethod             acceptMethod = 
            pjclass.addMethod (PUBLIC, boolean.class, "eval");
        
        QClassRegistry      classRegistry = 
            new QClassRegistry (pjclass.inheritedVar ("types").access ());
        
        JCompoundStatement  body = acceptMethod.body ();
        
        EvalGenerator       eg = 
            new EvalGenerator (
                null,   // params
                pjclass.inheritedVar (MESSAGE).access (),
                classRegistry,
                new QVariableContainer (FINAL, body, null, "$"),
                new QVariableContainer (PRIVATE, pjclass, null /*?*/, "v"),
                body
            );
        
        SourceClassMap                  scm = new SourceClassMap (inputTypes);

        scm.discoverFieldSelectors (e);
        
        SelectorGenerator   sg = 
            new SelectorGenerator (pjclass, eg) {
                @Override
                protected JExpr     getTypeIdxExpr () {
                    return (pjclass.inheritedVar (TYPE_IDX).access ());
                }
            };
        
        sg.genSelectors (scm);
        
        QValue              ret = eg.genEval (e);
        
        body.add (
            QBooleanType.nullableToClean (ret.read ()).returnStmt ()
        );
    }
    
    public MessagePredicate   finish (JavaCompilerHelper helper) {
        StringBuilder       buf = new StringBuilder ();
        SourceCodePrinter   p = new SourceCodePrinter (buf);

        try {
            p.print (pjclass);
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        }

        String      code = buf.toString ();

        if (DEBUG_DUMP_CODE) {
            try {
                IOUtil.dumpWithLineNumbers (code, System.out);
            } catch (Exception x) {
                x.printStackTrace ();
            }
        }

        Class <?>   pclass;
        
        try {
            pclass = helper.compileClass (pjclass.name (), code);
        } catch (ClassNotFoundException x) {
            throw new RuntimeException ("unexpected", x);
        }

        MessagePredicate   ret;

        try {
            ret = (MessagePredicate) pclass.newInstance ();
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new RuntimeException ("unexpected", x);
        }

        return (ret);
    }
}
