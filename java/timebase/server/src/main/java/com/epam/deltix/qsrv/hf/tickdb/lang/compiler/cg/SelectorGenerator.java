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

import java.util.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.jcg.*;
import com.epam.deltix.util.memory.*;
import static java.lang.reflect.Modifier.*;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.*;
 
/**
 *
 */
abstract class SelectorGenerator {
    public final JClass                 globalClass;
    public final EvalGenerator          evalGenerator;

    public SelectorGenerator (
        JClass                  globalClass, 
        EvalGenerator           evalGenerator
    )
    {
        this.globalClass = globalClass;
        this.evalGenerator = evalGenerator;
    }
        
    protected abstract JExpr            getTypeIdxExpr ();
    
    void                        genSelectors (
        SourceClassMap              scm
    )
    {
        //  MemoryDataInput in = new MemoryDataInput ();
        JExpr             in =
            globalClass.addVar (
                PRIVATE, MemoryDataInput.class, "in",
                CTXT.newExpr (MemoryDataInput.class)
            ).access ();

        // in.setBytes (inMsg.data, inMsg.offset, inMsg.length);
        evalGenerator.addTo.add (
            in.call (
                "setBytes",
                evalGenerator.inMsg.field ("data"),
                evalGenerator.inMsg.field ("offset"),
                evalGenerator.inMsg.field ("length")
            )
        );
        //
        //  Declare cache variables and bind to the evaluation environment
        //
        for (TypeCheckInfo tci : scm.allTypeChecks ()) {
            QBooleanType    qtype = (QBooleanType) QType.forDataType (tci.typeCheck.type);

            tci.cache = 
                qtype.declareValue (
                    "Result of " + tci.typeCheck,
                    evalGenerator.stateVarContainer, 
                    evalGenerator.classRegistry, 
                    false
                );
            
            evalGenerator.bind (tci.typeCheck, tci.cache);
        }

        for (ClassSelectorInfo csi : scm.allClassInfo ()) {
            int                     numFields = csi.highestUsedIdx + 1;

            for (int ii = 0; ii < numFields; ii++) {
                FieldSelectorInfo       fsi = csi.fields [ii];
                
                if (fsi.cache != null)
                    continue;
                
                if (fsi.fieldSelector == null && !fsi.usedAsBase)
                    continue;

                QValue                  cache;
                QType                   fstype = fsi.qtype;
                
                String                  comment =
                    "Decoded " + fsi.fieldSelector;
                
                if (fsi.fieldSelector == null)  // base field, but not used anywhere else
                    if (fstype.instanceAllocatesMemory ())
                        cache = fstype.declareValue (comment, evalGenerator.stateVarContainer, evalGenerator.classRegistry, false);
                    else
                        cache = fstype.declareValue (comment, evalGenerator.localVarContainer, evalGenerator.classRegistry, false);
                else {
                    cache = fstype.declareValue (comment, evalGenerator.stateVarContainer, evalGenerator.classRegistry, true);
                    evalGenerator.bind (fsi.fieldSelector, cache);
                }

                fsi.cache = cache;
            }
        }
        //
        //  Generate decoders
        //
        RecordClassDescriptor []    concreteTypes = scm.concreteTypes;
        int                         numInputTypes = concreteTypes.length;
        Collection <TypeCheckInfo>  typeChecks = scm.allTypeChecks ();

        if (numInputTypes > 1) {
            JExpr                   typeIdx = getTypeIdxExpr ();
            JSwitchStatement        sw = typeIdx.switchStmt ("typeSwitch");
            boolean                 typeDependentCodeFound = false;
            
            for (int ii = 0; ii < numInputTypes; ii++) {
                RecordClassDescriptor   rcd = concreteTypes [ii];
                ClassSelectorInfo       csi = scm.getSelectorInfo (rcd);

                if (csi.hasUsedFields () || !typeChecks.isEmpty ()) {
                    sw.addCaseLabel (CTXT.intLiteral (ii), csi.type.getName ());
                    genDecoderForOneType (scm.allTypeChecks (), csi, sw, in);
                    sw.addBreak ();
                    typeDependentCodeFound = true;
                }
            }

            if (typeDependentCodeFound)
                evalGenerator.addTo.add (sw);
        }
        else
            genDecoderForOneType (
                scm.allTypeChecks (), 
                scm.getSelectorInfo (concreteTypes [0]),
                evalGenerator.addTo,
                in
            );
    }

    private void                genDecoderForOneType (
        Collection <TypeCheckInfo>  typeChecks,
        ClassSelectorInfo           csi,
        JCompoundStatement          addTo,
        JExpr                       in
    )
    {
        for (TypeCheckInfo tci : typeChecks) {
            ClassDescriptor   testClass = tci.typeCheck.checkType;

            boolean     test = 
                testClass instanceof RecordClassDescriptor &&
                ((RecordClassDescriptor) testClass).isAssignableFrom (csi.type);

            addTo.add (tci.cache.write (QBooleanType.getLiteral (test)));
        }

        QByteSkipContext            skipper = new QByteSkipContext (in, addTo);
        
        int                         numFields = csi.highestUsedIdx + 1;
        
        for (int ii = 0; ii < numFields; ii++) {
            FieldSelectorInfo       fsi = csi.fields [ii];
            QType                   type = fsi.qtype;

            addTo.addComment ("Decode field " + fsi.field.getName ());

            if (fsi.fieldSelector == null && !fsi.usedAsBase) {
                int         n = type.getEncodedFixedSize ();

                if (n != QType.SIZE_VARIABLE)
                    skipper.skipBytes (n);
                else {
                    skipper.flush ();
                    
                    JStatement      ifMore =
                        CTXT.ifStmt (
                            in.call ("hasAvail"), 
                            type.skip (in)
                        );

                    addTo.add (ifMore);                    
                }
            }
            else {
                QValue          target = fsi.cache;
                
                skipper.flush ();
                
                JStatement      action;
                
                if (fsi.relativeTo != null)
                    action = fsi.cache.decodeRelative (in, fsi.relativeTo.cache);  
                else
                    action = target.decode (in);
                
                JStatement      ifMore =
                    CTXT.ifStmt (
                        in.call ("hasAvail"), 
                        action,
                        target.writeNull ()
                    );
                
                addTo.add (ifMore);
            }
        }
    }
}
