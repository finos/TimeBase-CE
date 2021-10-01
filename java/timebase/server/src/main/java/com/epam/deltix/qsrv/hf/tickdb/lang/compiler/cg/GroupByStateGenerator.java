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

import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.FilterIMSImpl;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.FilterState;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.GroupByFilterState;
import com.epam.deltix.util.jcg.*;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;
import org.ehcache.spi.serialization.SerializerException;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;
import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PUBLIC;

/**
 *
 */
class GroupByStateGenerator {

    private QClassRegistry classRegistry;
    private EvalGenerator evalGenerator;

    GroupByStateGenerator(QClassRegistry classRegistry, EvalGenerator evalGenerator) {
        this.classRegistry = classRegistry;
        this.evalGenerator = evalGenerator;
    }

    void generate(GroupByExpressions groupByExpressions, JClass interimStateClass, JExpr interimTypedState) {
        QVariableContainer memberStateVars = new QVariableContainer(PRIVATE, interimStateClass, interimTypedState, "m");
        CompiledExpression[] expressions = groupByExpressions.expressions;
        QValue[] values = new QValue[expressions.length];
        for (int i = 0; i < expressions.length; ++i) {
            CompiledExpression expression = expressions[i];
            QValue value = evalGenerator.genEval(expression);

            QType type = QType.forExpr(expression);
            values[i] = type.declareValue(
                "Result of " + expression + " (part of group by)",
                memberStateVars,
                classRegistry, false
            );

            evalGenerator.addTo.add(
                values[i].write(value.read())
            );
        }

        generateStateMethods(interimStateClass, values);
    }

    private void generateStateMethods(JClass interimStateClass, QValue[] values) {
        List<JMemberVariable> stateVars = interimStateClass.getVars();
        stateVars = stateVars.stream().filter(v -> v.name().startsWith("m")).collect(Collectors.toList());
        String[] variableNames = new String[stateVars.size()];
        JExpr[] variables = new JExpr[stateVars.size()];
        for (int i = 0; i < stateVars.size(); ++i) {
            variableNames[i] = stateVars.get(i).name();
            variables[i] = CTXT.staticVarRef("this", variableNames[i]);
        }

        // public boolean equals(Object o)
        JMethod stateEqualsMethod = interimStateClass.addMethod(PUBLIC, boolean.class, "equals");
        stateEqualsMethod.addAnnotation(CTXT.annotation(Override.class));
        JMethodArgument o = stateEqualsMethod.addArg(0, Object.class, "o");
        /*
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GroupByStateImpl that = (GroupByStateImpl) o;
         */
        stateEqualsMethod.body().add(
            CTXT.ifStmt(
                CTXT.binExpr(CTXT.thisLiteral(), "==", o),
                CTXT.trueLiteral().returnStmt()
            )
        );
        stateEqualsMethod.body().add(
            CTXT.ifStmt(
                CTXT.binExpr(
                    CTXT.binExpr(o, "==", CTXT.nullLiteral()),
                    "||",
                    CTXT.binExpr(CTXT.call("getClass"), "!=", CTXT.staticVarRef("o", "getClass()"))
                ),
                CTXT.falseLiteral().returnStmt()
            )
        );
        stateEqualsMethod.body().addVar(
            0, interimStateClass, "that", o.cast(interimStateClass)
        );
        /*
            return v1 == that.v1 &&
                v2 == that.v2 &&
                Objects.equals(v3, that.v3);
         */
        JExpr[] comparators = new JExpr[variableNames.length];
        for (int i = 0; i < variableNames.length; ++i) {
            if (values[i].type instanceof QVarcharType) {
                comparators[i] = CTXT.call("this." + variableNames[i] + ".equals", CTXT.staticVarRef("that", variableNames[i]));
            } else {
                comparators[i] = CTXT.binExpr(
                    CTXT.staticVarRef("this", variableNames[i]),
                    "==",
                    CTXT.staticVarRef("that", variableNames[i])
                );
            }
        }

        JExpr comparator = comparators[0];
        for (int i = 1; i < comparators.length; ++i) {
            comparator = CTXT.binExpr(comparator, "&&", comparators[i]);
        }

        stateEqualsMethod.body().add(comparator.returnStmt());


        // public int hashCode()
        JMethod stateHashCodeMethod = interimStateClass.addMethod(PUBLIC, int.class, "hashCode");
        stateHashCodeMethod.addAnnotation(CTXT.annotation(Override.class));
        stateHashCodeMethod.body().add(
            CTXT.staticCall(Objects.class, "hash", variables).returnStmt()
        );

        // public GroupByFilterState copy(GroupByFilterState to)
        JMethod stateCopyMethod = interimStateClass.addMethod(PUBLIC, GroupByFilterState.class, "copy");
        stateCopyMethod.addAnnotation(CTXT.annotation(Override.class));
        JMethodArgument to = stateCopyMethod.addArg(0, GroupByFilterState.class, "to");
        JLocalVariable thatVar = stateCopyMethod.body().addVar(
            0, interimStateClass, "that", to.cast(interimStateClass)
        );
        /*
        that.v1 = this.v1;
        return that;
        */
        for (int i = 0; i < variableNames.length; ++i) {
            if (values[i].type instanceof QVarcharType) {
                stateCopyMethod.body().add(
                    CTXT.call("that." + variableNames[i] + ".set",
                        CTXT.call("this." + variableNames[i] + ".get")
                    )
                );
            } else {
                stateCopyMethod.body().add(
                    CTXT.staticVarRef("that", variableNames[i]).assign(
                        CTXT.staticVarRef("this", variableNames[i])
                    )
                );
            }
        }

        stateCopyMethod.body().add(
            thatVar.returnStmt()
        );

        /*
        @Override
        public ByteBuffer serialize(GroupByFilterState groupByFilterState) throws SerializerException {
            interimState = (InterimState) groupByFilterState;
            MemoryDataOutput mdo = super.mdo;
            mdo.reset();
            mdo.writeByte(interimState.m1);
            return getBuffer();
        }
         */
        JInitMemberVariable interimStateVar = interimStateClass.addVar(PRIVATE, interimStateClass, "interimState");

        JMethod serializeMethod = interimStateClass.addMethod(PUBLIC, ByteBuffer.class, "serialize");
        serializeMethod.addException(SerializerException.class);
        JMethodArgument stateArg = serializeMethod.addArg(0, GroupByFilterState.class, "groupByFilterState");
        serializeMethod.body().add(
            interimStateVar.access().assign(stateArg.cast(interimStateClass))
        );
        JLocalVariable mdoVar = serializeMethod.body().addVar(
            0, MemoryDataOutput.class, "mdo", CTXT.staticVarRef("super", "mdo")
        );
        serializeMethod.body().add(CTXT.call("mdo.reset"));
        for (int i = 0; i < variableNames.length; ++i) {
            values[i].type.encode(values[i], mdoVar, serializeMethod.body());
        }
        serializeMethod.body().add(CTXT.call("getBuffer").returnStmt());


        /*
        @Override
        public GroupByFilterState read(ByteBuffer byteBuffer) throws ClassNotFoundException, SerializerException {
            interimState = new InterimState(filter);
            MemoryDataOutput mdi = super.mdi;
            setBuffer(byteBuffer);
            interimState.m1 = mdi.readByte();
            return object;
        }
         */
        JMethod readMethod = interimStateClass.addMethod(PUBLIC, GroupByFilterState.class, "read");
        readMethod.addException(ClassNotFoundException.class);
        readMethod.addException(SerializerException.class);
        JMethodArgument byteBufferArg = readMethod.addArg(0, ByteBuffer.class, "byteBuffer");
        readMethod.body().add(
            interimStateVar.access().assign(
                CTXT.newExpr(interimStateClass, CTXT.nullLiteral().cast(FilterIMSImpl.class))
            )
        );
        JLocalVariable mdiVar = readMethod.body().addVar(
            0, MemoryDataInput.class, "mdi", CTXT.staticVarRef("super", "mdi")
        );
        readMethod.body().add(
            CTXT.call("setBuffer", byteBufferArg)
        );
        for (int i = 0; i < variableNames.length; ++i) {
            readMethod.body().add(
                values[i].type.decode(mdiVar, values[i])
            );
        }
        readMethod.body().add(interimStateVar.access().returnStmt());
    }

}