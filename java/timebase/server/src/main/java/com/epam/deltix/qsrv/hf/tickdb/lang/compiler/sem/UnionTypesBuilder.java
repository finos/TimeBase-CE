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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.pub.md.ClassDataType;
import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.*;
import com.epam.deltix.util.parsers.CompilationException;

import java.util.*;
import java.util.stream.Collectors;

public class UnionTypesBuilder {

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

    public static Map<String, RecordClassDescriptor> buildType(CompiledQuery[] queries) {
        validateOutputTypes(queries);
        TypeFields streamFields = buildStreamFields(queries);
        TypeFields outputFields = mergeFields(queries, streamFields);
        return buildOutputTypes(queries, outputFields);
    }

    // returns type -> [field name -> field]
    private static TypeFields mergeFields(CompiledQuery[] queries, TypeFields streamFields) {
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

    private static Map<String, RecordClassDescriptor> buildOutputTypes(CompiledQuery[] queries, TypeFields mergedFields) {
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
                for (RecordClassDescriptor descriptor : descriptors) {
                    String typeName = descriptor.getName();
                    List<DataField> typeFields = QQLCompilerUtils.collectFields(descriptor);
                    List<CompiledExpression<?>> initializers = filter.selector.typeToInitializers.get(descriptor);
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
                    }
                }

                ClassDataType type = new ClassDataType(false, newDescriptors.toArray(new RecordClassDescriptor[0]));
                filter.selector = new TupleConstructor(
                    type, filter.selector.args[0], filter.selector.args[1],
                    typeToExpressions,
                    newExpressions.toArray(new CompiledExpression[0])
                );
            }
        }

        return outputTypes;
    }

    private static TypeFields buildStreamFields(CompiledQuery[] queries) {
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

    private static void validateOutputTypes(CompiledQuery[] queries) {
        for (CompiledQuery query1 : queries) {
            for (CompiledQuery query2 : queries) {
                if (query1 == query2) {
                    continue;
                }

                if (isStarQuery(query1) && isStarQuery(query2)) {
                    strictValidateTypes(
                        query1.type.getOutputType().getDescriptors(),
                        query2.type.getOutputType().getDescriptors()
                    );
                }
            }
        }
    }

    private static void strictValidateTypes(RecordClassDescriptor[] descriptors1, RecordClassDescriptor[] descriptors2) {
        for (RecordClassDescriptor descriptor1 : descriptors1) {
            for (RecordClassDescriptor descriptor2 : descriptors2) {
                // todo: case sensitive?
                if (descriptor1.getName().equalsIgnoreCase(descriptor2.getName())) {
                    strictValidateTypes(descriptor1, descriptor2);
                }
            }
        }
    }

    private static void strictValidateTypes(RecordClassDescriptor descriptor1, RecordClassDescriptor descriptor2) {
        if (descriptor1.getFields().length != descriptor2.getFields().length) {
            throw new CompilationException("Types are not compatible (have different fields): " + descriptor1.getName(), 0);
        }

        DataField[] fields1 = descriptor1.getFields();
        DataField[] fields2 = descriptor2.getFields();
        for (int i = 0; i < fields1.length; ++i) {
            DataField field1 = fields1[i];
            DataField field2 = fields2[i];
            if (!QQLCompilerUtils.fieldsAreEqual(field1, field2)) {
                throw new CompilationException("Types are not compatible (have different fields): " + descriptor1.getName(), 0);
            }
        }
    }

    private static int findField(List<DataField> fields, DataField field) {
        for (int i = 0; i < fields.size(); ++i) {
            // todo: case sensitive?
            if (fields.get(i).getName().equalsIgnoreCase(field.getName())) {
                return i;
            }
        }

        return -1;
    }

    private static boolean isStarQuery(CompiledQuery query) {
        return query instanceof StreamSelector || (query instanceof CompiledFilter && ((CompiledFilter) query).someFormOfSelectStar);
    }
}