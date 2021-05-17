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

import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import java.util.*;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.*;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.io.UncheckedIOException;
import com.epam.deltix.util.jcg.*;
import com.epam.deltix.util.jcg.scg.*;
import com.epam.deltix.util.lang.*;
import com.epam.deltix.util.memory.*;
import java.io.IOException;
import java.lang.reflect.Modifier;
import static java.lang.reflect.Modifier.*;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.*;
 
/**
 *
 */
class FilterGenerator {
    public static final String SOURCEQUERY = "sourceQuery";
    private static final String EXECUTEQUERY = "executeQuery";
    private static final String FILTER = "Filter";
    private static final String MSI = "MessageSourceImpl";
    private static final String SOURCE = "source";
    private static final String INTYPES = "inputTypes";
    private static final String OUTTYPES = "outputTypes";
    private static final String PARAMS = "params";
    
    private JClass                      filterClass;
    private JClass                      msiClass;
    private JClass                      stateClass;
    private QVariableContainer          stateVars;
    private QVariableContainer          filterVars;
    private QClassRegistry              classRegistry;
    private EvalGenerator               evalGenerator;
    private RecordClassDescriptor []    outputTypes;
    private RecordClassDescriptor []    inputTypes;
    private JMethod                     filterMethod;
    private JMethodArgument             inMsg;
    private JMethodArgument             inState;
    private JCompoundStatement          filterBody;
    private CompiledFilter              compFilter;
    private JStatement                  returnReject;
    private JStatement                  returnAccept;
    private JStatement                  returnAbort;
    private JConstructor                msic;
    
    public FilterGenerator (CompiledFilter f) {
        this.compFilter = f;
        
        Class <? extends FilterIMSImpl> baseClass;
        
        if (compFilter.groupBy == null) 
            baseClass = FlatFilterIMS.class;        
        else if (compFilter.groupBy instanceof GroupByEntity) 
            baseClass = GroupByEntityFilterIMS.class;        
        else 
            throw new UnsupportedOperationException ("group by values");        
        
        CompiledExpression              condition = compFilter.condition;
        TupleConstructor                selector = compFilter.selector;        
        boolean                         aggregate = compFilter.aggregate;
        
        outputTypes = compFilter.getConcreteOutputTypes ();
        inputTypes = compFilter.source.getConcreteOutputTypes ();
        
        SourceClassMap                  scm = new SourceClassMap (inputTypes);

        scm.discoverFieldSelectors (condition);
        scm.discoverFieldSelectors (selector);
        
        filterClass = CTXT.newClass (PUBLIC | FINAL, null, FILTER, FilterBase.class);
        msiClass = filterClass.innerClass (PRIVATE | FINAL, MSI, baseClass);

        msic = msiClass.addConstructor (PRIVATE);
        
        JMethodArgument     src = msic.addArg (FINAL, InstrumentMessageSource.class, SOURCE);
        JMethodArgument     inTypesArg = msic.addArg (FINAL, RecordClassDescriptor [].class, INTYPES);
        JMethodArgument     outTypesArg = msic.addArg (FINAL, RecordClassDescriptor [].class, OUTTYPES);
        JMethodArgument     paramsArg = msic.addArg (FINAL, ReadableValue [].class, PARAMS);

        msic.callParent (src, inTypesArg, outTypesArg, paramsArg);
                
        returnReject = msiClass.inheritedVar ("REJECT").access ().returnStmt ();
        returnAccept = msiClass.inheritedVar ("ACCEPT").access ().returnStmt ();
        returnAbort = msiClass.inheritedVar ("ABORT").access ().returnStmt ();
        
        filterMethod = msiClass.addMethod (PROTECTED, int.class, "accept");

        inMsg = filterMethod.addArg (Modifier.FINAL, RawMessage.class, "inMsg");
        
        inState = filterMethod.addArg (Modifier.FINAL, FilterState.class, "inState");
        
        filterBody = filterMethod.body ();

        stateClass = filterClass.innerClass (PRIVATE | FINAL, "State", FilterState.class);

        JMethod                 newClassMethod =
            msiClass.addMethod (PROTECTED | FINAL, stateClass, "newState");

        newClassMethod.body ().add (stateClass.newExpr ().returnStmt ());
        
        generateLimitCheck ();
        generateFirstOnlyCheck ();
        
        JLocalVariable          typedState =
            filterBody.addVar (
                FINAL, stateClass, "state",
                inState.cast (stateClass)
            );       
        
        stateVars = new QVariableContainer (PRIVATE, stateClass, typedState, "v");        
        //
        // override executeQuery()
        //
        JMethod     executeQueryMethod =
            filterClass.addMethod (PUBLIC, InstrumentMessageSource.class, EXECUTEQUERY);

        JMethodArgument options = 
            executeQueryMethod.addArg (Modifier.FINAL, SelectionOptions.class, "options");
        
        JMethodArgument params = 
            executeQueryMethod.addArg (Modifier.FINAL, ReadableValue [].class, "params");
        
        executeQueryMethod.body ().add (
            msiClass.newExpr (
                filterClass.inheritedVar (SOURCEQUERY).access ().call (EXECUTEQUERY, options, params),
                filterClass.inheritedVar (INTYPES).access (),
                filterClass.inheritedVar (OUTTYPES).access (),
                params
            ).returnStmt ()
        );

        filterVars = new QVariableContainer (FINAL, filterBody, null, "$");

        classRegistry = new QClassRegistry (filterClass.inheritedVar ("types").access ());

        evalGenerator =
            new EvalGenerator (
                params,
                inMsg,
                classRegistry,
                filterVars,
                stateVars,
                filterBody
            );
        //
        //  Select relevant data from source cursor
        //  
        SelectorGenerator   sg = 
            new SelectorGenerator (msiClass, evalGenerator) {
                @Override
                protected JExpr     getTypeIdxExpr () {
                    return (filterClass.callSuperMethod ("getInputTypeIndex"));
                }
            };
        
        sg.genSelectors (scm);
       
        //
        //  Compute the filter condition, if any
        //
        
        if (condition != null) {
            QValue      condValue = evalGenerator.genEval (condition);

            filterBody.add (
                CTXT.ifStmt (
                    QBooleanType.nullableToClean (condValue.read ()).not (),
                    returnReject
                )
            );            
        }
        
        JExpr                   outMsgInFilter;
        
        if (selector != null) {
            JInitMemberVariable     msgVar =
                stateClass.addVar (
                    Modifier.PRIVATE | Modifier.FINAL, 
                    RawMessage.class, "outMsg",
                    CTXT.newExpr (
                        RawMessage.class,
                        classRegistry.getTypeRef (selector.getClassDescriptor ())
                    )
                );
            
            JInitMemberVariable     mdoVar = 
                stateClass.addVar (
                    Modifier.PRIVATE | Modifier.FINAL, 
                    MemoryDataOutput.class, "out",
                    CTXT.newExpr (MemoryDataOutput.class)
                );
               
            JExpr                   mdoAccess = mdoVar.access (typedState);
            
            outMsgInFilter = msgVar.access (typedState);
            
            JExpr                   outMsgInState = msgVar.access ();
                
            CompiledExpression []   nsInits = selector.getNonStaticInitializers ();
            int                     n = nsInits.length;
            QValue []               nsInitVals = new QValue [n];
            
            for (int ii = 0; ii < n; ii++) 
                nsInitVals [ii] = evalGenerator.genEval (nsInits [ii]);

            //
            //  Encode the output message
            //
            CompiledExpression          tsInit = selector.getTimestampInitializer ();
            CompiledExpression          symbolInit = selector.getSymbolInitializer ();
            CompiledExpression          typeInit = selector.getTypeInitializer ();

            if (tsInit == null) {
                filterBody.add (
                        outMsgInFilter.call ("setTimeStampMs", inMsg.call("getTimeStampMs") )
                );
                
                filterBody.add (
                        outMsgInFilter.call ("setNanoTime", inMsg.call("getNanoTime") )
                );
            }
            else
                evalGenerator.genEval (
                        outMsgInFilter.call ("nullifyTimeStampMs")
                );

            if (symbolInit == null) 
                filterBody.add (
                        outMsgInFilter.call("setSymbol", inMsg.call("getSymbol"))
                );
            else 
                evalGenerator.genEval (
                        outMsgInFilter.call("setSymbol", CTXT.stringLiteral(""))
                );

//            if (typeInit == null)
//                filterBody.add (
//                        outMsgInFilter.call ("setInstrumentType", inMsg.call("getInstrumentType") )
//                );
//            else
//                evalGenerator.genEval (
//                    typeInit,
//                    new QInstrumentTypeValue (QType.INSTRUMENT_TYPE, outMsgInFilter)
//                );
             
            filterBody.add (mdoAccess.call ("reset"));
            
            for (int ii = 0; ii < n; ii++)
                nsInitVals [ii].encode (mdoAccess, filterBody);
            
            filterBody.add (outMsgInFilter.call ("setBytes", mdoAccess));
            
            if (aggregate)      // persist the symbol
                filterBody.add (
                        outMsgInFilter.call("setSymbol", outMsgInFilter.call("getSymbol").call ("toString"))
                );
            
            //
            //  Override getLastMessage
            //
            JMethod         getLastMessage = 
                stateClass.addMethod (Modifier.PROTECTED, RawMessage.class, "getLastMessage");
            
            getLastMessage.body ().add (outMsgInState.returnStmt ());
        }
        else {
            outMsgInFilter = inMsg;
        }
        
        if (compFilter.runningFilter == CompiledFilter.RunningFilter.DISTINCT) {
            // if (rms.alreadyContains (outmsg))
            //     return (false);
            JMemberVariable       rms =
                msiClass.addVar (
                    PRIVATE | FINAL,
                    RawMessageSet.class,
                    "rms",
                    CTXT.newExpr (RawMessageSet.class)
                );

            filterBody.add (
                CTXT.ifStmt (
                    rms.access ().call ("alreadyContains", outMsgInFilter),
                    returnReject
                )
            );            
        }
               
        filterBody.add (
            filterClass.inheritedVar ("outMsg").access ().assign (outMsgInFilter)
        );
        
        filterBody.add (returnAccept);
        
        if (aggregate)             
            overrideNext ("nextAggregated");                        
    }

    private JExpr               getMaxLimit () {
        long        limit = compFilter.tslimits.getInclusiveMaximum ();
        return (limit == Long.MAX_VALUE ? null : CTXT.longLiteral (limit));
    }
    
    private JExpr               getMinLimit () {
        long        limit = compFilter.tslimits.getInclusiveMinimum ();
        return (limit == Long.MIN_VALUE ? null : CTXT.longLiteral (limit));
    }
    
    private void                generateFirstOnlyCheck () {
        if (compFilter.runningFilter != CompiledFilter.RunningFilter.FIRST_ONLY)
            return;
        //
        //  If grouping by, we can never abort scan, because we may run 
        //  across new groups. Otherwise, we can simply abort after first.
        //
        JStatement      actionAfterFirst =
            compFilter.groupBy == null ? returnAbort : returnReject;
        
        filterBody.add (
            CTXT.ifStmt (inState.call ("isAccepted"), actionAfterFirst)
        );
    }
    
    private void                generateLimitCheck () {
        TimestampLimits             tslimits = compFilter.tslimits;
        
        if (tslimits == null)
            return;
        
        boolean     forward = compFilter.isForward ();
        JExpr       startLimit;
        JExpr       endLimit;
        String      op;
        
        if (forward) {
            startLimit = getMinLimit ();
            endLimit = getMaxLimit ();
            op = ">";
        }
        else {
            startLimit = getMaxLimit ();
            endLimit = getMinLimit ();
            op = "<";
        }
        
        //TODO - support parameters
        
        if (startLimit != null) {
            JMethod         adj = 
                msiClass.addMethod (Modifier.PROTECTED, long.class, "adjustResetPoint");
            
            JMethodArgument timeArg = 
                adj.addArg (Modifier.FINAL, long.class, "time");

            JExpr           inRange = 
                CTXT.binExpr (timeArg, op, startLimit);
            
            adj.body ().add (
                CTXT.condExpr (inRange, timeArg, startLimit).returnStmt ()
            );
        }

        if (endLimit != null)
            filterBody.add (
                CTXT.ifStmt (
                        CTXT.binExpr (inMsg.call ("getTimeStampMs"), op, endLimit),
                        returnAbort
                )
            );                    
    }
    
    private void                overrideNext (String delegateTo) {
        JMethod     next = msiClass.addMethod (Modifier.PUBLIC, boolean.class, "next"); 
        
        next.body ().add (msiClass.callSuperMethod (delegateTo).returnStmt ());
    }

    PreparedQuery               finish (JavaCompilerHelper helper, PreparedQuery source) {
        StringBuilder       buf = new StringBuilder ();
        SourceCodePrinter   p = new SourceCodePrinter (buf);

        try {
            p.print (filterClass);
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

        Class <?>   qclass;
        
        try {
            qclass = helper.compileClass (filterClass.name (), code);
        } catch (ClassNotFoundException x) {
            throw new RuntimeException ("unexpected", x);
        }

        FilterBase   ret;

        try {
            ret = (FilterBase) qclass.newInstance ();
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new RuntimeException ("unexpected", x);
        }

        ret.sourceQuery = source;
        ret.types = classRegistry.getTypes ();
        ret.outputTypes = outputTypes;
        ret.inputTypes = inputTypes;

        return (ret);
    }
/*
    private void                genSelectors (
        SourceClassMap              scm,
        JCompoundStatement          filterBody,
        JExpr                       inMsgX
    )
    {
        //  MemoryDataInput in = new MemoryDataInput ();
        JExpr             in =
            msiClass.addVar (
                PRIVATE, MemoryDataInput.class, "in",
                CTXT.newExpr (MemoryDataInput.class)
            ).access ();

        // in.setBytes (inMsg.data, inMsg.offset, inMsg.length);
        filterBody.add (
            in.call (
                "setBytes",
                inMsgX.field ("data"),
                inMsgX.field ("offset"),
                inMsgX.field ("length")
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
                    stateVars, 
                    classRegistry, 
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
                        cache = fstype.declareValue (comment, stateVars, classRegistry, false);
                    else
                        cache = fstype.declareValue (comment, filterVars, classRegistry, false);
                else {
                    cache = fstype.declareValue (comment, stateVars, classRegistry, true);
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
            JExpr                   typeIdx = filterClass.callSuperMethod ("getInputTypeIndex");
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
                filterBody.add (sw);
        }
        else
            genDecoderForOneType (
                scm.allTypeChecks (), 
                scm.getSelectorInfo (concreteTypes [0]),
                filterBody,
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
    */
     
}
    

