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
package com.epam.deltix.qsrv.solgen.cpp;

import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.codec.RecordLayout;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.solgen.CodegenUtils;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.text.tte.TinyTemplateEngine;
import com.epam.deltix.util.text.tte.Values;

import java.io.*;
import java.text.ParseException;
import java.util.*;

public class CppCodecGenerator {

    public static final String INDENT = CodegenUtils.INDENT1;
    public static final String INDENT2 = CodegenUtils.INDENT2;
    public static final String INDENT3 = CodegenUtils.INDENT3;
    public static final String INDENT4 = CodegenUtils.INDENT4;
    public static final String INDENT5 = CodegenUtils.INDENT5;

    public static final String NL = CodegenUtils.NL;
    public static final String NL2 = NL + NL;

    public static final String NAMESPACE_PREFIX = "dx_";
    public static final String CLASS_NAME_PREFIX = "Dx_";

    public static final String STREAM_CODEC = "StreamCodec";
    public static final String STREAM_ENCODER = "StreamEncoder";
    public static final String STREAM_DECODER = "StreamDecoder";

    public static final String ROOT_MESSAGE_TYPE = "NativeMessage";
    private static final String CLASS_CPP_TYPE = "shared_ptr<" + ROOT_MESSAGE_TYPE + ">";
    private static final String STRING_CPP_TYPE = "string";
    private static final String ARRAY_CPP_TYPE = "vector";
    private static final String BYTE_CPP_TYPE = "int8_t";
    private static final String UNSIGNED_BYTE_CPP_TYPE = "uint8_t";
    private static final String BINARY_CPP_TYPE = ARRAY_CPP_TYPE + "<" + UNSIGNED_BYTE_CPP_TYPE + ">";

    private final RecordClassDescriptor descriptor;
    private final NonStaticFieldLayout[] layouts;
    private final String streamKey;
    private final String className;
    private final String namespace;
    private final String parentClassName;
    private final String parentClassNamespace;
    private final String fileName;

    public CppCodecGenerator(String streamKey, RecordClassDescriptor descriptor) {
        this.descriptor = descriptor;
        NonStaticFieldLayout[] layouts = new RecordLayout(descriptor).getDeclaredNonStaticFields();
        this.layouts = layouts != null ? layouts : new NonStaticFieldLayout[0];
        this.streamKey = streamKey;
        this.className = getSimpleClassName(descriptor.getName());
        this.namespace = getNamespace(descriptor.getName(), streamKey);

        RecordClassDescriptor parentDescriptor = descriptor.getParent();
        this.parentClassName = parentDescriptor == null ? ROOT_MESSAGE_TYPE : getSimpleClassName(parentDescriptor.getName());
        this.parentClassNamespace = parentDescriptor == null ? null : getNamespace(parentDescriptor.getName(), streamKey);

        this.fileName = "codecs/" + streamKey + "/" + className + ".h";
    }

    public String getFileName() {
        return fileName;
    }

    public String getCodec() {
        final StringBuilder code = new StringBuilder();

        generateHead(code, descriptor, layouts);

        final TreeMap<String, String> fieldToType = new TreeMap<>();
        final TreeMap<String, String> fieldToNull = new TreeMap<>();
        generateCodecMethods(code, layouts, fieldToType, fieldToNull);
        generateClearMethod(code, fieldToType, fieldToNull);
        generateSetters(code, fieldToType);
        generateToString(code, fieldToType, fieldToNull);
        generateFields(code, fieldToType);

        generateFooter(code);

        return code.toString();
    }

    private void generateCodecMethods(StringBuilder code, NonStaticFieldLayout[] layouts,
                                      TreeMap<String, String> fieldToType, TreeMap<String, String> fieldToNull)
    {
        final StringBuilder encoderSource = new StringBuilder();

        code.append(INDENT).append("virtual void decode(DataReader &reader) {").append(NL);
        if (!ROOT_MESSAGE_TYPE.equals(parentClassName))
            code.append(INDENT2).append(parentClassName).append("::decode(reader);").append(NL);

        encoderSource.append(INDENT).append("virtual void encode(DataWriter &writer) {").append(NL);
        if (!ROOT_MESSAGE_TYPE.equals(parentClassName))
            encoderSource.append(INDENT2).append(parentClassName).append("::encode(writer);").append(NL);

        for (int i = 0, iCount = layouts.length; i < iCount; i++) {
            final DataType type = layouts[i].getType();

            String layoutName = layouts[i].getName();
            String cppDataType = getCppType(type, layoutName);
            String nullValue = getNullValue(type);

            generateLayoutCodec(code, encoderSource, layoutName, type, layouts[i].getRelativeTo(), 2, false);

            if (!fieldToType.containsKey(layoutName))
                fieldToType.put(layoutName, cppDataType);

            if (!fieldToNull.containsKey(layoutName))
                fieldToNull.put(layoutName, nullValue);
        }

        code.append(INDENT).append("}").append(NL2);
        encoderSource.append(INDENT).append("}").append(NL2);

        code.append(encoderSource);
    }

    private static String getCppType(final DataType type, final String layoutName) {
        final String encoding = type.getEncoding();
        String cppDataType = null;
        if (type.getClass() == FloatDataType.class) {
            if (encoding == null || encoding.equals(FloatDataType.ENCODING_FIXED_DOUBLE)) {
                cppDataType = "double";
            } else if (encoding.startsWith(FloatDataType.ENCODING_DECIMAL64)) {
                cppDataType = "Decimal64";
            } else if (encoding.startsWith(FloatDataType.ENCODING_SCALE_AUTO)) {
                cppDataType = "double";
            } else if (encoding.equals(FloatDataType.ENCODING_FIXED_FLOAT)) {
                cppDataType = "float";
            }
        } else if (type.getClass() == IntegerDataType.class) {
            IntegerDataType integerType = (IntegerDataType) type;
            if (encoding == null || integerType.getSize() == 8) {
                cppDataType = "int64_t";
            } else if (integerType.getSize() == 6) {
                cppDataType = "int64_t";
            } else if (integerType.getSize() == 1) {
                cppDataType = BYTE_CPP_TYPE;
            } else if (integerType.getSize() == 2) {
                cppDataType = "int16_t";
            } else if (integerType.getSize() == 4) {
                cppDataType = "int32_t";
            } else if (integerType.getSize() == IntegerDataType.PACKED_UNSIGNED_INT) {
                cppDataType = "uint32_t";
            } else if (integerType.getSize() == IntegerDataType.PACKED_UNSIGNED_LONG) {
                cppDataType = "uint64_t";
            } else if (integerType.getSize() == IntegerDataType.PACKED_INTERVAL) {
                cppDataType = "int32_t";
            }
        } else if (type.getClass() == VarcharDataType.class) {
            cppDataType = STRING_CPP_TYPE;
        } else if (type.getClass() == DateTimeDataType.class) {
            cppDataType = "int64_t";
        } else if (type.getClass() == BooleanDataType.class) {
            cppDataType = type.isNullable() ? UNSIGNED_BYTE_CPP_TYPE : "bool";
        } else if (type.getClass() == CharDataType.class) {
            cppDataType = "wchar_t";
        } else if (type.getClass() == EnumDataType.class) {
            int size = ((EnumDataType)type).descriptor.computeStorageSize();
            if (size == 1) {
                cppDataType = "int";
            } else if (size == 2) {
                cppDataType = "int";
            } else if (size == 4) {
                cppDataType = "int";
            } else if (size == 8) {
                cppDataType = "int64_t";
            }
        } else if (type.getClass() == TimeOfDayDataType.class) {
            cppDataType = "int32_t";
        } else if (type.getClass() == BinaryDataType.class) {
            cppDataType = BINARY_CPP_TYPE;
        } else if (type.getClass() == ArrayDataType.class) {
            cppDataType = ARRAY_CPP_TYPE + "<" + getCppType(((ArrayDataType) type).getElementDataType(), layoutName) + ">";
        } else if (type.getClass() == ClassDataType.class) {
            cppDataType = CLASS_CPP_TYPE;
        }

        if (cppDataType == null)
            throw new IllegalArgumentException("Unsupported encoding " + encoding + " for " + type.getClass().getName() + ". Field: " + layoutName);

        return cppDataType;
    }

    private static String getNullValue(final DataType type) {
        final String encoding = type.getEncoding();
        String nullValue = null;
        if (type.getClass() == FloatDataType.class) {
            if (encoding == null || encoding.equals(FloatDataType.ENCODING_FIXED_DOUBLE)) {
                nullValue = "DxApi::FLOAT64_NULL";
            } else if (encoding.startsWith(FloatDataType.ENCODING_DECIMAL64)) {
                nullValue = "D64_NULL";
            } else if (encoding.startsWith(FloatDataType.ENCODING_SCALE_AUTO)) {
                nullValue = "DxApi::DECIMAL_NULL";
            } else if (encoding.equals(FloatDataType.ENCODING_FIXED_FLOAT)) {
                nullValue = "DxApi::FLOAT32_NULL";
            }
        } else if (type.getClass() == IntegerDataType.class) {
            IntegerDataType integerType = (IntegerDataType) type;
            if (encoding == null || integerType.getSize() == 8) {
                nullValue = "DxApi::INT64_NULL";
            } else if (integerType.getSize() == 6) {
                nullValue = "DxApi::INT48_NULL";
            } else if (integerType.getSize() == 1) {
                nullValue = "DxApi::INT8_NULL";
            } else if (integerType.getSize() == 2) {
                nullValue = "DxApi::INT16_NULL";
            } else if (integerType.getSize() == 4) {
                nullValue = "DxApi::INT32_NULL";
            } else if (integerType.getSize() == IntegerDataType.PACKED_UNSIGNED_INT) {
                nullValue = "DxApi::UINT30_NULL";
            } else if (integerType.getSize() == IntegerDataType.PACKED_UNSIGNED_LONG) {
                nullValue = "DxApi::UINT61_NULL";
            } else if (integerType.getSize() == IntegerDataType.PACKED_INTERVAL) {
                nullValue = "DxApi::Constants::INTERVAL_NULL";
            }
        } else if (type.getClass() == VarcharDataType.class) {
            nullValue = null;
        } else if (type.getClass() == DateTimeDataType.class) {
            nullValue = "DxApi::TIMESTAMP_NULL";
        } else if (type.getClass() == BooleanDataType.class) {
            nullValue = type.isNullable() ? "Constants::BOOL_NULL" : null;
        } else if (type.getClass() == CharDataType.class) {
            nullValue = "DxApi::Constants::CHAR_NULL";
        } else if (type.getClass() == EnumDataType.class) {
            int size = ((EnumDataType)type).descriptor.computeStorageSize();
            if (size == 1) {
                nullValue = "DxApi::ENUM_NULL";
            } else if (size == 2) {
                nullValue = "DxApi::ENUM_NULL";
            } else if (size == 4) {
                nullValue = "DxApi::ENUM_NULL";
            } else if (size == 8) {
                nullValue = "DxApi::ENUM_NULL";
            }
        } else if (type.getClass() == TimeOfDayDataType.class) {
            nullValue = "DxApi::TIMEOFDAY_NULL";
        } else if (type.getClass() == BinaryDataType.class) {
            nullValue = null;
        } else if (type.getClass() == ArrayDataType.class) {
            nullValue = null;
        } else if (type.getClass() == ClassDataType.class) {
            nullValue = "nullptr";
        }

        return nullValue;
    }

    private static void appendIndents(StringBuilder code, int count, int relative) {
        for (int i = 0; i < count + relative; ++i)
            code.append(INDENT);
    }

    private void generateLayoutCodec(StringBuilder decoder, StringBuilder encoder,
                                            String layoutName, DataType type, NonStaticFieldLayout relativeTo,
                                            int indents, boolean isArrayValue)
    {
        final String fieldName = isArrayValue ? layoutName : getField(layoutName);

        boolean has = !isArrayValue;
        final String hasFieldName = getHasField(layoutName);

        final String encoding = type.getEncoding();
        if (type.getClass() == FloatDataType.class) {
            if (encoding == null || encoding.equals(FloatDataType.ENCODING_FIXED_DOUBLE)) {
                appendIndents(decoder, indents, 0);
                decoder.append(fieldName).append(" = reader.readFloat64()");
                if (relativeTo != null)
                    decoder.append(" + ").append(getField(relativeTo.getName()));
                decoder.append(";").append(NL);

                appendIndents(encoder, indents, 0);
                encoder.append("writer.writeFloat64(").append(fieldName);
                if (relativeTo != null)
                    encoder.append(" - ").append(getField(relativeTo.getName()));
                encoder.append(");").append(NL);
            } else if (encoding.startsWith(FloatDataType.ENCODING_DECIMAL64)) {
                appendIndents(decoder, indents, 0);
                decoder.append(fieldName).append(" = Decimal64::fromUnderlying((uint64_t) reader.readInt64())");
                if (relativeTo != null)
                    decoder.append(" + ").append(getField(relativeTo.getName()));
                decoder.append(";").append(NL);

                appendIndents(encoder, indents, 0);
                encoder.append("writer.writeUInt64(").append(fieldName).append(".toUnderlying()");
                if (relativeTo != null)
                    encoder.append(" - ").append(getField(relativeTo.getName()));
                encoder.append(");").append(NL);
            } else if (encoding.startsWith(FloatDataType.ENCODING_SCALE_AUTO)) {
                appendIndents(decoder, indents, 0);
                decoder.append(fieldName).append(" = reader.readDecimal()");
                if (relativeTo != null)
                    decoder.append(" + ").append(getField(relativeTo.getName()));
                decoder.append(";").append(NL);

                appendIndents(encoder, indents, 0);
                encoder.append("writer.writeDecimal(").append(fieldName);
                if (relativeTo != null)
                    encoder.append(" - ").append(getField(relativeTo.getName()));
                encoder.append(");").append(NL);
            } else if (encoding.equals(FloatDataType.ENCODING_FIXED_FLOAT)) {
                appendIndents(decoder, indents, 0);
                decoder.append(fieldName).append(" = reader.readFloat32()");
                if (relativeTo != null)
                    decoder.append(" + ").append(getField(relativeTo.getName()));
                decoder.append(";").append(NL);

                appendIndents(encoder, indents, 0);
                encoder.append("writer.writeFloat32(").append(fieldName);
                if (relativeTo != null)
                    encoder.append(" - ").append(getField(relativeTo.getName()));
                encoder.append(");").append(NL);
            }
        } else if (type.getClass() == IntegerDataType.class) {
            IntegerDataType integerDataType = (IntegerDataType) type;
            if (encoding == null || integerDataType.getSize() == 8) {
                appendIndents(decoder, indents, 0);
                decoder.append(fieldName).append(" = reader.readInt64();").append(NL);

                appendIndents(encoder, indents, 0);
                encoder.append("writer.writeInt64(").append(fieldName).append(");").append(NL);
            } else if (integerDataType.getSize() == 6) {
                appendIndents(decoder, indents, 0);
                decoder.append(fieldName).append(" = reader.readInt48();").append(NL);

                appendIndents(encoder, indents, 0);
                encoder.append("writer.writeInt48(").append(fieldName).append(");").append(NL);
            } else if (integerDataType.getSize() == 1) {
                appendIndents(decoder, indents, 0);
                decoder.append(fieldName).append(" = reader.readInt8();").append(NL);

                appendIndents(encoder, indents, 0);
                encoder.append("writer.writeInt8(").append(fieldName).append(");").append(NL);
            } else if (integerDataType.getSize() == 2) {
                appendIndents(decoder, indents, 0);
                decoder.append(fieldName).append(" = reader.readInt16();").append(NL);

                appendIndents(encoder, indents, 0);
                encoder.append("writer.writeInt16(").append(fieldName).append(");").append(NL);
            } else if (integerDataType.getSize() == 4) {
                appendIndents(decoder, indents, 0);
                decoder.append(fieldName).append(" = reader.readInt32();").append(NL);

                appendIndents(encoder, indents, 0);
                encoder.append("writer.writeInt32(").append(fieldName).append(");").append(NL);
            } else if (integerDataType.getSize() == IntegerDataType.PACKED_UNSIGNED_INT) {
                appendIndents(decoder, indents, 0);
                decoder.append(fieldName).append(" = reader.readUInt30();").append(NL);

                appendIndents(encoder, indents, 0);
                encoder.append("writer.writeUInt30(").append(fieldName).append(");").append(NL);
            } else if (integerDataType.getSize() == IntegerDataType.PACKED_UNSIGNED_LONG) {
                appendIndents(decoder, indents, 0);
                decoder.append(fieldName).append(" = reader.readUInt61();").append(NL);

                appendIndents(encoder, indents, 0);
                encoder.append("writer.writeUInteger61(").append(fieldName).append(");").append(NL);
            } else if (integerDataType.getSize() == IntegerDataType.PACKED_INTERVAL) {
                appendIndents(decoder, indents, 0);
                decoder.append(fieldName).append(" = reader.readInterval();").append(NL);

                appendIndents(encoder, indents, 0);
                encoder.append("writer.writeInterval(").append(fieldName).append(");").append(NL);
            }
        } else if (type.getClass() == VarcharDataType.class) {
            if (encoding == null || encoding.startsWith(VarcharDataType.ENCODING_ALPHANUMERIC)) {
                int encodingSize = 10;
                try {
                    encodingSize = VarcharDataType.extractSize(encoding);
                } catch (ParseException ex) {
                    throw new RuntimeException(ex);
                }
                appendIndents(decoder, indents, 0);
                if (has)
                    decoder.append(hasFieldName).append(" = reader.readAlphanumeric(").append(fieldName).append(", ").append(encodingSize).append(");").append(NL);
                else
                    decoder.append("reader.readAlphanumeric(").append(fieldName).append(", ").append(encodingSize).append(");").append(NL);

                appendIndents(encoder, indents, 0);
                if (has)
                    encoder.append(hasFieldName).append(" ? writer.writeAlphanumeric(").append(encodingSize).append(", ").append(fieldName).append(") : ")
                            .append(" writer.writeAlphanumericNull(").append(encodingSize).append(");").append(NL);
                else
                    encoder.append("writer.writeAlphanumeric(").append(encodingSize).append(", ").append(fieldName).append(");").append(NL);
            } else {
                appendIndents(decoder, indents, 0);
                if (has)
                    decoder.append(hasFieldName).append(" = reader.readUTF8(").append(fieldName).append(");").append(NL);
                else
                    decoder.append("reader.readUTF8(").append(fieldName).append(");").append(NL);

                appendIndents(encoder, indents, 0);
                if (has)
                    encoder.append("writer.writeUTF8(").append(hasFieldName).append(" ? &").append(fieldName).append(" : NULL);").append(NL);
                else
                    encoder.append("writer.writeUTF8(").append(fieldName).append(");").append(NL);
            }
        } else if (type.getClass() == DateTimeDataType.class) {
            appendIndents(decoder, indents, 0);
            decoder.append(fieldName).append(" = reader.readTimestamp();").append(NL);

            appendIndents(encoder, indents, 0);
            encoder.append("writer.writeTimestamp(").append(fieldName).append(");").append(NL);
        } else if (type.getClass() == BooleanDataType.class) {
            if (type.isNullable()) {
                appendIndents(decoder, indents, 0);
                decoder.append(fieldName).append(" = reader.readNullableBooleanInt8();").append(NL);

                appendIndents(encoder, indents, 0);
                encoder.append("writer.writeNullableBoolean(").append(fieldName).append(", ").append(fieldName).append(" == Constants::BOOL_NULL);").append(NL);
            } else {
                appendIndents(decoder, indents, 0);
                decoder.append(fieldName).append(" = reader.readBoolean();").append(NL);

                appendIndents(encoder, indents, 0);
                encoder.append("writer.writeBoolean(").append(fieldName).append(");").append(NL);
            }
        } else if (type.getClass() == CharDataType.class) {
            appendIndents(decoder, indents, 0);
            decoder.append(fieldName).append(" = reader.readWChar();").append(NL);

            appendIndents(encoder, indents, 0);
            encoder.append("writer.writeWChar(").append(fieldName).append(");").append(NL);
        } else if (type.getClass() == EnumDataType.class) {
            int size = ((EnumDataType)type).descriptor.computeStorageSize();
            if (size == 1) {
                appendIndents(decoder, indents, 0);
                decoder.append(fieldName).append(" = reader.readEnum8();").append(NL);

                appendIndents(encoder, indents, 0);
                encoder.append("writer.writeEnum8(").append(fieldName).append(");").append(NL);
            } else if (size == 2) {
                appendIndents(decoder, indents, 0);
                decoder.append(fieldName).append(" = reader.readEnum16();").append(NL);

                appendIndents(encoder, indents, 0);
                encoder.append("writer.writeEnum16(").append(fieldName).append(");").append(NL);
            } else if (size == 4) {
                appendIndents(decoder, indents, 0);
                decoder.append(fieldName).append(" = reader.readEnum32();").append(NL);

                appendIndents(encoder, indents, 0);
                encoder.append("writer.writeEnum32(").append(fieldName).append(");").append(NL);
            } else if (size == 8) {
                appendIndents(decoder, indents, 0);
                decoder.append(fieldName).append(" = reader.readEnum64();").append(NL);

                appendIndents(encoder, indents, 0);
                encoder.append("writer.writeEnum64(").append(fieldName).append(");").append(NL);
            } else {
                throw new IllegalArgumentException("Unsupported size of enum: " + size + ". Field: " + layoutName);
            }
        } else if (type.getClass() == TimeOfDayDataType.class) {
            appendIndents(decoder, indents, 0);
            decoder.append(fieldName).append(" = reader.readTimeOfDay();").append(NL);

            appendIndents(encoder, indents, 0);
            encoder.append("writer.writeTimeOfDay(").append(fieldName).append(");").append(NL);
        } else if (type.getClass() == BinaryDataType.class) {
            appendIndents(decoder, indents, 0);
            if (has)
                decoder.append(hasFieldName).append(" = reader.readBinary(").append(fieldName).append(");").append(NL);
            else
                decoder.append("reader.readBinary(").append(fieldName).append(");").append(NL);

            appendIndents(encoder, indents, 0);
            if (has) {
                encoder.append("if (!").append(hasFieldName).append(") { writer.writeBinaryArrayNull(); } ").append(NL);
                appendIndents(encoder, indents, 0);
                encoder.append("else { writer.writeBinaryArray(").append(fieldName).append(".data(), ").append(fieldName).append(".size()); }").append(NL);
            } else {
                encoder.append("writer.writeBinaryArray(").append(fieldName).append(".data(), ").append(fieldName).append(".size());").append(NL);
            }
        } else if (type.getClass() == ArrayDataType.class) {
            DataType elementType = ((ArrayDataType) type).getElementDataType();
            String varSize = getSizeField(layoutName);
            String varI = getIField(layoutName);

            //decoder
            appendIndents(decoder, indents, 0);
            decoder.append("int32_t ").append(varSize).append(" = reader.readArrayStart();").append(NL);
            if (has) {
                appendIndents(decoder, indents, 0);
                decoder.append("if (").append(varSize).append(" == INT32_NULL) {").append(hasFieldName).append(" = false; } else {").append(NL);
                appendIndents(decoder, indents, 1);
                decoder.append(hasFieldName).append(" = true;").append(NL);
            } else {
                appendIndents(decoder, indents, 0);
                decoder.append("{").append(NL);
            }
            appendIndents(decoder, indents, 1);
            decoder.append(fieldName).append(".resize(").append(varSize).append(");").append(NL);
            appendIndents(decoder, indents, 1);
            decoder.append("for (int ").append(varI).append(" = 0; ").append(varI).append(" < ").append(varSize)
                    .append("; ++").append(varI).append(") {")
                    .append(NL);

            //encoder
            appendIndents(encoder, indents, 0);
            if (has)
                encoder.append("if (!").append(hasFieldName).append(") { writer.writeArrayNull(); } else {").append(NL);
            else
                encoder.append("{").append(NL);

            appendIndents(encoder, indents, 1);
            encoder.append("writer.writeArrayStart(").append(fieldName).append(".size());").append(NL);
            appendIndents(encoder, indents, 1);
            encoder.append("for (int ").append(varI).append(" = 0; ")
                .append(varI).append(" < ").append(fieldName).append(".size(); ++").append(varI).append(") {")
                .append(NL);

            generateLayoutCodec(decoder, encoder, fieldName + "[" + varI + "]", elementType, null, indents + 2, true);

            //decoder
            appendIndents(decoder, indents, 1);
            decoder.append("}").append(NL);
            appendIndents(decoder, indents, 1);
            decoder.append("reader.readArrayEnd();").append(NL);
            appendIndents(decoder, indents, 0);
            decoder.append("}").append(NL);

            //encoder
            appendIndents(encoder, indents, 1);
            encoder.append("}").append(NL);
            appendIndents(encoder, indents, 1);
            encoder.append("writer.writeArrayEnd();").append(NL);
            appendIndents(encoder, indents, 0);
            encoder.append("}").append(NL);
        } else if (type.getClass() == ClassDataType.class) {
            /*
            int32_t var_type = reader.readObjectStart();
            if (var_type == Constants::INT32_NULL) {
                var = nullptr;
            } else if (var_type == 0) {
                var = shared_ptr<NativeMessage>(new RCD.name());
                var->decode(reader);
            } else if (var_type == 1) {
                var = shared_ptr<NativeMessage>(new RCd.name());
                var->decode(reader);
            } else {
                throw Exception("Unknown class type");
            }
            */

            RecordClassDescriptor[] descriptors = ((ClassDataType) type).getDescriptors();

            String varType = getTypeField(layoutName);

            //decoder
            appendIndents(decoder, indents, 0);
            decoder.append("int32_t ").append(varType).append(" = reader.readObjectStart();").append(NL);
            appendIndents(decoder, indents, 0);
            decoder.append("if (").append(varType).append(" == Constants::INT32_NULL) {").append(NL);
            appendIndents(decoder, indents, 1);
            decoder.append(fieldName).append(" = nullptr;").append(NL);
            for (int i = 0; i < descriptors.length; ++i) {
                appendIndents(decoder, indents, 0);
                decoder.append("} else if (").append(varType).append(" == ").append(i).append(") {").append(NL);
                appendIndents(decoder, indents, 1);

                String fullCppClassName = getCppFullClassName(streamKey, descriptors[i].getName());
                String classNamespace = getNamespace(descriptors[i].getName(), streamKey);
                if (this.namespace.equals(classNamespace))
                    fullCppClassName = getSimpleClassName(descriptors[i].getName());

                decoder.append(fieldName).append(" = shared_ptr<NativeMessage>(new ").append(fullCppClassName)
                        .append("());").append(NL);
                appendIndents(decoder, indents, 1);
                decoder.append(fieldName).append("->decode(reader);").append(NL);
                appendIndents(decoder, indents, 1);
                decoder.append("reader.readObjectEnd();").append(NL);
            }
            appendIndents(decoder, indents, 0);
            decoder.append("} else {").append(NL);
            appendIndents(decoder, indents, 1);
            decoder.append("throw runtime_error(\"Unknown class type\");").append(NL);
            appendIndents(decoder, indents, 0);
            decoder.append("}").append(NL);

            //encoder
            /*
            if (var == nullptr) { writer.writeObjectNull(); } else {
                if (!strcmp(var->getTypeName(), RDC.name()) {
                    writer.writeObjectStart(0);
                    var->encode(writer);
                    writer.writeObjectEnd();
                } else if ...
                } else {
                    throw Exception("Unknown class type");
                }
            }
            */
            appendIndents(encoder, indents, 0);
            encoder.append("if (").append(fieldName).append(" == nullptr) { writer.writeObjectNull(); } else {").append(NL);
            for (int i = 0; i < descriptors.length; ++i) {
                appendIndents(encoder, indents, 1);
                if (i == 0)
                    encoder.append("if ");
                else
                    encoder.append("} else if ");
                encoder.append("(!").append(fieldName).append("->getTypeName().compare(\"").append(descriptors[i].getName())
                        .append("\")) {").append(NL);

                appendIndents(encoder, indents, 2);
                encoder.append("writer.writeObjectStart(").append(i).append(");").append(NL);
                appendIndents(encoder, indents, 2);
                encoder.append(fieldName).append("->encode(writer);").append(NL);
                appendIndents(encoder, indents, 2);
                encoder.append("writer.writeObjectEnd();").append(NL);
            }
            appendIndents(encoder, indents, 1);
            encoder.append("} else {").append(NL);
            appendIndents(encoder, indents, 2);
            encoder.append("throw runtime_error(\"Unknown class type\");").append(NL);
            appendIndents(encoder, indents, 1);
            encoder.append("}").append(NL);
            appendIndents(encoder, indents, 0);
            encoder.append("}").append(NL);
        } else {
            throw new IllegalArgumentException("Unsupported type " + type + ". Field: " + layoutName);
        }
    }

    private void generateClearMethod(StringBuilder code, Map<String, String> fieldToType, Map<String, String> fieldToNull)
    {
        if (parentClassName == null)
            return;

        code.append(INDENT).append("inline void clear() {").append(NL);
        if (!ROOT_MESSAGE_TYPE.equals(parentClassName))
            code.append(INDENT2).append(parentClassName).append("::clear();").append(NL);

        for (Map.Entry<String, String> entry : fieldToType.entrySet()) {
            final String layoutName = entry.getKey();
            final String fieldName = getField(layoutName);
            final String cppType = entry.getValue();
            final String hasFieldName = getHasField(layoutName);
            final String nullValue = fieldToNull.get(layoutName);

            if (cppType.startsWith(ARRAY_CPP_TYPE) || cppType.startsWith(STRING_CPP_TYPE)) {
                code.append(INDENT2).append(fieldName).append(".clear();").append(NL);
                code.append(INDENT2).append(hasFieldName).append(" = false;").append(NL);
            } else {
                if (nullValue != null)
                    code.append(INDENT2).append(fieldName).append(" = ").append(nullValue).append(";").append(NL);
            }
        }

        code.append(INDENT).append("}").append(NL2);
    }

    private void generateSetters(StringBuilder code, TreeMap<String, String> fieldToType) {
        for (Map.Entry<String, String> entry : fieldToType.entrySet()) {
            final String layoutName = entry.getKey();
            final String fieldName = getField(layoutName);
            final String cppType = entry.getValue();
            final String hasFieldName = getHasField(layoutName);
            final String setter = getSetter(layoutName);

            if (cppType.startsWith(ARRAY_CPP_TYPE) || cppType.startsWith(STRING_CPP_TYPE)) {
                code.append(INDENT)
                        .append("inline void ").append(setter)
                        .append("(const ").append(cppType).append(" &value) {").append(NL);
                code.append(INDENT2).append("this->").append(fieldName).append(" = value;").append(NL);
                code.append(INDENT2).append(hasFieldName).append(" = true;").append(NL);
                code.append(INDENT).append("}").append(NL2);
            } else {
                code.append(INDENT)
                        .append("inline void ").append(setter)
                        .append("(").append(cppType).append(" value) {").append(NL);
                code.append(INDENT2)
                        .append("this->").append(fieldName).append(" = value;").append(NL);
                code.append(INDENT).append("}").append(NL2);
            }
        }
    }

    private void generateToString(StringBuilder code, TreeMap<String, String> fieldToType, TreeMap<String, String> fieldToNull) {
        if (parentClassName == null) {
            code.append(INDENT).append("virtual string toString() = 0;").append(NL);
            return;
        }

        code.append(INDENT).append("virtual string toString() {").append(NL);
        code.append(INDENT2).append("stringstream ss;").append(NL);
        code.append(INDENT2).append("ss << ").append(parentClassName).append("::toString() << \", \";").append(NL);

        boolean addComma = false;
        for (Map.Entry<String, String> entry : fieldToType.entrySet()) {
            final String cppType = entry.getValue();

            if (cppType.startsWith(BINARY_CPP_TYPE) ||
                cppType.startsWith(ARRAY_CPP_TYPE) ||
                cppType.startsWith(CLASS_CPP_TYPE))
                continue;

            final String layoutName = entry.getKey();
            final String fieldName = getField(layoutName);
            final String hasFieldName = getHasField(layoutName);
            final String nullValue = fieldToNull.get(layoutName);

            if (addComma)
                code.append(INDENT2).append("ss << \", \";").append(NL);
            addComma = true;

            code.append(INDENT2).append("ss << \"  ").append(fieldName).append(": \"; ").append(NL);

            if (cppType.startsWith(ARRAY_CPP_TYPE) || cppType.startsWith(STRING_CPP_TYPE)) {
                code.append(INDENT2).append("if (!").append(hasFieldName).append(") { ")
                        .append("ss << \"[NULL]\"; } else { ss << ").append(fieldName).append("; }")
                        .append(NL);
            } else {
                code.append(INDENT2);
                if (nullValue != null)
                    code.append("if (").append(fieldName).append(" == ").append(nullValue).append(") { ")
                        .append("ss << \"[NULL]\"; } else");

                if (cppType.startsWith(BYTE_CPP_TYPE) || cppType.startsWith(UNSIGNED_BYTE_CPP_TYPE))
                    code.append(" { ss << (int) ").append(fieldName);
                else
                    code.append(" { ss << ").append(fieldName);
                code.append("; }").append(NL);
            }
        }
        code.append(INDENT2).append("return ss.str();").append(NL);
        code.append(INDENT).append("};").append(NL2);
    }

    private void generateFields(StringBuilder code, TreeMap<String, String> fieldToValue) {
        for (Map.Entry<String, String> entry : fieldToValue.entrySet()) {
            final String layoutName = entry.getKey();
            final String fieldName = getField(layoutName);
            final String cppType = entry.getValue();

            code.append(INDENT).append(cppType).append(" ").append(fieldName).append(";").append(NL);
        }

        code.append(NL);
        for (Map.Entry<String, String> entry : fieldToValue.entrySet()) {
            final String layoutName = entry.getKey();
            final String cppType = entry.getValue();
            final String hasFieldName = getHasField(layoutName);

            if (cppType.startsWith(ARRAY_CPP_TYPE) || cppType.startsWith(STRING_CPP_TYPE)) {
                code.append(INDENT)
                        .append("bool ").append(hasFieldName).append(" = false;")
                        .append(NL);
            }
        }
    }

    private void generateHead(final StringBuilder sb, RecordClassDescriptor descriptor, NonStaticFieldLayout[] layouts) {
        String ifdef = getIfdef(namespace, className);
        sb.append("#ifndef ").append(ifdef).append(NL);
        sb.append("#define ").append(ifdef).append(NL2);

        sb.append("#include <string>").append(NL);
        sb.append("#include <stdint.h>").append(NL2);
        sb.append("#include \"dxapi/dxapi.h\"").append(NL);
        sb.append("#include \"dfp/DecimalNative.hpp\"").append(NL);
        if (parentClassName != null) {
            if (parentClassName.equals(ROOT_MESSAGE_TYPE))
                sb.append("#include \"src/codecs/");
            else
                sb.append("#include \"");

            sb.append(parentClassName).append(".h\"").append(NL2);
        }

        for (NonStaticFieldLayout layout : layouts) {
            RecordClassDescriptor[] rcds = null;
            if (layout.getType() instanceof ClassDataType) {
                rcds = ((ClassDataType) layout.getType()).getDescriptors();
            } else if (layout.getType() instanceof ArrayDataType) {
                DataType elementType = ((ArrayDataType) layout.getType()).getElementDataType();
                if (elementType instanceof ClassDataType) {
                    rcds = ((ClassDataType) elementType).getDescriptors();
                }
            }

            if (rcds != null)
                for (RecordClassDescriptor rcd : rcds)
                    sb.append("#include \"").append(getSimpleClassName(rcd.getName())).append(".h\"").append(NL);
        }

        sb.append("using namespace std;").append(NL);
        sb.append("using namespace DxApi;").append(NL2);
        sb.append("using namespace deltix::dfp;").append(NL2);

        if (namespace != null)
            sb.append("namespace ").append(namespace).append(" {").append(NL2);

        sb.append("class ").append(className);
        if (parentClassName != null) {
            sb.append(" : public ");
            if (parentClassNamespace != null && !parentClassNamespace.equals(namespace))
                sb.append(this.parentClassNamespace).append("::");
            sb.append(parentClassName);
        }
        sb.append(" {").append(NL);

        sb.append("private:").append(NL);
        if (descriptor != null) {
            sb.append(INDENT).append("std::string __class_type__ = \"").append(descriptor.getName()).append("\";").append(NL);
            sb.append(INDENT).append("std::string __class_guid__ = \"").append(descriptor.getGuid()).append("\";").append(NL2);
        } else {
            sb.append(INDENT).append("std::string __class_type__ = \"").append(className).append("\";").append(NL);
            sb.append(INDENT).append("std::string __class_guid__ = \"\";").append(NL2);
        }

        sb.append("public:").append(NL);
        sb.append(INDENT).append("virtual std::string & getTypeName() { return __class_type__; }").append(NL);
        sb.append(INDENT).append("virtual std::string & getGuid() { return __class_guid__; }").append(NL2);
    }

    private void generateFooter(StringBuilder code) {
        code.append("};").append(NL2);

        if (namespace != null)
            code.append("} // namespace ").append(namespace).append(NL2);

        String ifdef = getIfdef(namespace, className);
        code.append("#endif /* ").append(ifdef).append(" */").append(NL);
    }

    private static String    escapeCppName(String name) {
        name = name.replaceAll("[^A-Za-z0-9_]", "_");
        if (Character.isDigit(name.charAt(0))) {
            name = "_" + name;
        }

        return name;
    }

    private static String capitalize(String value) {
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    private static String getField(String layoutName) {
        return escapeCppName(layoutName) + "_";
    }

    private static String getHasField(String layoutName) {
        return "has_" + getField(layoutName);
    }

    private static String getTypeField(String layoutName) {
        return "type_" + getField(layoutName);
    }

    private static String getIField(String layoutName) {
        return "i_" + getField(layoutName);
    }

    private static String getSizeField(String layoutName) {
        return "size_" + getField(layoutName);
    }

    static String getSetter(String field) {
        return "set" + capitalize(escapeCppName(field));
    }

    static String getIfdef(String namespace, String className) {
        return (namespace != null ? escapeCppName(namespace.toUpperCase()) : "") + "_" + className.toUpperCase() + "_INC";
    }

    static String getNamespace(String fullClassName, String prefix) {
        if (fullClassName.lastIndexOf(".") >= 0) {
            return NAMESPACE_PREFIX + prefix + "::" + fullClassName.substring (0, fullClassName.lastIndexOf (".")).replaceAll("[\\.]", "::")/* + "::" + */;
        }

        return NAMESPACE_PREFIX + prefix;
    }

    static String getSimpleClassName (String s) {
        if (s.lastIndexOf (".") >= 0)
            s = s.substring (s.lastIndexOf (".") + 1);
        return CLASS_NAME_PREFIX + s;
    }

    static String getSimpleVarName(String fullClassName) {
        return getSimpleClassName(fullClassName).toLowerCase();
    }

    static String getSimpleEnumType(String ns, String fullClassName) {
        if (ns == null)
            return getSimpleClassName(fullClassName) + "Type";
        else
            return getNamespace(ns, ns) + "::" + getSimpleClassName(fullClassName) + "Type";
    }

    static String getCppFullClassName(String prefix, String fullClassName) {
        return getNamespace(fullClassName, prefix) + "::" + getSimpleClassName(fullClassName);
    }

    static String    getStreamCodecName(String streamKey, String namespace) {
        if (namespace == null)
            return getSimpleClassName(capitalize(streamKey)) + STREAM_CODEC;
        else
            return getNamespace(namespace, namespace) + "::" + getStreamCodecName(streamKey, null);
    }

    static String    getStreamEncoder(String streamKey, String namespace) {
        if (namespace == null)
            return getSimpleClassName(capitalize(streamKey)) + STREAM_ENCODER;
        else
            return getNamespace(namespace, namespace) + "::" + getStreamEncoder(streamKey, null);
    }

    static String    getStreamDecoder(String streamKey, String namespace) {
        if (namespace == null)
            return getSimpleClassName(capitalize(streamKey)) + STREAM_DECODER;
        else
            return getNamespace(namespace, namespace) + "::" + getStreamDecoder(streamKey, null);
    }
}
