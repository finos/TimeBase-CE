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

import com.epam.deltix.qsrv.hf.pub.md.ArrayDataType;
import com.epam.deltix.qsrv.hf.pub.md.ClassDataType;
import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.values.ValueBean;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.NumericType;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.functions.FirstFunctionDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.functions.FunctionInfoDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.functions.StatefulFunctionDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.functions.StatelessFunctionDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.ARRT;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.QRT;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.Varchar;
import com.epam.deltix.util.jcg.JArrayInitializer;
import com.epam.deltix.util.jcg.JClass;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JInitMemberVariable;
import com.epam.deltix.util.jcg.JLocalVariable;
import com.epam.deltix.util.jcg.JStatement;
import com.epam.deltix.util.jcg.JSwitchStatement;
import com.epam.deltix.util.jcg.JVariable;
import com.epam.deltix.util.jcg.scg.JTypeImpl;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.primitiveWrapper;

/**
 *  Generates the accept() method.
 */
class EvalGenerator {

    private static class EvalContext {
        private final JExpr iterator;
        private final JExpr position;
        private final JExpr length;
        private final int depth;

        public EvalContext(JExpr iterator, JExpr position, JExpr length, int depth) {
            this.iterator = iterator;
            this.position = position;
            this.length = length;
            this.depth = depth;
        }

        public JExpr getIterator() {
            return iterator;
        }

        public JExpr getPosition() {
            return position;
        }

        public JExpr getLength() {
            return length;
        }

        public int getDepth() {
            return depth;
        }
    }

    final JExpr                             params;
    final JExpr                             inMsg;
    final QClassRegistry                    classRegistry;
    final QVariableContainer                localVarContainer;
    final QVariableContainer                stateVarContainer;
    final QVariableContainer                interimStateVarContainer;
    final JCompoundStatement                output;
    final JClass msiClass;
    final Map<CompiledExpression<DataType>, Set<FieldAccessor>> fieldsMap = new HashMap<>();
    final Set<ArrayJoinElement>             arrayJoinElements = new HashSet<>();
    final EvalContext context;
    final JCompoundStatement initMethod;
    final JCompoundStatement resetFunctionsMethod;
    final JExpr initStartTime;
    final JExpr initStartNanoTime;
    final JExpr outMsg;
    final JExpr db;
    JCompoundStatement addTo;
    private boolean stateVars;

    private static int ARRAY_VAR_COUNTER = 0;
    private static final String ARRAY_VAR_PREFIX = "array";

    private final Map <CompiledExpression, QValue>  x2v =
        new HashMap <CompiledExpression, QValue> ();

    public EvalGenerator (
        JExpr                   params,
        JExpr                   inMsg,
        QClassRegistry          classRegistry,
        QVariableContainer      localVarContainer,
        QVariableContainer      stateVarContainer,
        QVariableContainer interimStateVarContainer,
        JCompoundStatement      output,
        JClass                  msiClass,
        JCompoundStatement      initMethod,
        JCompoundStatement      resetFunctionsMethod,
        JExpr                   initStartTime,
        JExpr                   initStartNanoTime,
        JExpr                   outMsg,
        JExpr        db
    )
    {
        this(params, inMsg, classRegistry, localVarContainer, stateVarContainer, interimStateVarContainer, output, msiClass,
            initMethod, resetFunctionsMethod, initStartTime, initStartNanoTime, outMsg, db, null);
    }

    public EvalGenerator (
        JExpr                   params,
        JExpr                   inMsg,
        QClassRegistry          classRegistry,
        QVariableContainer      localVarContainer,
        QVariableContainer      stateVarContainer,
        QVariableContainer interimStateVarContainer,
        JCompoundStatement      output,
        JClass                  msiClass,
        JCompoundStatement      initMethod,
        JCompoundStatement      resetFunctionsMethod,
        JExpr                   initStartTime,
        JExpr                   initStartNanoTime,
        JExpr                   outMsg,
        JExpr                   db,
        EvalContext context
    )
    {
        this.output = output;
        this.addTo = output;
        this.params = params;
        this.inMsg = inMsg;
        this.classRegistry = classRegistry;
        this.localVarContainer = localVarContainer;
        this.stateVarContainer = stateVarContainer;
        this.interimStateVarContainer = interimStateVarContainer;
        this.msiClass = msiClass;
        this.initMethod = initMethod;
        this.resetFunctionsMethod = resetFunctionsMethod;
        this.initStartTime = initStartTime;
        this.initStartNanoTime = initStartNanoTime;
        this.context = context;
        this.outMsg = outMsg;
        this.db = db;
    }

    public void prepare(List<CompiledExpression> expressions) {
        collectObjectFieldSelectors(expressions);
        collectArrayJoinElements(expressions);
    }

    public void startAddTo(JCompoundStatement addTo) {
        this.addTo = addTo;
        this.stateVars = true;
    }

    public void endAddTo() {
        this.addTo = output;
        this.stateVars = false;
    }

    private boolean isStateful(CompiledExpression<?> expression) {
        if (expression instanceof PluginFunction) {
            PluginFunction pluginFunction = (PluginFunction) expression;
            return pluginFunction.fd.isAggregate();
        } else if (expression instanceof PluginStatefulFunction) {
            return true;
        }

        return false;
    }

    private QVariableContainer getStateVarContainer(CompiledExpression<?> expression) {
        return isStateful(expression) ? stateVarContainer : interimStateVarContainer;
    }

    public void                 bind (CompiledExpression e, QValue v) {
        x2v.put (e, v);
    }

    private void                add (JStatement s) {
        addTo.add (s);
    }

    private void                move (
        QValue                      from,
        QValue                      to
    )
    {
        QCodeGenerator.move (from, to, addTo);
    }

    public void  genEval (JExpr jExpr)
    {
        addTo.add(jExpr);
    }

    public QValue genEval(CompiledExpression<?> e, Class<?> type) {
        return genEval(e, type, true);
    }

    public QValue genEval(CompiledExpression<?> e, Class<?> type, boolean generateNoCache) {
        QValue value = getFromCache(e, type);

        if (value == null) {
            QType qType = QType.forExpr(e);

            value = qType.declareValue(
                    "Result of " + e,
                    stateVars || qType.instanceAllocatesMemory() ? getStateVarContainer(e) : localVarContainer,
                    classRegistry, false
            );

            if (generateNoCache) {
                genEvalNoCache(e, value);
            }

            x2v.put(e, value);
        }

        return (value);
    }

    public QValue genEval(CompiledExpression e) {
        return genEval(e, true);
    }

    public QValue genEval(CompiledExpression e, boolean generateNoCache) {
        QValue value = getFromCache(e);

        if (value == null) {
            QType type = QType.forExpr(e);

            value = type.declareValue(
                "Result of " + e,
                stateVars || type.instanceAllocatesMemory() ? getStateVarContainer(e) : localVarContainer,
                classRegistry, false
            );

            if (generateNoCache) {
                genEvalNoCache(e, value);
            }

            x2v.put(e, value);
        }

        return (value);
    }

    public void                 genEval (
        CompiledExpression          e,
        QValue                      outValue
    )
    {
        QValue      value = getFromCache (e);

        if (value != null) {
            if (outValue != value)
                move (value, outValue);
        } else {
            QValue tempValue = genEval(e);
            move(tempValue, outValue);
        }
    }

    private QValue getFromCache(CompiledExpression<?> e) {
        QValue value = x2v.get(e);

        if (value == null && e instanceof CompiledConstant && canInline((CompiledConstant) e)) {
            value = genInlineCompiledConstant((CompiledConstant) e);
            x2v.put(e, value);
        } else if (value == null && e instanceof CompiledArrayConstant<?, ?, ?>) {
            value = genArrayConstant((CompiledArrayConstant<?, ?, ?>) e);
            x2v.put(e, value);
        }

        return (value);
    }

    private QValue getFromCache(CompiledExpression<?> e, Class<?> type) {
        QValue value = x2v.get(e);

        if (value == null && e instanceof CompiledConstant && canInline((CompiledConstant) e)) {
            value = genInlineCompiledConstant((CompiledConstant) e, type);
            x2v.put(e, value);
        } else if (value == null && e instanceof CompiledArrayConstant<?, ?, ?>) {
            value = genArrayConstant((CompiledArrayConstant<?, ?, ?>) e);
            x2v.put(e, value);
        }

        return (value);
    }

    private void                genEvalNoCache (
        CompiledExpression          e,
        QValue                      outValue
    ) {
        if (e instanceof CompiledConstant)
            genCompiledConstantEval((CompiledConstant) e, outValue);
        else if (e instanceof UnaryExpression)
            genUnaryOperation((UnaryExpression) e, outValue);
        else if (e instanceof BinaryExpression)
            genBinaryOperation((BinaryExpression) e, outValue);
        else if (e instanceof SimpleFunction)
            genSimpleFunctionEval((SimpleFunction) e, outValue);
        else if (e instanceof PluginFunction)
            genPluginFunctionEval((PluginFunction) e, outValue);
        else if (e instanceof PluginSimpleFunction)
            genPluginSimpleFunctionEval((PluginSimpleFunction) e, outValue);
        else if (e instanceof PluginStatefulFunction)
            genPluginFunction((PluginStatefulFunction) e, outValue);
        else if (e instanceof TupleConstructor)
            genTupleConstructorEval((TupleConstructor) e, outValue);
        else if (e instanceof ParamAccess)
            genParamAccessEval((ParamAccess) e, outValue);
        else if (e instanceof SymbolSelector)
            genSymbolSelectorEval((SymbolSelector) e, outValue);
        else if (e instanceof TimestampSelector)
            genTimestampSelectorEval((TimestampSelector) e, outValue);
        else if (e instanceof CompiledFilter)
            genCompiledFilterEval((CompiledFilter) e, outValue);
        else if (e instanceof ConnectiveExpression)
            genContainsExpression((ConnectiveExpression) e, outValue);
        else if (e instanceof FieldAccessor) {
            genFieldAccessor((FieldAccessor) e, outValue);
        } else if (e instanceof ThisSelector) {
            genThisSelector((ThisSelector) e, outValue);
        } else if (e instanceof TypeCheck) {
            genTypeCheck((TypeCheck) e, outValue);
        } else if (e instanceof ArrayIndexer) {
            genArrayIndexer((ArrayIndexer) e, outValue);
        } else if (e instanceof ArrayBooleanIndexer) {
            genArrayBooleanIndexer((ArrayBooleanIndexer) e, outValue);
        } else if (e instanceof ArrayIntegerIndexer) {
            genArrayIntegerIndexer((ArrayIntegerIndexer) e, outValue);
        } else if (e instanceof ArrayPredicate) {
            genArrayPredicate((ArrayPredicate) e, outValue);
        } else if (e instanceof Predicate) {
            genPredicate((Predicate) e, outValue);
        } else if (e instanceof PredicateIterator) {
            genPredicateIterator((PredicateIterator) e, outValue);
        } else if (e instanceof PredicateFunction) {
            genPredicateFunction((PredicateFunction) e, outValue);
        } else if (e instanceof ArraySlice) {
            genArraySlice((ArraySlice) e, outValue);
        } else if (e instanceof ArrayJoinElement) {
            genArrayJoinElement((ArrayJoinElement) e, outValue);
        } else if (e instanceof CastClassType) {
            genCastClassType((CastClassType) e, outValue);
        } else if (e instanceof CastArrayClassType) {
            genCastArrayType((CastArrayClassType) e, outValue);
        } else if (e instanceof CastPrimitiveType) {
            genCastPrimitiveType((CastPrimitiveType) e, outValue);
        } else if (e instanceof CompiledNullConstant) {
            genCompiledNullConstant((CompiledNullConstant) e, outValue);
        } else if (e instanceof CompiledIfExpression) {
            genCompiledIfExpression((CompiledIfExpression) e, outValue);
        } else if (e instanceof CompiledCaseExpression) {
            genCompiledCaseExpression((CompiledCaseExpression) e, outValue);
        } else {
            throw new UnsupportedOperationException(e.getClass().getName());
        }
    }

    private void genCompiledNullConstant(CompiledNullConstant e, QValue outValue) {
        addTo.add(outValue.writeNull());
    }

    private void genFieldAccessor(FieldAccessor fieldAccessor, QValue outValue) {
        QValue qValue = genEval(fieldAccessor.parent);

        boolean fetchNulls = fieldAccessor.fetchNulls;
        DataType classType = fieldAccessor.parent.type;

        RecordClassDescriptor[] descriptors;
        boolean array = false;
        if (classType instanceof ClassDataType) {
            descriptors = ((ClassDataType) classType).getDescriptors();
        } else if (classType instanceof ArrayDataType) {
            array = true;
            descriptors = ((ClassDataType) ((ArrayDataType) classType).getElementDataType()).getDescriptors();
        } else {
            return;
        }

        Set<FieldAccessor> accessors = fieldsMap.get(fieldAccessor.parent);
        SourceClassMap scm = new SourceClassMap(descriptors);
        accessors.forEach(accessor -> {
            if (accessor.fetchNulls == fetchNulls) {
                if (accessor.getSourceFieldName().equalsIgnoreCase(fieldAccessor.getSourceFieldName())) {
                    if (accessor.equals(fieldAccessor)) {
                        scm.discoverFieldAccessors(accessor);
                    }
                } else {
                    scm.discoverFieldAccessors(accessor);
                }
            }
        });
        scm.forEachField((fsi) -> {
            if (fieldAccessor.equals(fsi.fieldAccessor)) {
                fsi.cache = outValue;
            } else {
                for (FieldAccessor accessor : accessors) {
                    if (accessor.equals(fsi.fieldAccessor)) {
                        fsi.cache = genEval(accessor, false);
                    }
                }
            }
        });

        FieldAccessGenerator sg = array ?
            new FieldAccessGeneratorForArrays(msiClass, this, scm, qValue, fetchNulls) :
            new FieldAccessGeneratorForObjects(msiClass, this, scm, qValue);

        sg.genSelectors();
    }

    private void genThisSelector(ThisSelector thisSelector, QValue outValue) {
        JExpr inVar = msiClass.getVar("in").access();
        addTo.add(
            inVar.call("setBytes", inMsg.field("data"), inMsg.field("offset"), inMsg.field("length"))
        );
        addTo.add(
            outValue.read().call("set", CTXT.staticVarRef("super", "getInputTypeIndex()"), inVar)
        );
    }

    private void genTypeCheck(TypeCheck typeCheck, QValue outValue) {
        QValue qValue = genEval(typeCheck.args[0]);

        DataType type = typeCheck.args[0].type;
        if (type instanceof ClassDataType) {
            ClassDataType classDataType = (ClassDataType) type;
            RecordClassDescriptor[] descriptors = classDataType.getDescriptors();
            int num = -1;
            for (int i = 0; i < descriptors.length; ++i) {
                if (typeCheck.checkType.equals(descriptors[i])) {
                    num = i;
                    break;
                }
            }

            if (num >= 0) {
                this.addTo.add(
                    outValue.write(
                        CTXT.condExpr(
                            CTXT.binExpr(qValue.read().call("typeId"), "==", CTXT.intLiteral(num)),
                            CTXT.intLiteral(1).cast(byte.class),
                            CTXT.intLiteral(0).cast(byte.class)
                        )
                    )
                );

                return;
            }
        }

        this.addTo.add(outValue.write(CTXT.intLiteral(0).cast(byte.class)));
    }

    private void genArrayIndexer(ArrayIndexer e, QValue outValue) {
        QValue array = genEval(e.compiledSelector);
        QValue indexValue = genEval(e.compiledPredicate);

        this.addTo.add(
            outValue.write(
                CTXT.staticCall(
                    ARRT.class, "indexOf", array.read(),
                    CTXT.staticCall(Conversions.class, "int32", indexValue.read())
                )
            )
        );
    }

    private void genArrayBooleanIndexer(ArrayBooleanIndexer e, QValue outValue) {
        QValue array = genEval(e.compiledSelector);
        QValue indexValue = genEval(e.compiledPredicate);

        QArrayValue arrayValue = (QArrayValue) outValue;
        this.addTo.add(arrayValue.setInstance());
        this.addTo.add(
            CTXT.ifStmt(
                CTXT.staticCall(ARRT.class, "copyIf", array.read(), outValue.read(), indexValue.read()).not(),
                arrayValue.setNull().asStmt()
            )
        );
        this.addTo.add(arrayValue.setChanged());
    }

    private void genArrayIntegerIndexer(ArrayIntegerIndexer e, QValue outValue) {
        QArrayValue in = (QArrayValue) genEval(e.compiledSelector);
        QArrayValue indices = (QArrayValue) genEval(e.compiledPredicate);
        QArrayValue out = (QArrayValue) outValue;

        /*
        if (in.isNull() || indices.isNull()) {
            out.setNull();
        } else {
            out.setInstance();
            out.get().clear();
            for (int i = 0; i < indices.get().size(); ++i) {
                int index = (int) indices.getByte(i);
                if (index < 0) {
                    index = in.get().size() + index;
                }

                if (index >= 0 && index < in.get().size()) {
                    out.add(in.get().getByte(index));
                }
//                else {
//                    out.addNull();
//                }
            }
        }
         */

        JCompoundStatement elseStatements = CTXT.compStmt();
        elseStatements.add(out.setInstance());
        elseStatements.add(out.read().call("clear"));
        JLocalVariable len = elseStatements.addVar(Modifier.FINAL, int.class, "$len$", indices.size());
        JLocalVariable i = elseStatements.addVar(0, int.class, "$i$");

        JCompoundStatement forStatemens = CTXT.compStmt();
        JLocalVariable index = forStatemens.addVar(
            0, int.class, "$index$", CTXT.staticCall(Conversions.class, "int32", indices.getElement(i))
        );
        forStatemens.add(
            CTXT.ifStmt(
                CTXT.binExpr(index, "<", CTXT.intLiteral(0)),
                index.assign(CTXT.binExpr(in.size(), "+", index))
            )
        );
        forStatemens.add(
            CTXT.ifStmt(
                CTXT.binExpr(
                    CTXT.binExpr(index, ">=", CTXT.intLiteral(0)),
                    "&&",
                    CTXT.binExpr(index, "<", in.size())
                ),
                out.write(in.getElement(index)),
                out.addNull()
            )
        );

        elseStatements.add(
            CTXT.forStmt(
                i.assignExpr(CTXT.intLiteral(0)),
                CTXT.binExpr(i, "<", len),
                i.getAndInc(),
                forStatemens
            )
        );

        this.addTo.add(
            CTXT.ifStmt(
                CTXT.binExpr(in.isNull(), "||", indices.isNull()),
                out.setNull().asStmt(),
                elseStatements
            )
        );
        this.addTo.add(out.setChanged());
    }

    private void genArrayPredicate(ArrayPredicate e, QValue outValue) {
        QValue array = genEval(e.compiledSelector);

        QArrayValue arrayValue = (QArrayValue) array;

        JCompoundStatement statements = CTXT.compStmt();
        JCompoundStatement forStatements = CTXT.compStmt();

        int depth = context != null ? context.getDepth() + 1 : 1;

        JLocalVariable len = statements.addVar(Modifier.FINAL, int.class, "$arrayLen$" + depth, arrayValue.size());
        JLocalVariable ii = statements.addVar(0, int.class, "$arrayI$" + depth);

        QVariableContainer localVarContainer = new QVariableContainer (0, statements, null, "$" + depth + "$");
        EvalGenerator evalGenerator = new EvalGenerator(
            params, inMsg, classRegistry,
            localVarContainer, stateVarContainer, interimStateVarContainer,
            forStatements,
            msiClass,
            initMethod, resetFunctionsMethod, initStartTime, initStartNanoTime, outMsg, db,
            new EvalContext(arrayValue.getElement(ii), ii, len, depth)
        );

        evalGenerator.collectObjectFieldSelectors(Arrays.asList(e.compiledPredicate));

        QValue valuePredicate = evalGenerator.genEval(e.compiledPredicate);
        forStatements.add(
            CTXT.ifStmt(
                CTXT.binExpr(valuePredicate.read(), "==", CTXT.intLiteral(1).cast(byte.class)),
                outValue.write(arrayValue.getElement(ii))
            )
        );

        statements.add(
            CTXT.forStmt(
                ii.assignExpr(CTXT.intLiteral(0)),
                CTXT.binExpr(ii, "<", len),
                ii.getAndInc(),
                forStatements
            )
        );

        this.addTo.add(outValue.writeNull());
        this.addTo.add(
            CTXT.ifStmt(
                arrayValue.isNull().not(), statements
            )
        );
    }

    private void genPredicate(Predicate e, QValue outValue) {
        QValue object = genEval(e.compiledSelector);

        JCompoundStatement statements = CTXT.compStmt();

        int depth = context != null ? context.getDepth() + 1 : 1;

        QVariableContainer localVarContainer = new QVariableContainer (0, statements, null, "$" + depth + "$");
        EvalGenerator evalGenerator = new EvalGenerator(
            params, inMsg, classRegistry,
            localVarContainer, stateVarContainer, interimStateVarContainer,
            statements,
            msiClass,
            initMethod, resetFunctionsMethod, initStartTime, initStartNanoTime, outMsg, db,
            new EvalContext(object.read(), CTXT.intLiteral(0), CTXT.intLiteral(1), depth)
        );

        evalGenerator.collectObjectFieldSelectors(Arrays.asList(e.compiledPredicate));

        QValue valuePredicate = evalGenerator.genEval(e.compiledPredicate);
        statements.add(
            CTXT.ifStmt(
                CTXT.binExpr(valuePredicate.read(), "==", CTXT.intLiteral(1).cast(byte.class)),
                outValue.write(object.read())
            )
        );

        this.addTo.add(outValue.writeNull());
        this.addTo.add(
            CTXT.ifStmt(
                object.readIsNull(false), statements
            )
        );
    }

    private void genPredicateIterator(PredicateIterator e, QValue outValue) {
        if (context != null) {
            addTo.add(outValue.write(context.iterator));
        } else {
            throw new RuntimeException("IT can be used only in array predicate.");
        }
    }

    private void genPredicateFunction(PredicateFunction e, QValue outValue) {
        if (context != null) {
            switch (e.functionName) {
                case POSITION:
                    addTo.add(outValue.write(context.position));
                    return;
                case LAST:
                    addTo.add(outValue.write(CTXT.binExpr(context.length, "-", CTXT.intLiteral(1))));
                    return;
            }
        }

        throw new RuntimeException("Predicate functions can be used only in array predicate.");
    }

    private void genArraySlice(ArraySlice e, QValue outValue) {
        QValue arrayIn = genEval(e.selector);
        QValue from = null;
        if (e.compiledFrom != null) {
            from = genEval(e.compiledFrom);
        }
        QValue to = null;
        if (e.compiledTo != null) {
            to = genEval(e.compiledTo);
        }
        QValue step = null;
        if (e.compiledStep != null) {
            step = genEval(e.compiledStep);
        }

        QArrayValue arrayOut = (QArrayValue) outValue;
        this.addTo.add(arrayOut.setInstance());
        this.addTo.add(
            CTXT.ifStmt(
                CTXT.staticCall(
                    ARRT.class, "slice",
                    arrayIn.read(), arrayOut.read(),
                    from != null ? CTXT.staticCall(Conversions.class, "int32", from.read()) : CTXT.intLiteral(IntegerDataType.INT32_NULL),
                    to != null ? CTXT.staticCall(Conversions.class, "int32", to.read()) : CTXT.intLiteral(IntegerDataType.INT32_NULL),
                    step != null ? CTXT.staticCall(Conversions.class, "int32", step.read()) : CTXT.intLiteral(IntegerDataType.INT32_NULL)
                ).not(),
                arrayOut.setNull().asStmt()
            )
        );
        this.addTo.add(arrayOut.setChanged());
    }

    public void genArrayJoinElement(ArrayJoinElement arrayJoinElement, QValue outValue) {
        QArrayValue array = (QArrayValue) genEval(arrayJoinElement.arrayJoinExpression);

        /*
            int maxSize = ARRT.getMaxSize(state.v2, state.v3);
            if (maxSize > 0) {
                updateWaitingMessages(maxSize);

                if (state.waitingMessagesCount > 0) {
                    if (!state.v2.isNull() && currentWaitingMessage() < state.v2.get ().size ()) {
                        $1 = state.v2.get ().getFloat (state.currentWaitingMessage);
                    } else {
                        $1 = Float.NaN;
                    }
                    if (!state.v3.isNull() && currentWaitingMessage() < state.v3.get ().size ()) {
                        $2 = state.v3.get ().getFloat (state.currentWaitingMessage);
                    } else {
                        $2 = Float.NaN;
                    }

                    nextWaitingMessage();
                }
            } else {
                return REJECT;
            }
         */

        Set<ArrayJoinElement> arrayJoinElements = new HashSet<>(this.arrayJoinElements);
        arrayJoinElements.remove(arrayJoinElement);

        List<QValue> outputs = new ArrayList<>();
        List<QArrayValue> arrays = new ArrayList<>();
        outputs.add(outValue);
        arrays.add(array);
        for (ArrayJoinElement element : arrayJoinElements) {
            outputs.add(genEval(element, false));
            arrays.add((QArrayValue) genEval(element.arrayJoinExpression));
        }


        JLocalVariable maxSize = addTo.addVar(Modifier.FINAL, int.class, "$maxSize$",
            CTXT.staticCall(ARRT.class, "getMaxSize", arrays.stream().map(QArrayValue::variable).toArray(JExpr[]::new))
        );

        JCompoundStatement ifStatements = CTXT.compStmt();

        JCompoundStatement ifWaitingMessagesCountStatements2 = CTXT.compStmt();
        for (int i = 0; i < outputs.size(); ++i) {
            QValue output = outputs.get(i);
            QArrayValue arr = arrays.get(i);

            ifWaitingMessagesCountStatements2.add(
                CTXT.ifStmt(
                    CTXT.binExpr(
                        arr.isNull().not(), "&&", CTXT.binExpr(CTXT.call("currentWaitingMessage"), "<", arr.size())
                    ),
                    output.write(arr.getElement(CTXT.call("currentWaitingMessage"))),
                    output.writeNull()
                )
            );
        }
        ifWaitingMessagesCountStatements2.add(
            CTXT.ifStmt(
                CTXT.binExpr(CTXT.localVarRef("state"), "!=", CTXT.nullLiteral()),
                CTXT.call("nextWaitingMessage").asStmt()
            )
        );

        ifStatements.add(
            CTXT.ifStmt(
                CTXT.binExpr(CTXT.localVarRef("state"), "!=", CTXT.nullLiteral()),
                CTXT.call("updateWaitingMessages", maxSize).asStmt()
            )
        );
        ifStatements.add(ifWaitingMessagesCountStatements2);

        for (int i = 0; i < outputs.size(); ++i) {
            this.addTo.add(outputs.get(i).writeNull());
        }

        if (arrayJoinElement.left) {
            this.addTo.add(
                CTXT.ifStmt(
                    CTXT.binExpr(maxSize, ">", CTXT.intLiteral(0)),
                    ifStatements
                )
            );
        } else {
            this.addTo.add(
                CTXT.ifStmt(
                    CTXT.binExpr(maxSize, ">", CTXT.intLiteral(0)),
                    ifStatements,
                    msiClass.inheritedVar("REJECT").access().returnStmt()
                )
            );
        }
    }

    public void genCastClassType(CastClassType castClassType, QValue outValue) {
        QValue parent = genEval(castClassType.parent);

        RecordClassDescriptor[] sourceDescriptors = (castClassType.sourceType).getDescriptors();
        RecordClassDescriptor[] targetDescriptors = ((ClassDataType) castClassType.type).getDescriptors();

        JSwitchStatement sw = parent.read().call("typeId").switchStmt("castSwitch");
        for (int i = 0; i < sourceDescriptors.length; ++i) {
            int pos = findType(targetDescriptors, sourceDescriptors[i]);
            if (pos >= 0) {
                sw.addCaseLabel(CTXT.intLiteral(i), sourceDescriptors[i].toString());
                sw.add(outValue.write(parent.read()));
                sw.add(outValue.read().call("typeId", CTXT.intLiteral(pos)));
                sw.addBreak();
            }
        }
        sw.addDefaultLabel();
        sw.add(outValue.writeNull());

        addTo.add(sw);
    }

    public void genCastArrayType(CastArrayClassType castArrayClassType, QValue outValue) {
        QArrayValue array = (QArrayValue) genEval(castArrayClassType.parent);
        QArrayValue outArray = (QArrayValue) outValue;

        RecordClassDescriptor[] sourceDescriptors =
            ((ClassDataType) (castArrayClassType.sourceType).getElementDataType()).getDescriptors();
        RecordClassDescriptor[] targetDescriptors =
            ((ClassDataType) ((ArrayDataType) castArrayClassType.type).getElementDataType()).getDescriptors();

        JCompoundStatement ifBody = CTXT.compStmt();
        JLocalVariable len = ifBody.addVar(Modifier.FINAL, int.class, "len", array.startRead());
        JLocalVariable ii = ifBody.addVar(0, int.class, "ii");

        JCompoundStatement forBody = CTXT.compStmt();
        forBody.add(
            CTXT.ifStmt(array.next().not(), CTXT.continueStmt())
        );
        JExpr parent = array.getElement();
        JSwitchStatement sw = parent.call("typeId").switchStmt("castSwitch");
        for (int i = 0; i < sourceDescriptors.length; ++i) {
            int pos = findType(targetDescriptors, sourceDescriptors[i]);
            if (pos >= 0) {
                sw.addCaseLabel(CTXT.intLiteral(i), sourceDescriptors[i].toString());
                sw.add(
                    outArray.variable().call("addCopy", parent)
                        .call("typeId", CTXT.intLiteral(pos))
                );
                sw.addBreak();
            }
        }
        sw.addDefaultLabel();
        sw.add(outArray.write(CTXT.nullLiteral()));
        forBody.add(sw);

        ifBody.add(
            CTXT.forStmt(
                ii.assignExpr(CTXT.intLiteral(0)),
                CTXT.binExpr(ii, "<", len),
                ii.getAndInc(),
                forBody
            )
        );

        addTo.add(outValue.writeNull());
        addTo.add(
            CTXT.ifStmt(array.isNull().not(), ifBody)
        );
    }

    public void genCastPrimitiveType(CastPrimitiveType castPrimitiveType, QValue outValue) {
        QValue value = genEval(castPrimitiveType.parent);
        if (castPrimitiveType.array) {
            genCastArrayPrimitiveType(castPrimitiveType, (QArrayValue) value, (QArrayValue) outValue);
        } else {
            genCastPrimitiveType(castPrimitiveType, value, outValue);
        }
    }

    public void genCastPrimitiveType(CastPrimitiveType castPrimitiveType, QValue value, QValue outValue) {
        addTo.add(
            outValue.write(
                getCastPrimitiveType(castPrimitiveType, value.read())
            )
        );
    }

    public void genCastArrayPrimitiveType(CastPrimitiveType castPrimitiveType, QArrayValue value, QArrayValue outValue) {
        JCompoundStatement statements = CTXT.compStmt();
        statements.add(value.setInstance());

        JLocalVariable len = statements.addVar(Modifier.FINAL, int.class, "len", value.size());
        JLocalVariable ii = statements.addVar(0, int.class, "ii");

        JCompoundStatement forBody = CTXT.compStmt();
        forBody.add(
            outValue.write(
                getCastPrimitiveType(castPrimitiveType, value.getElement(ii))
            )
        );

        statements.add(
            CTXT.forStmt(
                ii.assignExpr(CTXT.intLiteral(0)),
                CTXT.binExpr(ii, "<", len),
                ii.getAndInc(),
                forBody
            )
        );

        statements.add(value.setChanged());

        addTo.add(outValue.setNull());
        addTo.add(
            CTXT.ifStmt(
                value.isNull().not(),
                statements
            )
        );
    }

    public JExpr getCastPrimitiveType(CastPrimitiveType castPrimitiveType, JExpr value) {
        if (castPrimitiveType.type.isNullable()) {
            return castPrimitiveType.targetNumeric.castFrom(value, castPrimitiveType.sourceNumeric);
        } else {
            return castPrimitiveType.targetNumeric.read(value, castPrimitiveType.sourceNumeric);
        }
    }

    private int findType(RecordClassDescriptor[] types, RecordClassDescriptor type) {
        for (int i = 0; i < types.length; ++i) {
            if (types[i].equals(type)) {
                return i;
            }
        }

        return -1;
    }

    public void collectArrayJoinElements(List<CompiledExpression> expressions) {
        expressions.forEach(this::collectArrayJoinElements);
    }

    public void collectArrayJoinElements(CompiledExpression expression) {
        if (expression instanceof ArrayJoinElement) {
            arrayJoinElements.add((ArrayJoinElement) expression);
        } else if (expression instanceof CompiledComplexExpression) {
            CompiledComplexExpression ccx = (CompiledComplexExpression) expression;

            for (CompiledExpression<?> arg : ccx.args) {
                collectArrayJoinElements(arg);
            }
        }
    }

    public void collectObjectFieldSelectors(List<CompiledExpression> expressions) {
        Consumer<FieldAccessor> f = (accessor) -> {
            if (accessor.parent != null) {
                fieldsMap.computeIfAbsent(accessor.parent, k -> new HashSet<>()).add(accessor);
            }
        };

        expressions.forEach(expression -> {
            forAllSelectors(expression, f);
        });
    }

    private void forAllSelectors(CompiledExpression<?> e, Consumer<FieldAccessor> f) {
        if (e instanceof FieldAccessor) {
            f.accept((FieldAccessor) e);
        }

        if (e instanceof CompiledComplexExpression) {
            CompiledComplexExpression ccx = (CompiledComplexExpression) e;

            for (CompiledExpression<?> arg : ccx.args) {
                forAllSelectors(arg, f);
            }
        }
    }

    private void genContainsExpression (ConnectiveExpression e, QValue outValue) {

        CompiledExpression []       args = e.args;
        int                         n = args.length;

        // java class of constants
        Class<?> clazz = QType.forDataType(args[0].type).getJavaClass();
        if (clazz == Varchar.class)
            clazz = Object.class;

        Class<?> arrayClass = Array.newInstance(clazz, 0).getClass();  // Class.forName(clazz.getName() + "[]")

        // array definition of constant values
        JArrayInitializer init = CTXT.arrayInitializer(clazz);
        for (int i = 1; i < n; i++) { // args starts from 1
            QValue value = genEval(args[i]);
            init.add(value.read());
        }

        Class<?> collection = null;
        try {
            collection = Class.forName("com.epam.deltix.util.collections.generated." + primitiveWrapper(clazz).getSimpleName() + (n > 10 ? "HashSet" : "ArrayList"));
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }

        QVariableContainer          stateVarContainer = getStateVarContainer(e);
        JVariable                   collectionVar =
                stateVarContainer.addVar(
                        "Constants of " + e,
                        true,
                        collection,
                        CTXT.newExpr(collection, CTXT.newExpr(arrayClass, null, init))
                );

        JExpr cInstance = stateVarContainer.access (collectionVar);

        JExpr expr = cInstance.call("contains", genEval(e.getArgument()).read());
        expr = CTXT.staticCall (QRT.class, e.isConjunction() ? "bneg" : "bpos", expr);

        addTo.add(outValue.write(expr));
    }

    private void collectFields(RecordClassDescriptor descriptor, List<DataField> fields) {
        if (descriptor == null) {
            return;
        }

        collectFields(descriptor.getParent(), fields);

        for (DataField field : descriptor.getFields()) {
            fields.add(field);
        }
    }

    private void                genCompiledConstantEval (
        CompiledConstant            e,
        QValue                      outValue
    )
    {
        throw new UnsupportedOperationException ("Non-inlineable constant");
    }

    private void                genParamAccessEval (
        ParamAccess                 e,
        QValue                      outValue
    )
    {
        Class <? extends ValueBean> beanClass = QCGHelpers.getValueBeanClass (e.type);

        addTo.add (
            outValue.write (
                params.index (e.ref.index).cast (beanClass).call ("getRaw")
            )
        );
    }

    private boolean             canInline (CompiledConstant e) {
        return (true);  // Not correct for all cases... TODO: fix
    }

    private QValue              genInlineCompiledConstant (
        CompiledConstant            e
    )
    {
        return (QType.forExpr (e).makeConstant (e.value));
    }

    private QValue genInlineCompiledConstant(CompiledConstant e, Class<?> type) {
        if (type == boolean.class) {
            return new QExprValue(QType.forExpr(e), CTXT.booleanLiteral(e.getBoolean()));
        }
        return QType.forExpr(e).makeConstant(e.value);
    }

    private void                genTupleConstructorEval (
        TupleConstructor            e,
        QValue                      outValue
    )
    {
        throw new UnsupportedOperationException ();
    }

    private void                genPluginFunctionEval (
        PluginFunction              e,
        QValue                      outValue
    )
    {
        FunctionInfoDescriptor fd = e.fd;
        DataType []                 sig = fd.signature();
        Class <?>                   rtc = fd.getCls();
        QVariableContainer          stateVarContainer = getStateVarContainer(e);
        JVariable                   instanceVar =
            stateVarContainer.addVar (
                "State of " + e,
                true,
                rtc,
                CTXT.newExpr (rtc)
            );

        JExpr                       instance =
            stateVarContainer.access (instanceVar);

        CompiledExpression []       args = e.args;
        int                         n = args.length;

        for (int ii = 0; ii < n; ii++) {
            CompiledExpression      arg = args [ii];
            QType                   argqtype = QType.forDataType (sig [ii]);
            QValue                  argqval =
                new QPluginArgValue (argqtype, instance, ii);

            genEval (arg, argqval);
        }

        try {
            rtc.getMethod ("update");
            addTo.add (instance.call ("update"));
        } catch (NoSuchMethodException x) {
            // not required.
        }

        QType                   resultqtype = QType.forExpr (e);
        QValue                  resultqval =
            new QPluginResultValue (resultqtype, instance);

        move (resultqval, outValue);
    }

    private void genPluginSimpleFunctionEval(PluginSimpleFunction function, QValue outValue) {
        StatelessFunctionDescriptor fd = function.getDescriptor();

        CompiledExpression<?>[] args = function.args;
        DataType[] signature = fd.signature();

        int n = args.length;
        if (fd.hasPool())
            n++;
        if (fd.hasResult())
            n++;
        if (fd.hasDB())
            n++;

        JExpr[] actualArgs = new JExpr[n];
        if (fd.hasDB()) {
            actualArgs[fd.getDBIndex()] = db;
        }
        if (fd.hasPool()) {
            assert outValue instanceof QArrayValue;
            actualArgs[fd.getPoolIndex()] = ((QArrayValue) outValue).getPool();
        }
        if (fd.hasResult()) {
            if (outValue instanceof QVarcharValue) {
               actualArgs[fd.getResultIndex()] = ((QVarcharValue) outValue).getStringBuilder();
            } else if (outValue instanceof QArrayValue && ((QArrayValue) outValue).hasClasses()) {
               actualArgs[fd.getResultIndex()] = ((QArrayValue) outValue).readTyped();
            } else {
               actualArgs[fd.getResultIndex()] = outValue.read();
            }
            if (fd.resultClassName() != null) {
                actualArgs[fd.getResultIndex()] = actualArgs[fd.getResultIndex()].cast((JTypeImpl) fd::resultClassName);
            }
        }
        int j = 0;
        for (int i = 0; i < n; i++) {
            if (i == fd.getResultIndex() || i == fd.getPoolIndex() || i == fd.getDBIndex())
                continue;
            CompiledExpression<?> arg = args[j];
            DataType type = signature[j];
            QValue value = genEval(arg);
            actualArgs[i] = expressionWithType(value, arg.type, type);
            j++;
        }

        if (outValue instanceof QArrayValue) {
            addTo.add(((QArrayValue) outValue).setInstance());
        }
        if (fd.isBoolean()) {
            JExpr condition = CTXT.staticCall(fd.getCls(), fd.getMethod(), actualArgs).not();
            if (outValue instanceof QArrayValue) {
                if (((QArrayValue) outValue).hasClasses()) {
                    JCompoundStatement stmt = CTXT.compStmt();
                    stmt.add(((QArrayValue) outValue).setTypedChanged());
                    stmt.add(((QArrayValue) outValue).writeTyped());
                    addTo.add(CTXT.ifStmt(condition, outValue.writeNull(), stmt));
                } else {
                    addTo.add(CTXT.ifStmt(condition, outValue.writeNull(), ((QArrayValue) outValue).setChanged().asStmt()));
                }
            } else {
                addTo.add(CTXT.ifStmt(condition, outValue.writeNull()));
            }
        } else {
            addTo.add(outValue.write(CTXT.staticCall(fd.getCls(), fd.getMethod(), actualArgs)));
        }
    }

    private void genFirstFunction(PluginStatefulFunction function, QValue out) {
        CompiledExpression<?>[] args = function.getOtherArgs();
        CompiledExpression<?> arg = args[0];
        QValue value = genEval(arg);
        JInitMemberVariable isFilled = (JInitMemberVariable) stateVarContainer.addVar("Flag for " + function,
                false, boolean.class, CTXT.falseLiteral());
        JExpr isFilledVar = stateVarContainer.access(isFilled);
        if (out instanceof QArrayValue) {
            QArrayValue outValue = (QArrayValue) out;
            JCompoundStatement statement = CTXT.compStmt();
            statement.add(isFilledVar.assign(CTXT.trueLiteral()));
            statement.add(outValue.copy(((QArrayValue) value).variable()));
            addTo.add(CTXT.ifStmt(isFilledVar.not(), statement));
            resetFunctionsMethod.add(isFilled.access().assign(CTXT.falseLiteral()));
        } else {
            QValue stateValue = out.type.declareValue("State of " + function, stateVarContainer, classRegistry, true);
            JCompoundStatement statement = CTXT.compStmt();
            statement.add(isFilledVar.assign(CTXT.trueLiteral()));
            if (out instanceof QObjectValue) {
                statement.add(((QObjectValue) stateValue).copy(value.read()));
            } else {
                statement.add(stateValue.write(value.read()));
            }
            addTo.add(CTXT.ifStmt(isFilledVar.not(), statement));
            addTo.add(out.write(stateValue.read()));
            resetFunctionsMethod.add(isFilled.access().assign(CTXT.falseLiteral()));
        }
    }

    private void genPluginFunction(PluginStatefulFunction function, QValue outValue) {
        StatefulFunctionDescriptor fd = function.getDescriptor();
        if (fd instanceof FirstFunctionDescriptor) {
            genFirstFunction(function, outValue);
            return;
        }
        JInitMemberVariable instanceVar = (JInitMemberVariable) stateVarContainer.addVar("State of " + function, true, fd.cls(),
                CTXT.newExpr(fd.cls()));

        CompiledExpression<?>[] initArgs = function.getInitArgs();
        DataType[] initArgTypes = fd.initArgTypes();
        int initLength = fd.initLength();
        JExpr[] actualInitArgs = new JExpr[initLength];
        int i = 0;
        if (fd.startTimeIndex() != -1) {
            actualInitArgs[fd.startTimeIndex()] = initStartTime;
        }
        if (fd.startNanoTimeIndex() != -1) {
            actualInitArgs[fd.startTimeIndex()] = initStartNanoTime;
        }
        for (int j = 0; j < initArgs.length; j++) {
            if (fd.startTimeIndex() == i)
                i++;
            CompiledExpression<?> arg = initArgs[j];
            DataType type = initArgTypes[j];
            QValue value = genEval(arg, fd.initArgs().get(i).getType());
            actualInitArgs[i++] = expressionWithType(value, arg.type, type);
        }

        CompiledExpression<?>[] args = function.getOtherArgs();
        int computeLength = fd.computeLength();
        DataType[] argTypes = fd.argTypes();
        JExpr[] actualArgs = new JExpr[computeLength];
        i = 0;
        if (fd.timestampIndex() != -1) {
            actualArgs[fd.timestampIndex()] = inMsg.call("getTimeStampMs");
        }
        if (fd.nanoTimeIndex() != -1) {
            actualArgs[fd.timestampIndex()] = inMsg.call("getNanoTime");
        }
        for (int j = 0; j < args.length; j++) {
            if (fd.timestampIndex() == i)
                i++;
            CompiledExpression<?> arg = args[j];
            DataType type = argTypes[j];
            QValue value = genEval(arg);
            actualArgs[i++] = expressionWithType(value, arg.type, type);
        }

        JExpr functionInstance = stateVarContainer.access(instanceVar);
        if (fd.isInitPresent()) {
            initMethod.add(functionInstance.call(fd.initMethod(), actualInitArgs));
        }
        resetFunctionsMethod.add(instanceVar.access().call(fd.resetMethod()));
        addTo.add(functionInstance.call(fd.computeMethod(), actualArgs));
        DataType returnType = fd.returnType();
        if (outValue instanceof QArrayValue && returnType instanceof ArrayDataType) {
            if (isConcreteArrayType((ArrayDataType) returnType)) {
                if (outValue.type instanceof QArrayType && ((QArrayType) outValue.type).hasClasses()) {
                    addTo.add(((QArrayValue) outValue).setTypedList(functionInstance.call(fd.resultMethod())));
                } else {
                    throw new RuntimeException("Failed to compile function " + function.name + ": types mismatch");
                }
            } else {
                addTo.add(((QArrayValue) outValue).setList(functionInstance.call(fd.resultMethod())));
            }
        } else {
            addTo.add(outValue.write(functionInstance.call(fd.resultMethod())));
        }
    }

    private static boolean isConcreteArrayType(ArrayDataType type) {
        if (type.getElementDataType() instanceof ClassDataType) {
            return ((ClassDataType) type.getElementDataType()).getDescriptors().length > 0;
        }

        return false;
    }

    private static JExpr expressionWithType(QValue value, DataType source, DataType target) {
        if (NumericType.isNumericType(source) && NumericType.isNumericType(target)) {
            return Objects.requireNonNull(NumericType.forType(target)).read(value, NumericType.forType(source));
        } else if (source instanceof ClassDataType && target instanceof ClassDataType &&
                !((ClassDataType) source).isFixed() && ((ClassDataType) target).isFixed()) {
            try {
                return value.read().cast(Class.forName(((ClassDataType) target).getFixedDescriptor().getName()));
            } catch (ClassNotFoundException ignored) {
            }
        } else if (source instanceof ArrayDataType && target instanceof ArrayDataType && value instanceof QArrayValue) {
            DataType sourceElement = ((ArrayDataType) source).getElementDataType();
            DataType targetElement = ((ArrayDataType) target).getElementDataType();
            if (sourceElement instanceof ClassDataType && targetElement instanceof ClassDataType) {
                if (((QArrayValue) value).hasClasses() && ((ClassDataType) targetElement).getDescriptors().length > 0) {
                    return ((QArrayValue) value).readTyped();
                }
            }
        }
        return value.read();
    }

    private void                genUnaryOperation(UnaryExpression expression, QValue outValue) {
        CompiledExpression<?>[] args = expression.args;
        int n = args.length;
        QValue[] argValues = new QValue[n];

        for (int ii = 0; ii < n; ii++)
            argValues[ii] = genEval(args[ii]);

        expression.generateOperation(argValues[0], outValue, addTo);
    }

    private void                genBinaryOperation(BinaryExpression expression, QValue outValue) {
        CompiledExpression<?>[] args = expression.args;
        int n = args.length;
        QValue[] argValues = new QValue[n];

        for (int ii = 0; ii < n; ii++)
            argValues[ii] = genEval(args[ii]);

        expression.generateOperation(argValues[0], argValues[1], outValue, addTo);
    }

    private void genCompiledIfExpression(CompiledIfExpression e, QValue outValue) {
        QValue conditionVal = genEval(e.condition);
        QValue thenVal = genEval(e.thenExpression);
        QValue elseVal = genEval(e.elseExpression);

        JExpr conditionExpression = CTXT.condExpr(
            CTXT.binExpr(conditionVal.read(), "==", CTXT.intLiteral(1)),
            thenVal.read(), elseVal.read()
        );
        if (outValue instanceof QArrayValue) {
            this.addTo.add(((QArrayValue) outValue).setNull());
            this.addTo.add(((QArrayValue) outValue).writeAll(conditionExpression));
        } else {
            this.addTo.add(outValue.write(conditionExpression));
        }
    }

    private void genCompiledCaseExpression(CompiledCaseExpression e, QValue outValue) {
        QValue caseVal = genEval(e.caseExpression);
        QValue elseVal = genEval(e.elseExpression);
        List<QValue> whenVals = new ArrayList<>();
        List<QValue> thenVals = new ArrayList<>();
        for (int i = 0; i < e.whenExpressions.size(); ++i) {
            whenVals.add(genEval(e.whenExpressions.get(i).whenExpression));
            thenVals.add(genEval(e.whenExpressions.get(i).thenExpression));
        }

        List<JExpr> cond = new ArrayList<>();
        List<JStatement> then = new ArrayList<>();
        for (int i = 0; i < whenVals.size(); ++i) {
            cond.add(
                CTXT.binExpr(whenVals.get(i).read(), "==", caseVal.read())
            );
            then.add(
                outValue instanceof QArrayValue ?
                    ((QArrayValue) outValue).writeAll(thenVals.get(i).read()) :
                    outValue.write(thenVals.get(i).read())
            );
        }
        JStatement els = outValue instanceof QArrayValue ?
            ((QArrayValue) outValue).writeAll(elseVal.read()) :
            outValue.write(elseVal.read());

        if (outValue instanceof QArrayValue) {
            this.addTo.add(((QArrayValue) outValue).setNull());
        }
        this.addTo.add(CTXT.ifStmt(cond, then, els));
    }

    private void                genSimpleFunctionEval (
        SimpleFunction              e,
        QValue                      outValue
    )
    {
        CompiledExpression []       args = e.args;
        int                         n = args.length;
        QValue []                   argValues = new QValue [n];

        for (int ii = 0; ii < n; ii++)
            argValues [ii] = genEval (args [ii]);

        switch (e.code) {
            case INTEGER_TO_FLOAT:
                QCodeGenerator.move(argValues[0], outValue, addTo);
                break;

            case DECIMAL_TO_FLOAT:
                QType.decimalToFloat(argValues[0], outValue, addTo);
                break;

            case INTEGER_TO_DECIMAL:
                QType.integerToDecimal(argValues[0], outValue, addTo);
                break;

            case VARCHAR_LIKE:
                QType.genEqOp (argValues [0], "QRT.slike", true, argValues [1], outValue, addTo);
                break;

            case VARCHAR_NLIKE:
                QType.genEqOp (argValues [0], "QRT.snlike", true, argValues [1], outValue, addTo);
                break;

            case IS_NULL:  
            case IS_NOT_NULL:
                addTo.add (
                    outValue.write (
                        CTXT.staticCall (
                            QRT.class, "bpos",
                            argValues [0].readIsNull (e.code == SimpleFunctionCode.IS_NULL)
                        )
                    )
                );
                break;

            case IS_NAN:
            case IS_NOT_NAN:
                addTo.add (
                    outValue.write (
                        CTXT.staticCall(
                            QRT.class, "bpos",
                            argValues[0].type.checkNan(argValues[0].read(), e.code == SimpleFunctionCode.IS_NAN)
                        )
                    )
                );
                break;

            default:
                throw new UnsupportedOperationException (e.code.name ());
        }
    }

    private void                genSymbolSelectorEval (
        SymbolSelector              e,
        QValue                      outValue
    )
    {
        addTo.add (outValue.write (inMsg.call ("getSymbol")));
    }
    
    private void                genTimestampSelectorEval (
        TimestampSelector           e,
        QValue                      outValue
    )
    {
        addTo.add (outValue.write (inMsg.call ("getTimeStampMs")));
    }
    
    private void                genCompiledFilterEval (
        CompiledFilter              e,        
        QValue                      outValue
    )
    {
        //TMP
    }

    private QValue genArrayConstant(CompiledArrayConstant<?, ?, ?> e) {
        QVariableContainer stateVarContainer = getStateVarContainer(e);
        JVariable var = stateVarContainer.addVar("Array constant " + e,true, e.getCls(), CTXT.newExpr(e.getCls(),
                CTXT.newArrayExpr(e.getElementCls(), e.getElements())));
        QArrayValue value = new QArrayValue(QType.forExpr(e), stateVarContainer.access(var));
        x2v.put (e, value);
        return value;
    }
}