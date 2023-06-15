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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import com.epam.deltix.qsrv.hf.pub.md.ClassDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;

import java.util.*;

/**
 *
 */
public class TupleConstructor extends CompiledComplexExpression {

    public final Map<RecordClassDescriptor, List<CompiledExpression<?>>> typeToExpressions;

    public final Map<RecordClassDescriptor, CompiledExpression<?>> typeToCondition;

    private final ClassDataType classType;

    private static CompiledExpression []    cat (
        CompiledExpression        timestampInitializer,
        CompiledExpression        symbolInitializer,
        //CompiledExpression        typeInitializer,
        CompiledExpression ...    nonStaticInitializers
    )
    {
        int                         n = nonStaticInitializers.length;
        CompiledExpression [] a = new CompiledExpression [n + 2];

        a [0] = timestampInitializer;
        a [1] = symbolInitializer;
        //a [2] = typeInitializer;

        if (n != 0)
            System.arraycopy (nonStaticInitializers, 0, a, 2, n);

        return (a);
    }

    public static TupleConstructor polymorphicTuple(TupleConstructor[] tuples, Map<String, CompiledExpression<?>> conditions) {
        List<RecordClassDescriptor> descriptors = new ArrayList<>();
        Map<RecordClassDescriptor, List<CompiledExpression<?>>> expressions = new HashMap<>();
        Map<RecordClassDescriptor, CompiledExpression<?>> typeToCondition = new HashMap<>();
        for (int i = 0; i < tuples.length; ++i) {
            descriptors.addAll(Arrays.asList(tuples[i].classType.getDescriptors()));
            expressions.putAll(tuples[i].typeToExpressions);
        }
        descriptors.forEach(d -> {
            CompiledExpression<?> e = conditions.get(d.getName());
            if (e != null) {
                typeToCondition.put(d, e);
            }
        });

        return new TupleConstructor(
            new ClassDataType(true, descriptors.toArray(new RecordClassDescriptor[0])),
            tuples[0].getTimestampInitializer(),
            tuples[0].getSymbolInitializer(),
            //tuples[0].getTypeInitializer(),
            expressions, typeToCondition
        );
    }

    private static CompiledExpression[] listExpressions(Map<RecordClassDescriptor, List<CompiledExpression<?>>> typeToExpressions) {
        return typeToExpressions.values().stream().flatMap(Collection::stream).toArray(CompiledExpression[]::new);
    }

    public TupleConstructor(
        ClassDataType type,
        CompiledExpression<?> timestampInitializer,
        CompiledExpression<?> symbolInitializer,
        //CompiledExpression<?> typeInitializer,
        Map<RecordClassDescriptor, List<CompiledExpression<?>>> typeToExpressions
    ) {
        this(type, timestampInitializer, symbolInitializer, typeToExpressions, new HashMap<>());
    }

    public TupleConstructor(
        ClassDataType type,
        CompiledExpression<?> timestampInitializer,
        CompiledExpression<?> symbolInitializer,
        //CompiledExpression<?> typeInitializer,
        Map<RecordClassDescriptor, List<CompiledExpression<?>>> typeToExpressions,
        Map<RecordClassDescriptor, CompiledExpression<?>> typeToCondition
    ) {
        super(type, cat(timestampInitializer, symbolInitializer, listExpressions(typeToExpressions)));

        this.classType = type;
        this.typeToExpressions = typeToExpressions;
        this.typeToCondition = typeToCondition;
    }

    public RecordClassDescriptor[] getClassDescriptors() {
        return (((ClassDataType) type).getDescriptors());
    }

    public CompiledExpression getTimestampInitializer() {
        return (args[0]);
    }

    public CompiledExpression getSymbolInitializer() {
        return (args[1]);
    }

//    public CompiledExpression getTypeInitializer() {
//        return (args[2]);
//    }

    public CompiledExpression[] getNonStaticInitializers() {
        int n = args.length - 2;
        CompiledExpression[] ret = new CompiledExpression[n];

        System.arraycopy(args, 2, ret, 0, n);

        return (ret);
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object obj) {
        return
            super.equals(obj) &&
                Arrays.equals(getClassDescriptors(), ((TupleConstructor) obj).getClassDescriptors());
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Arrays.hashCode(getClassDescriptors());
    }

    @Override
    public void print(StringBuilder out) {
        out.append("new ");
        printDescriptorNames(out);
        out.append("(");
        printArgs(out, 2);
        out.append(")");
    }

    public void printDescriptorNames(StringBuilder out) {
        out.append("{");
        RecordClassDescriptor[] descriptors = getClassDescriptors();
        for (int i = 0; i < descriptors.length; ++i) {
            out.append(descriptors[i].getName());
        }
        out.append("}");
    }
}