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
package com.epam.deltix.util.parquet;

import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.codec.RecordLayout;
import com.epam.deltix.qsrv.hf.pub.md.*;
import org.apache.parquet.schema.*;

import java.util.ArrayList;
import java.util.List;

public class ParquetUtil {

    public static MessageType createParquetSchema(String name, RecordClassDescriptor... descriptors) {
        return new MessageType(name, createRootMessageTypes(descriptors));
    }

    private static List<Type> createRootMessageTypes(RecordClassDescriptor... descriptors) {
        List<Type> types = new ArrayList<>();

        types.addAll(createAbstractMessageTypes());
        types.addAll(createRecordClassType(descriptors));

        return types;
    }

    private static List<Type> createAbstractMessageTypes() {
        List<Type> types = new ArrayList<>();
        types.add(new PrimitiveType(
                Type.Repetition.REQUIRED,
                PrimitiveType.PrimitiveTypeName.INT64,
                "timestamp"
        ));
        types.add(new PrimitiveType(
                Type.Repetition.REQUIRED,
                PrimitiveType.PrimitiveTypeName.INT64,
                "nanoTime"
        ));
        types.add(Types.required(PrimitiveType.PrimitiveTypeName.BINARY)
                .as(LogicalTypeAnnotation.stringType())
                .named("symbol"));
        types.add(Types.required(PrimitiveType.PrimitiveTypeName.BINARY)
                .as(LogicalTypeAnnotation.enumType())
                .named("instrumentType"));
        return types;
    }

    private static List<Type> createRecordClassType(RecordClassDescriptor... descriptors) {
        List<Type> types = new ArrayList<>();

        if (descriptors.length == 1)
            types.addAll(createFieldTypes(descriptors[0]));
        else
            types.addAll(createPolymorphicTypes(descriptors));

        return types;
    }

    private static List<Type> createPolymorphicTypes(RecordClassDescriptor... descriptors) {
        List<Type> types = new ArrayList<>();
        for (ClassDescriptor descriptor : descriptors)
            types.add(new GroupType(Type.Repetition.OPTIONAL, descriptor.getName(), createFieldTypes((RecordClassDescriptor) descriptor)));

        return types;
    }

    private static List<Type> createFieldTypes(RecordClassDescriptor descriptor) {
        List<Type> types = new ArrayList<>();
        NonStaticFieldLayout[] fields = new RecordLayout(descriptor).getNonStaticFields();
        for (NonStaticFieldLayout field : fields) {
            String fieldName = field.getName();
            types.add(createFieldType(fieldName, field.getType()));
        }
        return types;
    }

    private static Type createFieldType(String name, DataType type) {
        if (type instanceof ClassDataType) {
            return new GroupType(
                    getRepetition(type.isNullable()),
                    name,
                    createRecordClassType(((ClassDataType) type).getDescriptors())
            );
        } else if (type instanceof ArrayDataType) {
            DataType elementType = ((ArrayDataType) type).getElementDataType();
            if (elementType instanceof ClassDataType)
                return new GroupType(
                        Type.Repetition.REPEATED,
                        name,
                        createRecordClassType(((ClassDataType) elementType).getDescriptors())
                );
            else if (elementType instanceof ArrayDataType)
                throw new UnsupportedOperationException("Nested arrays are not supported");
            else
                return buildPrimitiveType(Type.Repetition.REPEATED, elementType, name);
        } else {
            return buildPrimitiveType(getRepetition(type.isNullable()), type, name);
        }
    }

    private static Type.Repetition getRepetition(boolean isNullable) {
        return isNullable ? Type.Repetition.OPTIONAL : Type.Repetition.REQUIRED;
    }

    private static PrimitiveType buildPrimitiveType(Type.Repetition repetition, DataType type, String name) {
        String encoding = type.getEncoding();

        if (type.getClass() == FloatDataType.class) {
            if (encoding != null && encoding.equals(FloatDataType.ENCODING_FIXED_FLOAT))
                return new PrimitiveType(repetition, PrimitiveType.PrimitiveTypeName.FLOAT, name);
            else
                return new PrimitiveType(repetition, PrimitiveType.PrimitiveTypeName.DOUBLE, name);
        } else if (type.getClass() == IntegerDataType.class) {
            if (encoding == null || encoding.equals(IntegerDataType.ENCODING_INT64))
                return new PrimitiveType(repetition, PrimitiveType.PrimitiveTypeName.INT64, name);
            else if (encoding.equals(IntegerDataType.ENCODING_INT48))
                return new PrimitiveType(repetition, PrimitiveType.PrimitiveTypeName.INT64, name);
            else
                return new PrimitiveType(repetition, PrimitiveType.PrimitiveTypeName.INT32, name);
        } else if (type.getClass() == VarcharDataType.class) {
            return Types.primitive(PrimitiveType.PrimitiveTypeName.BINARY, repetition)
                    .as(LogicalTypeAnnotation.stringType())
                    .named(name);
        } else if (type.getClass() == CharDataType.class) {
            return Types.primitive(PrimitiveType.PrimitiveTypeName.INT32, repetition)
                    .as(LogicalTypeAnnotation.intType(16, true))
                    .named(name);
        } else if (type.getClass() == DateTimeDataType.class) {
            return new PrimitiveType(repetition, PrimitiveType.PrimitiveTypeName.INT64, name);
        } else if (type.getClass() == BooleanDataType.class) {
            return new PrimitiveType(repetition, PrimitiveType.PrimitiveTypeName.BOOLEAN, name);
        } else if (type.getClass() == EnumDataType.class) {
            return Types.primitive(PrimitiveType.PrimitiveTypeName.BINARY, repetition)
                    .as(LogicalTypeAnnotation.enumType())
                    .named(name);
        } else if (type.getClass() == TimeOfDayDataType.class) {
            return new PrimitiveType(repetition, PrimitiveType.PrimitiveTypeName.INT32, name);
        } else if (type.getClass() == BinaryDataType.class) {
            return new PrimitiveType(repetition, PrimitiveType.PrimitiveTypeName.BINARY, name);
        } else {
            throw new IllegalArgumentException("Unsupported type " + type);
        }
    }

//    public static void main(String[] args) {
//        MessageType type = createParquetSchema("test", StreamConfigurationHelper.mkSecurityMetaInfoDescriptors());
//        System.out.println(type);
//    }

}
