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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;
import com.epam.deltix.util.parsers.Element;

import java.util.*;
import java.util.stream.Collectors;

public class QQLTupleBuilder {

    private static class FieldExpression {
        private DataField field;
        private RecordClassDescriptor descriptor;
        private Expression expression;
        private CompiledExpression<?> compiledExpression;
        private boolean isArrayJoin;

        private static void collectFields(RecordClassDescriptor descriptor, List<FieldExpression> fieldExpressions) {
            if (descriptor.getParent() != null) {
                collectFields(descriptor.getParent(), fieldExpressions);
            }

            for (int i = 0; i < descriptor.getFields().length; ++i) {
                DataField field = descriptor.getFields()[i];
                if (field instanceof NonStaticDataField) {
                    FieldExpression fieldExpression = new FieldExpression();
                    fieldExpression.field = field;
                    fieldExpression.descriptor = descriptor;
                    fieldExpressions.add(fieldExpression);
                }
            }
        }
    }

    private static class DescriptorCache {
        private RecordClassDescriptor descriptor;
        private RecordClassDescriptor outDescriptor;
        private List<DataField> fields = new ArrayList<>();
        private boolean isChanged;
        private DescriptorCache parent;
    }

    private final QQLExpressionCompiler qqlCompiler;

    QQLTupleBuilder(QQLExpressionCompiler qqlCompiler) {
        this.qqlCompiler = qqlCompiler;
    }

    TupleConstructor createTupleWithArrayJoins(CompiledQuery query,
                                               Map<CompiledExpression<DataType>, Expression> compiledArrayJoins,
                                               boolean clearTimeAndIdentity)
    {
        Map<RecordClassDescriptor, List<FieldExpression>> typeToExpressions = new LinkedHashMap<>();

        RecordClassDescriptor[] descriptors = query.getConcreteOutputTypes();
        for (int i = 0; i < descriptors.length; ++i) {
            List<FieldExpression> fieldExpressions = new ArrayList<>();
            FieldExpression.collectFields(descriptors[i], fieldExpressions);

            for (int fieldI = 0; fieldI < fieldExpressions.size(); ++fieldI) {
                FieldExpression fieldExpression = fieldExpressions.get(fieldI);

                DataField field = fieldExpression.field;
                CompiledExpression<DataType> arrayJoin =
                    findExpressionByName(compiledArrayJoins.keySet(), field.getName());
                if (arrayJoin != null) {
                    fieldExpression.isArrayJoin = true;
                    fieldExpression.expression = compiledArrayJoins.get(arrayJoin);
                    fieldExpression.compiledExpression = arrayJoin;
                } else {
                    fieldExpression.expression = new FieldAccessExpression(
                        new TypeIdentifier(descriptors[i].getName()),
                        new FieldIdentifier(Element.NO_LOCATION, field.getName())
                    );
                    fieldExpression.compiledExpression = qqlCompiler.compile(fieldExpression.expression, null);
                }
            }

            typeToExpressions.put(descriptors[i], fieldExpressions);
        }

        // find array join expression that was not mapped to any field
        Set<CompiledExpression<?>> notMappedArrayJoins = new HashSet<>(compiledArrayJoins.keySet());
        for (List<FieldExpression> fieldExpressions : typeToExpressions.values()) {
            for (FieldExpression fieldExpression : fieldExpressions) {
                if (fieldExpression.isArrayJoin) {
                    notMappedArrayJoins.remove(fieldExpression.compiledExpression);
                }
            }
        }

        // add that array joins as the last field of record class descriptor
        for (CompiledExpression<?> arrayJoin : notMappedArrayJoins) {
            Set<RecordClassDescriptor> forTypes = new HashSet<>();
            extractRootTypes(arrayJoin, forTypes);
            typeToExpressions.forEach((descriptor, fieldExpressions) -> {
                if (forTypes.size() == 0 || containsDescriptor(descriptor, forTypes)) {
                    FieldExpression fieldExpression = new FieldExpression();
                    fieldExpression.isArrayJoin = true;
                    fieldExpression.expression = compiledArrayJoins.get(arrayJoin);
                    fieldExpression.compiledExpression = arrayJoin;
                    fieldExpression.descriptor = descriptor;

                    fieldExpressions.add(fieldExpression);
                }
            });
        }
        typeToExpressions.forEach((descriptor, fieldExpressions) -> {
            List<FieldExpression> parentExpressions = typeToExpressions.get(descriptor.getParent());
            if (parentExpressions != null) {
                updateParentFieldExpressions(fieldExpressions, parentExpressions);
            }
        });

        // find types that has no any array join field and mark them as empty (not required to change structure)
        for (List<FieldExpression> fieldExpressions : typeToExpressions.values()) {
            if (fieldExpressions.stream().filter(f -> f.isArrayJoin).findAny().orElse(null) == null) {
                fieldExpressions.clear();
            }
        }

        return createTuple(typeToExpressions, clearTimeAndIdentity);
    }

    private void updateParentFieldExpressions(List<FieldExpression> fieldExpressions, List<FieldExpression> parentExpressions) {
        if (fieldExpressions.size() == 0 || parentExpressions.size() == 0) {
            return;
        }

        RecordClassDescriptor lastDescriptor = fieldExpressions.get(fieldExpressions.size() - 1).descriptor;

        List<FieldExpression> newExpressions = new ArrayList<>();
        newExpressions.addAll(parentExpressions);
        newExpressions.addAll(
            fieldExpressions.stream().filter(f -> f.descriptor.equals(lastDescriptor)).collect(Collectors.toList())
        );
        fieldExpressions.clear();
        fieldExpressions.addAll(newExpressions);
    }

    private void extractRootTypes(CompiledExpression expression, Set<RecordClassDescriptor> types) {
        if (expression instanceof FieldSelector) {
            types.add(((FieldSelector) expression).fieldRef.parent);
        }

        if (expression instanceof CompiledComplexExpression) {
            CompiledComplexExpression compiledComplexExpression = (CompiledComplexExpression) expression;
            for (int i = 0; i < compiledComplexExpression.args.length; ++i) {
                extractRootTypes(compiledComplexExpression.args[i], types);
            }
        }
    }

    private boolean containsDescriptor(RecordClassDescriptor descriptor, Set<RecordClassDescriptor> types) {
        if (types.contains(descriptor)) {
            return true;
        }

        if (descriptor.getParent() != null) {
            return containsDescriptor(descriptor.getParent(), types);
        }

        return false;
    }

    @SuppressWarnings("ConvertToStringSwitch")
    private TupleConstructor createTuple(Map<RecordClassDescriptor, List<FieldExpression>> typeToExpressions,
                                         boolean clearTimeAndIdentity)
    {
        ArrayList<CompiledExpression> nsInits = new ArrayList<>();
        CompiledExpression tsInit = null;
        CompiledExpression symbolInit = null;
        //CompiledExpression typeInit = null;

        List<RecordClassDescriptor> newDescriptors = new ArrayList<>();
        Map<RecordClassDescriptor, List<CompiledExpression<?>>> typeToInitializers = new LinkedHashMap<>();
        Map<RecordClassDescriptor, DescriptorCache> descriptorsMap = new LinkedHashMap<>();
        typeToExpressions.forEach(((descriptor, expressions) -> {
            if (expressions == null || expressions.size() == 0) {
                newDescriptors.add(descriptor);
                typeToInitializers.put(descriptor, new ArrayList<>());
                return;
            }

            DescriptorCache descriptorCache = null;
            HashSet<String> namesInUse = new HashSet<>();
            ArrayList<CompiledExpression<?>> compiledExpressions = new ArrayList<>();
            for (int i = 0; i < expressions.size(); ++i) {
                FieldExpression fieldExpression = expressions.get(i);

                descriptorCache = descriptorsMap.get(fieldExpression.descriptor);
                if (descriptorCache == null) {
                    descriptorsMap.put(fieldExpression.descriptor, descriptorCache = new DescriptorCache());
                    descriptorCache.descriptor = fieldExpression.descriptor;
                    DescriptorCache parentDescriptorCache = descriptorsMap.get(descriptorCache.descriptor.getParent());
                    if (parentDescriptorCache != null) {
                        descriptorCache.parent = parentDescriptorCache;
                        descriptorCache.isChanged = parentDescriptorCache.isChanged;
                    }
                }
                if (fieldExpression.isArrayJoin) {
                    descriptorCache.isChanged = true;
                }

                CompiledExpression<?> compiledExpression = fieldExpression.compiledExpression;
                compiledExpressions.add(compiledExpression);

                DataType type = compiledExpression.type;
                String name = compiledExpression.getFieldName();
                if (name == null || !namesInUse.add(name)) {
                    if (name == null) {
                        name = "$";
                    }

                    for (int jj = 1; ; jj++) {
                        String test = name + jj;

                        if (namesInUse.add(test)) {
                            name = test;
                            break;
                        }
                    }
                }

                if (descriptorCache.outDescriptor != null) {
                    continue;
                }
                descriptorCache.fields.add(new NonStaticDataField(name, name, type));
                nsInits.add(compiledExpression);
            }

            RecordClassDescriptor newDescriptor = copyIfChanged(descriptorCache);

            newDescriptors.add(newDescriptor);
            typeToInitializers.put(newDescriptor, compiledExpressions);
        }));

        if (clearTimeAndIdentity) {
            if (tsInit == null)
                tsInit = new CompiledConstant(StandardTypes.NULLABLE_TIMESTAMP, null);

            if (symbolInit == null)
                symbolInit = new CompiledConstant(StandardTypes.CLEAN_VARCHAR, "");

//            if (typeInit == null)
//                typeInit = new CompiledConstant(StdEnvironment.INSTR_TYPE_ENUM, InstrumentType.SYSTEM.ordinal());
        }

        return (
            new TupleConstructor(
                new ClassDataType(false, newDescriptors.toArray(new RecordClassDescriptor[0])),
                tsInit, symbolInit, typeToInitializers
            )
        );
    }

    private static CompiledExpression<DataType> findExpressionByName(Set<CompiledExpression<DataType>> compiledExpressions, String name) {
        for (CompiledExpression<DataType> compiledExpression : compiledExpressions) {
            if (name.equalsIgnoreCase(compiledExpression.getFieldName())) {
                return compiledExpression;
            }
        }

        return null;
    }

    private static boolean markIsChanged(DescriptorCache descriptor) {
        if (descriptor == null) {
            return false;
        }

        if (markIsChanged(descriptor.parent)) {
            descriptor.isChanged = true;
            return true;
        }

        return descriptor.isChanged;
    }

    private static RecordClassDescriptor copyIfChanged(DescriptorCache descriptorCache) {
        if (descriptorCache == null) {
            return null;
        }

        if (descriptorCache.outDescriptor != null) {
            return descriptorCache.outDescriptor;
        }

        RecordClassDescriptor parentDescriptor = copyIfChanged(descriptorCache.parent);

        if (descriptorCache.isChanged) {
            RecordClassDescriptor descriptor = descriptorCache.descriptor;
            String queryID = QQLExpressionCompiler.getQueryID(descriptorCache.fields);

            descriptorCache.outDescriptor = new RecordClassDescriptor(
                    queryID, descriptor.getTitle(), descriptor.isAbstract(), parentDescriptor,
                descriptorCache.fields.toArray(new DataField[descriptorCache.fields.size()])
            );
        } else {
            descriptorCache.outDescriptor = descriptorCache.descriptor;
        }

        return descriptorCache.outDescriptor;
    }
}