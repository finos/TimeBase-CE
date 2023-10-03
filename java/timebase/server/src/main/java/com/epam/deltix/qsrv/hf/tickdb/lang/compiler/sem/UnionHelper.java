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
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.Expression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.Identifier;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;
import com.epam.deltix.util.parsers.CompilationException;

import java.util.*;
import java.util.stream.Collectors;

public class UnionHelper {

    private static class TypeFields {
        private final Map<String, LinkedHashMap<String, DataField>> fieldsMap = new HashMap<>();

        private void put(String descriptor, DataField field) {
            fieldsMap.computeIfAbsent(descriptor, k -> new LinkedHashMap<>()).put(field.getName().toUpperCase(), field);
        }

        private void putAll(TypeFields fields) {
            fields.fieldsMap.forEach((t, f) -> {
                f.values().forEach(field -> {
                    put(t, field);
                });
            });
        }

        private boolean contains(String descriptor) {
            LinkedHashMap<String, DataField> result = fieldsMap.get(descriptor);
            return result != null;
        }

        private DataField get(String descriptor, String fieldName) {
            DataField field = fieldsMap.computeIfAbsent(descriptor, k -> new LinkedHashMap<>()).get(fieldName.toUpperCase());
            return field;
        }

        private List<DataField> get(String descriptor) {
            LinkedHashMap<String, DataField> fields = fieldsMap.get(descriptor);
            if (fields != null) {
                return new ArrayList<>(fields.values());
            } else {
                return null;
            }
        }
    }

    public static CompiledUnion unionCompiledQueries(CompiledQuery[] compiledQueries, SelectLimit limit) {
        validateOutputTypesMatches(compiledQueries);
        TypeFields streamFields = buildStreamFields(compiledQueries);
        TypeFields outputFields = mergeFields(streamFields, compiledQueries);
        Map<String, RecordClassDescriptor> outputTypes = unionOutputTypes(outputFields, compiledQueries);

        return new CompiledUnion(
            new QueryDataType(true, new ClassDataType(
                true, outputTypes.values().toArray(new RecordClassDescriptor[0])
            )),
            compiledQueries[0].isForward(), limit, compiledQueries
        );
    }

    public static StreamSelector unionStreamSelectors(StreamSelector... selectors) {
        validateOutputTypesMatches(selectors);

        RecordClassSet outputSchema = merge(
            Arrays.stream(selectors)
                .map(s -> new RecordClassSet(s.type.getOutputType().getDescriptors()))
                .toArray(RecordClassSet[]::new)
        );

        return new StreamSelector(
            new QueryDataType(true, new ClassDataType(
                true,
                outputSchema.getContentClasses()
            )),
            outputSchema.getClassDescriptors(),
            Arrays.stream(selectors)
                .flatMap(s -> Arrays.stream(s.streams))
                .toArray(TickStream[]::new)
        );
    }

    private static RecordClassSet merge(RecordClassSet... rcss) {
        Map<String, ClassDescriptor> types = new HashMap<>();
        Map<String, RecordClassDescriptor> topTypes = new HashMap<>();
        for (int i = 0; i < rcss.length; ++i) {
            ClassDescriptor[] descriptors = rcss[i].getClassDescriptors();
            for (ClassDescriptor descriptor : descriptors) {
                copy(descriptor, types);
            }

            RecordClassDescriptor[] rcds = rcss[i].getTopTypes();
            for (RecordClassDescriptor rcd : rcds) {
                topTypes.put(rcd.getName(), (RecordClassDescriptor) types.get(rcd.getName()));
            }
        }

        return new RecordClassSet(topTypes.values().toArray(new RecordClassDescriptor[0]));
    }

    private static ClassDescriptor copy(ClassDescriptor descriptor, Map<String, ClassDescriptor> copy) {
        if (descriptor == null) {
            return null;
        }

        ClassDescriptor newDescriptor = copy.get(descriptor.getName());
        if (newDescriptor != null) {
            return newDescriptor;
        }

        if (descriptor instanceof RecordClassDescriptor) {
            RecordClassDescriptor rcd = (RecordClassDescriptor) descriptor;
            RecordClassDescriptor parent = rcd.getParent();
            if (parent != null) {
                parent = (RecordClassDescriptor) copy(parent, copy);
            }

            newDescriptor = new RecordClassDescriptor(
                ClassDescriptor.createGuid(),
                rcd.getName(), rcd.getTitle(), rcd.isAbstract(), parent,
                copyFields(rcd, copy)
            );
        } else if (descriptor instanceof EnumClassDescriptor) {
            EnumClassDescriptor ecd = (EnumClassDescriptor) descriptor;
            newDescriptor = new EnumClassDescriptor(
                ecd.getName(),
                ecd.getTitle(),
                ecd.isBitmask(),
                Arrays.copyOf(ecd.getValues(), ecd.getValues().length)
            );
        } else {
            throw new RuntimeException("Unknown type of descriptor");
        }

        newDescriptor.setDescription(descriptor.getDescription());
        copy.put(newDescriptor.getName(), newDescriptor);
        return newDescriptor;

    }

    private static DataField[] copyFields(
        RecordClassDescriptor descriptor, Map<String, ClassDescriptor> copy) {

        DataField[] fields = Arrays.copyOf(descriptor.getFields(), descriptor.getFields().length);
        for (int i = 0; i < fields.length; ++i) {
            DataType type = fields[i].getType();
            DataField field = fields[i];
            if (type instanceof ClassDataType) {
                RecordClassDescriptor[] fieldDescriptors = classDescriptors((ClassDataType) type);
                for (int j = 0; j < fieldDescriptors.length; ++j) {
                    fieldDescriptors[j] = (RecordClassDescriptor) copy(fieldDescriptors[j], copy);
                }
                fields[i] = copyField(field, new ClassDataType(type.isNullable(), fieldDescriptors));
            } else if (type instanceof ArrayDataType) {
                ArrayDataType arrayDataType = (ArrayDataType) type;
                DataType elementDataType = arrayDataType.getElementDataType();
                if (elementDataType instanceof ClassDataType) {
                    RecordClassDescriptor[] fieldDescriptors = classDescriptors((ClassDataType) elementDataType);
                    for (int j = 0; j < fieldDescriptors.length; ++j) {
                        fieldDescriptors[j] = (RecordClassDescriptor) copy(fieldDescriptors[j], copy);
                    }
                    fields[i] = copyField(field,
                        new ArrayDataType(arrayDataType.isNullable(),
                            new ClassDataType(elementDataType.isNullable(), fieldDescriptors)
                        )
                    );
                }
            } else if (type instanceof EnumDataType) {
                EnumDataType enumDataType = (EnumDataType) type;
                EnumClassDescriptor ecd = (EnumClassDescriptor) copy(enumDataType.descriptor, copy);
                fields[i] = copyField(field, new EnumDataType(enumDataType.isNullable(), ecd));
            }
        }

        return fields;
    }

    private static DataField copyField(DataField field, DataType type) {
        DataField newField;
        if (field instanceof StaticDataField) {
            StaticDataField f = (StaticDataField) field;
            newField = new StaticDataField(f.getName(), f.getTitle(), type, f.getStaticValue());
        } else {
            NonStaticDataField f = (NonStaticDataField) field;
            newField = new NonStaticDataField(
                f.getName(), f.getTitle(), type, f.isPk(), f.getRelativeTo(), false //f.isDisplayIdentifier()
            );
            newField.setAttributes(f.getAttributes());
        }
        newField.setDescription(field.getDescription());

        return newField;
    }

    private static RecordClassDescriptor[] classDescriptors(ClassDataType classDataType) {
        return classDataType.isFixed()
            ? new RecordClassDescriptor[]{ classDataType.getFixedDescriptor() }
            : Arrays.copyOf(classDataType.getDescriptors(), classDataType.getDescriptors().length);
    }

    // returns type -> [field name -> field]
    private static TypeFields mergeFields(TypeFields streamFields, CompiledQuery... queries) {
        TypeFields mergedFields = new TypeFields();
        for (int i = 0; i < queries.length; ++i) {
            if (!isStarQuery(queries[i])) {
                ClassDataType classDataType = queries[i].type.getOutputType();
                RecordClassDescriptor[] descriptors = classDataType.getDescriptors();
                for (RecordClassDescriptor descriptor : descriptors) {
                    // stream descriptor can't be merged
                    if (streamFields.contains(descriptor.getName())) {
                        continue;
                    }

                    List<DataField> fields = QQLCompilerUtils.collectFields(descriptor);
                    for (DataField field : fields) {
                        DataField currentField = mergedFields.get(descriptor.getName(), field.getName());
                        if (currentField != null) {
                            if (!QQLCompilerUtils.typesAreEqual(field.getType(), currentField.getType())) {
                                throw new CompilationException("Output types are different for field " + field.getName(), 0);
                            }
                        } else {
                            mergedFields.put(descriptor.getName(), field);
                        }
                    }
                }
            }
        }

        mergedFields.putAll(streamFields);
        return mergedFields;
    }

    private static Map<String, RecordClassDescriptor> unionOutputTypes(TypeFields mergedFields, CompiledQuery... queries) {
        Map<String, RecordClassDescriptor> outputTypes = new HashMap<>();
        for (int i = 0; i < queries.length; ++i) {
            if (isStarQuery(queries[i])) {
                RecordClassDescriptor[] descriptors = queries[i].type.getOutputType().getDescriptors();
                for (int j = 0; j < descriptors.length; ++j) {
                    // todo: case sensitive?
                    outputTypes.put(descriptors[j].getName(), descriptors[j]);
                }
            }
        }

        for (int i = 0; i < queries.length; ++i) {
            CompiledQuery query = queries[i];
            if (!isStarQuery(query) && query instanceof CompiledFilter) {
                CompiledFilter filter = (CompiledFilter) query;
                RecordClassDescriptor[] descriptors = ((ClassDataType) filter.selector.type).getDescriptors();
                List<RecordClassDescriptor> newDescriptors = new ArrayList<>();
                List<CompiledExpression<?>> newExpressions = new ArrayList<>();
                Map<RecordClassDescriptor, List<CompiledExpression<?>>> typeToExpressions = new LinkedHashMap<>();
                Map<RecordClassDescriptor, CompiledExpression<?>> typeToCondition = new LinkedHashMap<>();
                for (RecordClassDescriptor descriptor : descriptors) {
                    String typeName = descriptor.getName();
                    List<DataField> typeFields = QQLCompilerUtils.collectFields(descriptor);
                    List<CompiledExpression<?>> initializers = filter.selector.typeToExpressions.get(descriptor);
                    Set<String> usedFields = typeFields.stream()
                        .map(t -> t.getName().toUpperCase()).collect(Collectors.toSet());
                    List<DataField> fields = mergedFields.get(typeName);
                    if (fields != null) {
                        List<DataField> newFields = new ArrayList<>();
                        List<CompiledExpression<?>> expressions = new ArrayList<>();
                        for (int j = 0; j < fields.size(); ++j) {
                            DataField field = fields.get(j);
                            int index = findField(typeFields, field);

                            newFields.add(field);
                            usedFields.remove(field.getName().toUpperCase());
                            if (index >= 0) {
                                if (!QQLCompilerUtils.typesAreEqual(typeFields.get(index).getType(), field.getType())) {
                                    throw new CompilationException("Field '" + field.getName() + "' has different output types.", 0);
                                }
                                expressions.add(initializers.get(index));
                            } else {
                                if (field.getType().isNullable()) {
                                    expressions.add(new CompiledNullConstant(field.getType(), field.getName()));
                                } else {
                                    throw new CompilationException("Field '" + field.getName() + "' is not nullable.", 0);
                                }
                            }
                        }

                        if (usedFields.size() > 0) {
                            throw new CompilationException(
                                "Can't extend type '" + typeName + "' with fields " + String.join(",", usedFields), 0
                            );
                        }

                        RecordClassDescriptor newDescriptor = outputTypes.get(typeName);
                        if (newDescriptor == null) {
                            outputTypes.put(typeName, newDescriptor = new RecordClassDescriptor(
                                typeName, null, false, null,
                                newFields.toArray(new DataField[0])
                            ));
                        }
                        newDescriptors.add(newDescriptor);
                        newExpressions.addAll(expressions);
                        typeToExpressions.computeIfAbsent(newDescriptor, k -> new ArrayList<>())
                            .addAll(expressions);
                        typeToCondition.put(newDescriptor, filter.selector.typeToCondition.get(descriptor));
                    }
                }

                ClassDataType type = new ClassDataType(false, newDescriptors.toArray(new RecordClassDescriptor[0]));
                filter.selector = new TupleConstructor(
                    type, filter.selector.args[0], filter.selector.args[1], //filter.selector.args[2],
                    typeToExpressions, typeToCondition
                );
            }
        }

        return outputTypes;
    }

    private static TypeFields buildStreamFields(CompiledQuery... queries) {
        TypeFields streamFields = new TypeFields();
        for (int i = 0; i < queries.length; ++i) {
            if (isStarQuery(queries[i])) {
                RecordClassDescriptor[] descriptors = queries[i].type.getOutputType().getDescriptors();
                for (RecordClassDescriptor descriptor : descriptors) {
                    if (!streamFields.contains(descriptor.getName())) {
                        List<DataField> fields = QQLCompilerUtils.collectFields(descriptor);
                        for (DataField field : fields) {
                            streamFields.put(descriptor.getName(), field);
                        }
                    }
                }
            }
        }

        return streamFields;
    }

    private static void validateOutputTypesMatches(CompiledQuery... queries) {
        for (CompiledQuery query1 : queries) {
            for (CompiledQuery query2 : queries) {
                if (query1 == query2) {
                    continue;
                }

                if (isStarQuery(query1) && isStarQuery(query2)) {
                    validateTypesMatches(
                        buildAllDescriptors(query1.type.getOutputType().getDescriptors()),
                        buildAllDescriptors(query2.type.getOutputType().getDescriptors())
                    );
                }
            }
        }
    }

    private static RecordClassDescriptor[] buildAllDescriptors(RecordClassDescriptor[] descriptors) {
        return Arrays.stream(new RecordClassSet(descriptors).getClassDescriptors())
            .filter(d -> d instanceof RecordClassDescriptor)
            .toArray(RecordClassDescriptor[]::new);
    }

    private static void validateTypesMatches(RecordClassDescriptor[] descriptors1, RecordClassDescriptor[] descriptors2) {
        for (RecordClassDescriptor descriptor1 : descriptors1) {
            for (RecordClassDescriptor descriptor2 : descriptors2) {
                if (descriptor1.getName().equals(descriptor2.getName())) {
                    validateTypesMatches(descriptor1, descriptor2);
                }
            }
        }
    }

    private static void validateTypesMatches(RecordClassDescriptor descriptor1, RecordClassDescriptor descriptor2) {
        DataField[] fields1 = fields(descriptor1);
        DataField[] fields2 = fields(descriptor2);

        if (fields1.length != fields2.length) {
            throw new CompilationException("Types are not compatible (have different fields count): " + descriptor1.getName(), 0);
        }

        for (int i = 0; i < fields1.length; ++i) {
            DataField field1 = fields1[i];
            DataField field2 = fields2[i];
            if (!QQLCompilerUtils.fieldsAreEqual(field1, field2)) {
                throw new CompilationException("Types are not compatible (have different fields): " + descriptor1.getName(), 0);
            }
        }
    }

    private static DataField[] fields(RecordClassDescriptor descriptor) {
        List<DataField> fields = new ArrayList<>();
        fields(descriptor, fields);
        return fields.toArray(new DataField[0]);
    }

    private static void fields(RecordClassDescriptor descriptor, List<DataField> fields) {
        RecordClassDescriptor parent = descriptor.getParent();
        if (parent != null) {
            fields(parent, fields);
        }

        for (DataField field : descriptor.getFields()) {
            if (field instanceof NonStaticDataField) {
                fields.add(field);
            }
        }
    }

    private static int findField(List<DataField> fields, DataField field) {
        for (int i = 0; i < fields.size(); ++i) {
            if (fields.get(i).getName().equals(field.getName())) {
                return i;
            }
        }

        return -1;
    }

    private static boolean isStarQuery(CompiledQuery query) {
        return query instanceof StreamSelector || (query instanceof CompiledFilter && ((CompiledFilter) query).someFormOfSelectStar);
    }

    static boolean isIdentifiers(Expression... expressions) {
        if (expressions.length < 1) {
            return false;
        }

        for (int i = 0; i < expressions.length; ++i) {
            if (!(expressions[i] instanceof Identifier)) {
                return false;
            }
        }

        return true;
    }
}
