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

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.ReadableValue;
import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.OverCountExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.OverTimeExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.FilterBase;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.FilterIMSImpl;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.FilterState;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.FilterStateProvider;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.GroupByEntityFilterStateProvider;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.GroupByFilterState;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.GroupByFilterStateProvider;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.OverCountFilter;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.OverTimeFilter;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.RawMessageSet;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.SingleFilterStateProvider;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.PreparedQuery;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.io.UncheckedIOException;
import com.epam.deltix.util.jcg.JClass;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JConstructor;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JInitMemberVariable;
import com.epam.deltix.util.jcg.JLocalVariable;
import com.epam.deltix.util.jcg.JMemberVariable;
import com.epam.deltix.util.jcg.JMethod;
import com.epam.deltix.util.jcg.JMethodArgument;
import com.epam.deltix.util.jcg.JStatement;
import com.epam.deltix.util.jcg.JSwitchStatement;
import com.epam.deltix.util.jcg.scg.SourceCodePrinter;
import com.epam.deltix.util.lang.JavaCompilerHelper;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.DEBUG_DUMP_CODE;
import static java.lang.reflect.Modifier.FINAL;
import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PROTECTED;
import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;

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
    private static final String DB = "db";
    private static final String PARAMS = "params";

    private JClass                      filterClass;
    private String                      acceptMethodName = "accept";
    private JClass                      msiClass;
    private JClass                      stateClass;
    private JClass                      interimStateClass;
    private QVariableContainer          stateVars;
    private QVariableContainer          interimStateVars;
    private JMethod                     resetFunctionsMethod;
    private JCompoundStatement          resetFunctionsBody;
    private QVariableContainer          filterVars;
    private QClassRegistry              classRegistry;
    private EvalGenerator               evalGenerator;
    private RecordClassDescriptor []    outputTypes;
    private RecordClassDescriptor []    inputTypes;
    private JMethod                     filterMethod;
    private JMethod                     encodeNullMethod;
    private JMethodArgument             inMsg;
    private JMethodArgument             inState;
    private JCompoundStatement          filterBody;
    private CompiledFilter              compFilter;
    private JStatement                  returnReject;
    private JStatement                  returnAccept;
    private JStatement                  returnAbort;
    private JConstructor                msic;
    private JMethod                     initMethod;
    private JCompoundStatement          initMethodBody;
    private JInitMemberVariable         initializedVar;
    private JMethodArgument             startTime;
    private JMethodArgument             startNanoTime;
    private QValue                      conditionValue;

    public FilterGenerator (CompiledFilter f) {
        this.compFilter = f;

        // define accept method and filter base class
        Class<? extends FilterIMSImpl> baseClass;
        if (compFilter.isOverTime()) {
            baseClass = OverTimeFilter.class;
            acceptMethodName = "acceptGroupByTime";
        } else if (compFilter.isOverCount()) {
            baseClass = OverCountFilter.class;
            acceptMethodName = "acceptGroupByCount";
        } else {
            baseClass = FilterIMSImpl.class;
        }

        // define state provider class
        Class<? extends FilterStateProvider> baseStateProviderClass;
        CompiledExpression[] groupByExpressions = null;
        boolean groupBy = false;
        if (compFilter.groupBy instanceof GroupByEntity) {
            groupBy = true;
            baseStateProviderClass = GroupByEntityFilterStateProvider.class;
        } else if (compFilter.groupBy != null) {
            groupBy = true;
            baseStateProviderClass = GroupByFilterStateProvider.class;
            if (compFilter.groupBy instanceof GroupByExpressions) {
                groupByExpressions = ((GroupByExpressions) compFilter.groupBy).expressions;
            }
        } else {
            baseStateProviderClass = SingleFilterStateProvider.class;
        }

        // collect expressions
        CompiledExpression              condition = compFilter.condition;
        TupleConstructor                selector = compFilter.selector;
        boolean                         aggregate = compFilter.aggregate;

        List<CompiledExpression> allExpressions = new ArrayList<>();
        if (condition != null) {
            allExpressions.add(condition);
        }
        if (selector != null) {
            allExpressions.addAll(Arrays.asList(selector.getNonStaticInitializers()));
            allExpressions.addAll(selector.typeToCondition.values().stream()
                    .filter(Objects::nonNull).collect(Collectors.toList()));
        }
        if (groupByExpressions != null) {
            allExpressions.addAll(Arrays.asList(groupByExpressions));
        }

        outputTypes = compFilter.getConcreteOutputTypes ();
        inputTypes = compFilter.source.getConcreteOutputTypes ();

        // generate filter class
        filterClass = CTXT.newClass(PUBLIC | FINAL, null, FILTER, FilterBase.class);

        classRegistry = new QClassRegistry(filterClass.inheritedVar("types").access());

        // generate message source impl class
        msiClass = filterClass.innerClass(PRIVATE | FINAL, MSI, baseClass);
        // message source impl constructor
        msic = msiClass.addConstructor(PRIVATE);
        JMethodArgument     src = msic.addArg (FINAL, InstrumentMessageSource.class, SOURCE);
        JMethodArgument     inTypesArg = msic.addArg (FINAL, RecordClassDescriptor [].class, INTYPES);
        JMethodArgument     outTypesArg = msic.addArg (FINAL, RecordClassDescriptor [].class, OUTTYPES);
        JMethodArgument     paramsArg = msic.addArg (FINAL, ReadableValue [].class, PARAMS);
        JMethodArgument     dbArg = msic.addArg (FINAL, DXTickDB.class, DB);

        if (f.isOverTime()) {
            OverTimeExpression over = (OverTimeExpression) f.getOver();
            msic.callParent(src, inTypesArg, outTypesArg, paramsArg, dbArg, CTXT.longLiteral(over.getTimeInterval().getNanoTime()),
                    CTXT.booleanLiteral(over.isReset()), CTXT.booleanLiteral(over.isTrigger()),
                    CTXT.booleanLiteral(over.isEvery()), CTXT.booleanLiteral(f.isRunning()));
        } else if (f.isOverCount()) {
            OverCountExpression over = (OverCountExpression) f.getOver();
            msic.callParent(src, inTypesArg, outTypesArg, paramsArg, dbArg, CTXT.intLiteral(over.getCount()),
                    CTXT.booleanLiteral(over.isReset()), CTXT.booleanLiteral(f.isRunning()));
        } else {
            msic.callParent (src, inTypesArg, outTypesArg, paramsArg, dbArg);
        }

        // message source impl return values
        returnReject = msiClass.inheritedVar ("REJECT").access ().returnStmt ();
        returnAccept = msiClass.inheritedVar ("ACCEPT").access ().returnStmt ();
        returnAbort = msiClass.inheritedVar ("ABORT").access ().returnStmt ();

        // MemoryDataInput in = new MemoryDataInput ();
        JExpr in = msiClass.addVar(PRIVATE, MemoryDataInput.class, "in", CTXT.newExpr (MemoryDataInput.class)).access();

        // message source impl accept method
        filterMethod = msiClass.addMethod(PROTECTED, int.class, acceptMethodName);
        inMsg = filterMethod.addArg(Modifier.FINAL, RawMessage.class, "inMsg");
        inState = filterMethod.addArg(Modifier.FINAL, FilterState.class, "inState");
        filterBody = filterMethod.body();

        // override executeQuery()
        JMethod executeQueryMethod =
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
                        params,
                        filterClass.inheritedVar(DB).access()
                ).returnStmt ()
        );

        // filter state provider implementation
        JClass stateProviderClass = filterClass.innerClass (PRIVATE | FINAL, "FilterStateProviderImpl", baseStateProviderClass);
        JConstructor stateProviderClassConstructor = stateProviderClass.addConstructor(PUBLIC);
        JMethodArgument stateProviderClassConstructorArg = stateProviderClassConstructor.addArg(0, FilterIMSImpl.class, "filter");
        stateProviderClassConstructor.body().add(CTXT.call("super", stateProviderClassConstructorArg));

        // new state provider method
        JMethod newStateProviderMethod = msiClass.addMethod (PROTECTED | FINAL, stateProviderClass, "newStateProvider");
        JMethodArgument newStateProviderMethodArg = newStateProviderMethod.addArg(0, FilterIMSImpl.class, "filter");
        newStateProviderMethod.body().add(stateProviderClass.newExpr(newStateProviderMethodArg).returnStmt());

        // FilterState implementation class
        stateClass = filterClass.innerClass (PRIVATE | FINAL, "State", FilterState.class);
        JConstructor stateConstructor = stateClass.addConstructor(PUBLIC);
        JMethodArgument stateConstructorArg = stateConstructor.addArg(0, FilterIMSImpl.class, "filter");
        stateConstructor.body().add(CTXT.call("super", stateConstructorArg));

        // new state method
        JMethod newStateMethod = stateProviderClass.addMethod(PUBLIC, FilterState.class, "newState");
        newStateMethod.body().add(stateClass.newExpr(CTXT.staticVarRef("this", "filter")).returnStmt());

        // InterimFilterState implementation class
        interimStateClass = filterClass.innerClass(
                PUBLIC | FINAL | STATIC, "InterimState",
                baseStateProviderClass == GroupByFilterStateProvider.class ? GroupByFilterState.class : FilterState.class
        );
        JConstructor interimStateConstructor = interimStateClass.addConstructor(PUBLIC);
        JMethodArgument interimStateConstructorArg = interimStateConstructor.addArg(0, FilterIMSImpl.class, "filter");
        interimStateConstructor.body().add(CTXT.call("super", interimStateConstructorArg));
        JConstructor interimStateConstructorClassLoader = interimStateClass.addConstructor(PUBLIC);
        interimStateConstructorClassLoader.addArg(0, ClassLoader.class, "classLoader");
        interimStateConstructorClassLoader.body().add(CTXT.call("super", CTXT.nullLiteral()));
        JMethod getInterimStateMethod = msiClass.addMethod(PUBLIC, interimStateClass, "getInterimState");
        getInterimStateMethod.body().add(
                CTXT.staticVarRef("this", "interimState").returnStmt()
        );
        if (baseStateProviderClass == GroupByFilterStateProvider.class) {
            JMethod newGroupByStateMethod = stateProviderClass.addMethod(PUBLIC, GroupByFilterState.class, "newGroupByState");
            newGroupByStateMethod.body().add(interimStateClass.newExpr(CTXT.staticVarRef("this", "filter")).returnStmt());
        }

        // private InterimState interimState = new InterimState();
        JExpr interimState = msiClass.addVar(PRIVATE, interimStateClass, "interimState", CTXT.newExpr(interimStateClass, CTXT.thisLiteral())).access();

        // state variables
        JLocalVariable typedState = filterBody.addVar(
                FINAL, stateClass, "state", inState.cast(stateClass)
        );
        JExpr interimTypedState = CTXT.staticVarRef("this", "interimState");

        // filter state output message
        final JInitMemberVariable msgVar;
        final JExpr outMsgInFilter;
        if (selector != null) {
            msgVar = generateVar(
                    stateClass, RawMessage.class, "outMsg", classRegistry.getTypeRef(selector.getClassDescriptors()[0])
            );
            outMsgInFilter = msgVar.access(typedState);
        } else if (OverTimeFilter.class.isAssignableFrom(baseClass) || OverCountFilter.class.isAssignableFrom(baseClass)) {
            msgVar = generateVar(stateClass, RawMessage.class, "outMsg", classRegistry.getTypeRef(outputTypes[0]));
            outMsgInFilter = msgVar.access(typedState);
        } else if (groupBy) {
            msgVar = generateVar(stateClass, RawMessage.class, "outMsg");
            outMsgInFilter = msgVar.access(typedState);
        } else {
            outMsgInFilter = inMsg;
            msgVar = null;
        }

        // FilterState methods
        initializedVar = stateClass.addVar(PRIVATE, boolean.class, "initialized", CTXT.booleanLiteral(false));
        JMethod isInitialized = stateClass.addMethod(PROTECTED, boolean.class, "isInitialized");
        isInitialized.body().add(initializedVar.access().returnStmt());
        JMethod setInitialized = stateClass.addMethod(PROTECTED, void.class, "setInitialized");
        setInitialized.body().add(initializedVar.access().assign(CTXT.trueLiteral()));
        resetFunctionsMethod = stateClass.addMethod(PROTECTED, void.class, "resetFunctions");
        resetFunctionsBody = resetFunctionsMethod.body();

        // message source impl init method
        initMethod = msiClass.addMethod (PROTECTED, void.class, "init");
        startTime = initMethod.addArg(FINAL, long.class, "startTime");
        startNanoTime = initMethod.addArg(FINAL, long.class, "startNanoTime");
        initMethod.addArg(FINAL, stateClass, "state");
        initMethodBody = initMethod.body();


        // generate message source impl body (accept method)
        // generate start checks
        generateLimitCheck();
        generateSymbolSubscription();
        generateFirstOnlyCheck();
        initializeSelectLimits(f.limit);

        // call init method
        filterBody.add(
                CTXT.ifStmt(
                        CTXT.binExpr(
                                CTXT.binExpr(typedState, "!=", CTXT.nullLiteral()),
                                "&&",
                                typedState.call(isInitialized.name()).not()
                        ),
                        CTXT.call(initMethod.name(),
                                inMsg.call("getTimeStampMs"), inMsg.call("getNanoTime"), typedState
                        ).asStmt()
                )
        );

        // recalculate join expressions flag
        JLocalVariable recalculateJoinCachesVar = null;
        // todo: support group by
        if (baseStateProviderClass != GroupByFilterStateProvider.class && hasArrayJoins(allExpressions)) {
            recalculateJoinCachesVar = filterBody.addVar(
                    FINAL, boolean.class, "recalculateJoinCaches",
                    CTXT.staticVarRef("this", "hasWaitingMessages()").not()
            );
        }

        clearPools(recalculateJoinCachesVar);

        // create expressions generator
        filterVars = new QVariableContainer(0, filterBody, null, "$");
        stateVars = new QVariableContainer(PRIVATE, stateClass, typedState, "v");
        interimStateVars = new QVariableContainer(PRIVATE, interimStateClass, interimTypedState, "iv");
        JMemberVariable db = msiClass.inheritedVar(DB);
        evalGenerator = new EvalGenerator(
                params,
                inMsg,
                classRegistry,
                filterVars,
                stateVars,
                interimStateVars,
                filterBody,
                msiClass,
                initMethodBody,
                resetFunctionsBody,
                startTime,
                startNanoTime,
                outMsgInFilter,
                db.access()
        );
        evalGenerator.prepare(allExpressions);

        SourceClassMap scm = new SourceClassMap(inputTypes);
        allExpressions.forEach(scm::discoverFieldSelectors);

        generateSelectors(recalculateJoinCachesVar, scm, in);
        generateGroupBy(compFilter.groupBy, typedState, interimTypedState);
        generateJoinCachedValues(recalculateJoinCachesVar, condition, allExpressions);
        generateCondition(condition, filterBody);
        generateEncodeNull(baseClass);

        // generate encoder code
        if (selector != null) {
            JInitMemberVariable mdoVar = generateVar(stateClass, MemoryDataOutput.class, "out");
            JExpr outMsgInState = msgVar.access();
            JExpr mdoAccess = mdoVar.access(typedState);
            JMethod getOut = stateClass.addMethod(PROTECTED, MemoryDataOutput.class, "getOut");
            getOut.addAnnotation(CTXT.annotation(Override.class));
            getOut.body().add(mdoVar.access().returnStmt());

            CompiledExpression[] nsInits = selector.getNonStaticInitializers();
            int n = nsInits.length;
            QValue[] nsInitVals = new QValue[n];

            for (int ii = 0; ii < n; ii++) {
                nsInitVals[ii] = evalGenerator.genEval(nsInits[ii]);
            }

            //
            //  Encode the output message
            //
            CompiledExpression          tsInit = selector.getTimestampInitializer ();
            CompiledExpression          symbolInit = selector.getSymbolInitializer ();
            //CompiledExpression          typeInit = selector.getTypeInitializer ();

            if (tsInit == null) {
                filterBody.add(outMsgInFilter.call("setTimeStampMs", inMsg.call("getTimeStampMs")));
                filterBody.add(outMsgInFilter.call("setNanoTime", inMsg.call("getNanoTime")));
            } else {
                evalGenerator.genEval(outMsgInFilter.call("nullifyTimeStampMs"));
            }

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
//                        typeInit,
//                        new QInstrumentTypeValue (QType.INSTRUMENT_TYPE, outMsgInFilter)
//                );

            filterBody.add(mdoAccess.call("reset"));

            if (selector.getClassDescriptors().length > 1) {
                generateEncodersForMultipleTypes(selector, mdoAccess, filterBody);
            } else {
                for (int ii = 0; ii < n; ii++) {
                    nsInitVals[ii].encode(mdoAccess, filterBody);
                }
            }

            filterBody.add(outMsgInFilter.call("setBytes", mdoAccess));

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
        } else if (OverTimeFilter.class.isAssignableFrom(baseClass) || OverCountFilter.class.isAssignableFrom(baseClass)) {
            filterBody.add(outMsgInFilter.call("setTimeStampMs", inMsg.call("getTimeStampMs")));
            filterBody.add(outMsgInFilter.call("setNanoTime", inMsg.call("getNanoTime")));
            filterBody.add(outMsgInFilter.call("setSymbol", inMsg.call("getSymbol")));

            JInitMemberVariable mdoVar = generateVar(stateClass, MemoryDataOutput.class, "out");
            JExpr outMsgInState = msgVar.access();
            JMethod getLastMessage = stateClass.addMethod (Modifier.PROTECTED, RawMessage.class, "getLastMessage");
            getLastMessage.addAnnotation(CTXT.annotation(Override.class));
            getLastMessage.body ().add (outMsgInState.returnStmt ());
            JExpr mdoAccess = mdoVar.access(typedState);
            JMethod getOut = stateClass.addMethod(PROTECTED, MemoryDataOutput.class, "getOut");
            getOut.addAnnotation(CTXT.annotation(Override.class));
            getOut.body().add(mdoVar.access().returnStmt());
            filterBody.add(mdoAccess.call("reset", CTXT.intLiteral(0)));
            filterBody.add(mdoAccess.call("write", inMsg.field("data"), inMsg.field("offset"), inMsg.field("length")));
            filterBody.add(outMsgInFilter.call("setBytes", mdoAccess));
        } else if (groupBy) {
            filterBody.add(outMsgInFilter.call("copyFrom", inMsg));
            JMethod getLastMessage = stateClass.addMethod(Modifier.PROTECTED, RawMessage.class, "getLastMessage");
            getLastMessage.addAnnotation(CTXT.annotation(Override.class));
            getLastMessage.body().add(msgVar.access().returnStmt());
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

        filterBody.add (filterClass.inheritedVar ("outMsg").access ().assign (outMsgInFilter));

        filterBody.add(returnAccept);

        initMethodBody.add(typedState.call(setInitialized.name()));

        if (aggregate && (baseClass == FilterIMSImpl.class))
            overrideNext("nextAggregated");
    }

    private void initializeSelectLimits(SelectLimit limit) {
        if (limit != null) {
            filterBody.add(
                    CTXT.call(
                            "setLimit",
                            CTXT.longLiteral(limit.getLimit()),
                            CTXT.longLiteral(limit.getOffset())
                    )
            );
        }
    }

    private void clearPools(JLocalVariable recalculateCachesVar) {
        if (recalculateCachesVar != null) {
            filterBody.add(
                    CTXT.ifStmt(recalculateCachesVar, CTXT.call("clearPools").asStmt())
            );
        } else {
            filterBody.add(CTXT.call("clearPools"));
        }
    }

    private JInitMemberVariable generateVar(JClass jClass, Class<?> type, String name, JExpr... args) {
        return jClass.addVar(
                Modifier.PRIVATE | Modifier.FINAL,
                type, name,
                CTXT.newExpr(type, args)
        );
    }

    private void generateSelectors(JLocalVariable recalculateCachesVar, SourceClassMap scm, JExpr inVar) {
        JCompoundStatement outStatement = CTXT.compStmt();
        if (recalculateCachesVar != null) {
            evalGenerator.startAddTo(outStatement);
        }

        SelectorGenerator sg = new SelectorGeneratorForRootObjects(msiClass, evalGenerator, scm, filterClass, inVar);
        sg.genSelectors();

        if (recalculateCachesVar != null) {
            evalGenerator.endAddTo();
            filterBody.add(CTXT.ifStmt(recalculateCachesVar, outStatement));
        }
    }

    private void generateGroupBy(GroupBySpec groupBySpec, JLocalVariable typedState, JExpr interimTypedState) {
        if (groupBySpec instanceof GroupByExpressions) {
            new GroupByStateGenerator(classRegistry, evalGenerator)
                    .generate((GroupByExpressions) groupBySpec, interimStateClass, interimTypedState);

            evalGenerator.addTo.add(
                    CTXT.ifStmt(
                            CTXT.binExpr(typedState, "==", CTXT.nullLiteral()),
                            CTXT.intLiteral(0).returnStmt()
                    )
            );
        }
    }

    private void generateCondition(CompiledExpression condition, JCompoundStatement addTo) {
        if (condition != null && conditionValue == null) {
            conditionValue = evalGenerator.genEval(condition);
            addTo.add(
                    CTXT.ifStmt(
                            QBooleanType.nullableToClean(conditionValue.read()).not(),
                            returnReject
                    )
            );
        }
    }

    private void generateEncodeNull(Class<?> baseClass) {
        if (OverTimeFilter.class.isAssignableFrom(baseClass)) {
            JMethod encodeNull = msiClass.addMethod(PROTECTED, void.class, "encodeNull");
            encodeNull.addAnnotation(CTXT.annotation(Override.class));
            JMethodArgument mdo = encodeNull.addArg(FINAL, MemoryDataOutput.class, "mdo");
            JCompoundStatement encodeNullBody = encodeNull.body();
            for (DataField field : outputTypes[0].getFields()) {
                if (!field.getType().isNullable()) {
                    field.getType().setNullable(true);
                }
                QType<?> type = QType.forDataType(field.getType());
                type.encodeNull(mdo, encodeNullBody);
            }
        }
    }

    private void generateJoinCachedValues(JLocalVariable recalculateCachesVar, CompiledExpression condition, List<CompiledExpression> expressions) {
        if (recalculateCachesVar != null) {
            List<CompiledExpression> cachedExpressions = new ArrayList<>();
            expressions.forEach(expression -> {
                if (canCache(expression)) {
                    cachedExpressions.add(expression);
                } else {
                    extractJoinCachedExpressions(expression, cachedExpressions);
                }
            });

            JCompoundStatement outStatement = CTXT.compStmt();
            evalGenerator.startAddTo(outStatement);
            cachedExpressions.forEach(expression -> {
                if (expression.equals(condition)) {
                    generateCondition(condition, outStatement);
                } else {
                    evalGenerator.genEval(expression);
                }
            });
            evalGenerator.endAddTo();
            if (!outStatement.isEmpty()) {
                filterBody.add(CTXT.ifStmt(recalculateCachesVar, outStatement));
            }
        }
    }

    private void generateEncodersForMultipleTypes(TupleConstructor selector, JExpr mdoAccess, JCompoundStatement addTo) {
        Map<RecordClassDescriptor, List<QValue>> typeToQValue = new LinkedHashMap<>();
        selector.typeToExpressions.forEach((descriptor, compiledExpressions) -> {
            List<QValue> qValues = new ArrayList<>();
            for (CompiledExpression<?> compiledExpression : compiledExpressions) {
                qValues.add(evalGenerator.genEval(compiledExpression));
            }
            typeToQValue.put(descriptor, qValues);
        });

        List<JExpr> cond = new ArrayList<>();
        List<JStatement> then = new ArrayList<>();
        JCompoundStatement els;

        // outputTypeId for non-record selectors
        JLocalVariable outputTypeId = selector.typeToCondition.size() > 0 ? null :
                addTo.addVar(0, int.class, "outputTypeId", filterClass.callSuperMethod("getInputTypeIndex"));
        typeToQValue.forEach((type, values) -> {
            if (outputTypeId != null) {
                cond.add(
                        CTXT.binExpr(
                                outputTypeId,
                                "==",
                                CTXT.intLiteral(classRegistry.typeIndex(type))
                        )
                );
            } else {
                CompiledExpression<?> condition = selector.typeToCondition.get(type);
                if (condition != null) {
                    cond.add(
                            CTXT.binExpr(
                                    evalGenerator.genEval(condition).read(),
                                    "==",
                                    CTXT.intLiteral(1).cast(byte.class)
                            )
                    );
                } else {
                    return;
                }
            }

            JCompoundStatement thenStatement = CTXT.compStmt();
            if (values.size() > 0) {
                thenStatement.add(CTXT.staticVarRef("state.outMsg", "type").assign(classRegistry.getTypeRef(type)));
                for (QValue value : values) {
                    value.encode(mdoAccess, thenStatement);
                }
            } else {
                thenStatement.add(filterClass.inheritedVar("outMsg").access().assign(inMsg));
                thenStatement.add(returnAccept);
            }
            then.add(thenStatement);
        });
        els = CTXT.compStmt();
        els.add(returnReject);

        addTo.add(CTXT.ifStmt(cond, then, els));
    }

    private boolean hasArrayJoins(List<CompiledExpression> expressions) {
        for (CompiledExpression compiledExpression : expressions) {
            if (dependsOnArrayJoin(compiledExpression)) {
                return true;
            }
        }

        return false;
    }

    private boolean canCache(CompiledExpression expression) {
        return !dependsOnArrayJoin(expression) && !dependsOnFunction(expression);
    }

    private boolean dependsOnArrayJoin(CompiledExpression expression) {
        if (expression instanceof ArrayJoinElement) {
            return true;
        }

        if (expression instanceof CompiledComplexExpression) {
            CompiledComplexExpression compiledComplexExpression = (CompiledComplexExpression) expression;
            for (int i = 0; i < compiledComplexExpression.args.length; ++i) {
                if (dependsOnArrayJoin(compiledComplexExpression.args[i])) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean dependsOnFunction(CompiledExpression expression) {
        if (expression instanceof PluginSimpleFunction || expression instanceof PluginStatefulFunction) {
            return true;
        }

        if (expression instanceof CompiledComplexExpression) {
            CompiledComplexExpression compiledComplexExpression = (CompiledComplexExpression) expression;
            for (int i = 0; i < compiledComplexExpression.args.length; ++i) {
                if (dependsOnFunction(compiledComplexExpression.args[i])) {
                    return true;
                }
            }
        }

        return false;
    }

    private void extractJoinCachedExpressions(CompiledExpression expression, List<CompiledExpression> cachedExpressions) {
        if (expression instanceof ArrayJoinElement) {
            ArrayJoinElement arrayJoinElement = (ArrayJoinElement) expression;
            cachedExpressions.addAll(Arrays.asList(arrayJoinElement.args));
            return;
        }

        if (expression instanceof CompiledComplexExpression) {
            CompiledComplexExpression compiledComplexExpression = (CompiledComplexExpression) expression;
            for (int i = 0; i < compiledComplexExpression.args.length; ++i) {
                extractJoinCachedExpressions(compiledComplexExpression.args[i], cachedExpressions);
            }
        }
    }

    private JExpr getMaxLimit() {
        long limit = compFilter.tslimits.getInclusiveMaximum();
        return (limit == Long.MAX_VALUE ? null : CTXT.longLiteral(limit));
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

    private void generateSymbolSubscription() {
        SymbolLimits symbolLimits = compFilter.symbolLimits;
        if (symbolLimits == null || symbolLimits.symbols().size() == 0) {
            return;
        }

        JMethod adj = msiClass.addMethod(Modifier.PROTECTED, String[].class, "symbolsToAdjust");
        adj.body().add(
                CTXT.newArrayExpr(
                        String.class,
                        symbolLimits.symbols().stream().map(CTXT::stringLiteral)
                                .toArray(JExpr[]::new)
                ).returnStmt ()
        );
    }

    private void                overrideNext (String delegateTo) {
        JMethod     next = msiClass.addMethod (Modifier.PUBLIC, boolean.class, "next");

        next.body ().add (msiClass.callSuperMethod (delegateTo).returnStmt ());
    }

    PreparedQuery               finish (JavaCompilerHelper helper, PreparedQuery source, DXTickDB db) {
        StringBuilder       buf = new StringBuilder ();
        SourceCodePrinter   p = new SourceCodePrinter (buf);

        try {
            p.print (filterClass);
        } catch (IOException iox) {
            throw new UncheckedIOException(iox);
        }

        String      code = buf.toString ();

        if (DEBUG_DUMP_CODE) {
            try {
                IOUtil.dumpWithLineNumbers (code, System.out);
                System.out.println(code);
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
            ret = (FilterBase) qclass.getDeclaredConstructor().newInstance();
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new RuntimeException ("unexpected", x);
        }

        ret.sourceQuery = source;
        ret.types = classRegistry.getTypes ();
        ret.outputTypes = outputTypes;
        ret.inputTypes = inputTypes;
        ret.setDb(db);

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