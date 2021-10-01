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

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.GroupBySpec;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.MismatchTypesException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.messages.QueryStatusMessage;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.parsers.CompilationException;

import java.util.*;

public class QQLCompilerUtils {

    static void checkTypesCompatibility(FieldAccessorExpression e, List<DataFieldRef> dfrs) {
        for (DataFieldRef dfr : dfrs) {
            if (dfr.field instanceof StaticDataField) {
                throw new CompilationException("Static types are restricted for nested objects", e);
            }
        }

        if (dfrs.size() <= 1) {
            return;
        }

        DataType[] types = dfrs.stream().map(d -> d.field.getType()).toArray(DataType[]::new);
        checkTypesAreEqual(e, types);
    }

    public static void checkTypesAreEqual(Expression e, DataType[] types) {
        DataType baseType = types[0];
        for (int i = 1; i < types.length; ++i) {
            DataType type = types[i];
            if (baseType instanceof ClassDataType) {
                if (!(type instanceof ClassDataType)) {
                    throw new MismatchTypesException(e, types);
                }
            } else if (baseType instanceof ArrayDataType) {
                if (type instanceof ArrayDataType) {
                    if (!simpleTypesAreEquals(((ArrayDataType) baseType).getElementDataType(), ((ArrayDataType) type).getElementDataType())) {
                        throw new MismatchTypesException(e, types);
                    }
                } else {
                    throw new MismatchTypesException(e, types);
                }
            } else if (baseType instanceof EnumDataType && type instanceof EnumDataType) {
                EnumDataType baseEnumType = (EnumDataType) baseType;
                EnumDataType enumType = (EnumDataType) type;
                if (!baseEnumType.getBaseName().equalsIgnoreCase(enumType.getBaseName())) {
                    throw new MismatchTypesException(e, types);
                }
            } else {
                if (!simpleTypesAreEquals(baseType, type)) {
                    throw new MismatchTypesException(e, types);
                }
            }
        }
    }

    public static boolean fieldsAreEqual(DataField f1, DataField f2) {
        if (f1 != null && f2 != null) {
            if (!f1.getName().equalsIgnoreCase(f2.getName())) {
                return false;
            }

            return typesAreEqual(f1.getType(), f2.getType());
        }

        return Util.xequals(f1, f2);
    }

    public static boolean typesAreEqual(DataType t1, DataType t2) {
        if (t1 instanceof ClassDataType) {
            if (t2 instanceof ClassDataType) {
                RecordClassDescriptor[] descriptors1 = ((ClassDataType) t1).getDescriptors();
                RecordClassDescriptor[] descriptors2 = ((ClassDataType) t2).getDescriptors();
                if (descriptors1.length != descriptors2.length) {
                    return false;
                } else {
                    for (int i = 0; i < descriptors1.length; ++i) {
                        if (!descriptors1[i].getName().equalsIgnoreCase(descriptors2[i].getName())) {
                            return false;
                        }
                    }
                }
            } else {
                return false;
            }
        } else if (t1 instanceof ArrayDataType) {
            if (t2 instanceof ArrayDataType) {
                return typesAreEqual(((ArrayDataType) t1).getElementDataType(), ((ArrayDataType) t2).getElementDataType());
            } else {
                return false;
            }
        } else if (t1 instanceof EnumDataType && t2 instanceof EnumDataType) {
            EnumDataType baseEnumType = (EnumDataType) t1;
            EnumDataType enumType = (EnumDataType) t2;
            if (!baseEnumType.getBaseName().equalsIgnoreCase(enumType.getBaseName())) {
                return false;
            }
        } else {
            return simpleTypesAreEquals(t1, t2);
        }

        return true;
    }

    private static boolean simpleTypesAreEquals(DataType t1, DataType t2) {
        if (t1.getClass() != t2.getClass()) {
            return false;
        } else if (!Util.xequals(t1.getEncoding(), t2.getEncoding())) {
            return false;
        }

        return true;
    }

    static DataType combinePolymorphicTypes(List<DataType> types) {
        DataType baseType = types.get(0);
        if (baseType instanceof ClassDataType) {
            return new ClassDataType(true,
                types.stream()
                    .flatMap(t -> Arrays.stream(((ClassDataType) t).getDescriptors()))
                    .toArray(RecordClassDescriptor[]::new)
            );
        } else if (baseType instanceof ArrayDataType) {
            return new ArrayDataType(true,
                new ClassDataType(true,
                    types.stream()
                        .flatMap(t -> Arrays.stream(((ClassDataType) ((ArrayDataType) t).getElementDataType()).getDescriptors()))
                        .toArray(RecordClassDescriptor[]::new)
                )
            );
        }

        return baseType;
    }

    static DataType makeSlicedType(DataType type) {
        if (type instanceof ArrayDataType) {
            return new ArrayDataType(
                true, ((ArrayDataType) type).getElementDataType().nullableInstance(true)
            );
        } else {
            return new ArrayDataType(true, type.nullableInstance(true));
        }
    }

    static boolean containsThis(Expression e) {
        if (e instanceof Identifier) {
            Identifier identifier = (Identifier) e;
            if ("this".equalsIgnoreCase(identifier.id)) {
                return true;
            }
        } else if (e instanceof ComplexExpression) {
            ComplexExpression complexExpression = (ComplexExpression) e;
            for (int i = 0; i < complexExpression.args.length; ++i) {
                if (containsThis(complexExpression.args[i])) {
                    return true;
                }
            }
        }

        return false;
    }

    static boolean findType(RecordClassDescriptor[] descriptors, RecordClassDescriptor type) {
        for (int i = 0; i < descriptors.length; ++i) {
            if (QQLCompilerUtils.findType(descriptors[i], type)) {
                return true;
            }
        }

        return false;
    }

    static boolean findType(RecordClassDescriptor concreteType, RecordClassDescriptor type) {
        if (concreteType == null) {
            return false;
        } else if (concreteType.equals(type)) {
            return true;
        }

        return findType(concreteType.getParent(), type);
    }

    public static List<DataField> collectFields(RecordClassDescriptor descriptor) {
        return collectFields(descriptor, false);
    }

    public static List<DataField> collectFields(RecordClassDescriptor descriptor, boolean includeStatic) {
        List<DataField> fields = new ArrayList<>();
        collectFields(descriptor, fields, includeStatic);
        return fields;
    }

    public static void collectFields(RecordClassDescriptor descriptor, List<DataField> fields, boolean includeStatic) {
        if (descriptor.getParent() != null) {
            collectFields(descriptor.getParent(), fields, includeStatic);
        }

        for (DataField field : descriptor.getFields()) {
            if (includeStatic) {
                fields.add(field);
            } else {
                if (field instanceof NonStaticDataField) {
                    fields.add(field);
                }
            }
        }
    }

    public static Expression[] flattenUnion(UnionExpression e) {
        List<Expression> flat = new ArrayList<>();
        flattenUnion(e, flat, 0);
        return flat.toArray(new Expression[0]);
    }

    public static void flattenUnion(UnionExpression e, List<Expression> flat, int depth) {
        for (int i = 0; i < e.args.length; ++i) {
            if (e.args[i] instanceof UnionExpression) {
                if (depth > 0 && e.limit != null) {
                    throw new CompilationException("Limit clause can be specified only for top level of union", e.limit);
                }
                flattenUnion((UnionExpression) e.args[i], flat, depth + 1);
            } else {
                flat.add(e.args[i]);
            }
        }
    }

    static QueryDataType addQueryStatusType(QueryDataType base, GroupBySpec groupBySpec) {
        if (groupBySpec == null) {
            return base;
        }

        RecordClassDescriptor[] baseRCDs = base.getOutputType().getDescriptors();
        int n = baseRCDs.length;
        for (int ii = 0; ii < n; ii++) {
            if (QueryStatusMessage.CLASS_NAME.equals(baseRCDs[ii].getName())) {
                return (base);
            }
        }

        RecordClassDescriptor[] outRCDs = new RecordClassDescriptor[n + 1];
        System.arraycopy(baseRCDs, 0, outRCDs, 0, n);
        try {
            outRCDs[n] = Introspector.createEmptyMessageIntrospector().introspectRecordClass(QueryStatusMessage.class);
        } catch (Introspector.IntrospectionException e) {
            throw new RuntimeException(e);
        }

        return (new QueryDataType(false, new ClassDataType(false, outRCDs)));
    }

}