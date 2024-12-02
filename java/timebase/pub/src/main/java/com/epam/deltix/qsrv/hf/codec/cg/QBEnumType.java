/*
 * Copyright 2024 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.qsrv.hf.codec.CodecUtils;
import com.epam.deltix.qsrv.hf.codec.EnumAnalyzer;
import com.epam.deltix.qsrv.hf.pub.md.EnumClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.EnumValue;
import com.epam.deltix.qsrv.hf.pub.md.MdUtil;
import com.epam.deltix.util.collections.CharSequenceToIntegerMap;
import com.epam.deltix.util.collections.generated.LongArrayList;
import com.epam.deltix.util.collections.generated.LongEnumeration;
import com.epam.deltix.util.collections.generated.LongToObjectHashMap;
import com.epam.deltix.util.jcg.*;

import java.lang.reflect.Array;
import java.util.Arrays;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;
import static java.lang.reflect.Modifier.FINAL;

/**
 *
 */
public class QBEnumType extends QBoundType<QEnumType> {
    // storage size
    private final int size;
    // externally defined Enum[] array used for bound decoding
    private JExpr values = null;
    // externally defined IntegerToObjectHashMap used for bound decoding
    private JExpr valuesMap = null;
    // externally defined CharSequenceToIntegerMap used to encode bound CharSequence/String
    private JExpr stringMap = null;
    // externally defined String[] array to decode bound CharSequence/String
    private JExpr strings = null;
    // externally defined byte[]/short[]/int[]/long[] array used for bound encoding
    // contains schema enum values at enum class ordinal positions
    private JExpr enumValuesMap = null;
    // externally defined boolean[] array used for bound encoding
    // true if enum class ordinal position doesn't mapping to schema
    private JExpr enumInvalidBindingMap = null;
    private boolean hasNotBindingField;
    // externally defined <Integer>ArrayList used to validate encoded byte/.../int/long values
    private JExpr valueSet = null;
    // primitive integer type corresponding to size value
    private Class<?> primitiveClass = null;

    private static final long MAX_SPARSE_ARRAY_SIZE = Integer.getInteger("TimeBase.codecs.enum.maxSpaseArraySize", 1024);
    private final boolean useSparseArray;
    private final long sparseArraySize;


    public QBEnumType(QEnumType qType, Class<?> javaType, QAccessor accessor, QVariableContainerLookup lookupContainer) {
        super(qType, javaType, accessor);

        size = qType.getEncodedFixedSize();
        sparseArraySize = findMaxValue(qType.dt.descriptor) + 1;
        useSparseArray = sparseArraySize <= MAX_SPARSE_ARRAY_SIZE && !hasNegativeValues(qType.dt.descriptor);

        initHelperMembers(lookupContainer);
    }

    private void initHelperMembers(QVariableContainerLookup lookupContainer) {
        if (lookupContainer.isEncoding) {
            if (MdUtil.isStringType(javaBaseType)) {
                // create CharSequenceToIntegerMap
                final EnumClassDescriptor ecd = qType.dt.descriptor;
                final String name = "enum_map_" + trimBadChars(ecd.getName());
                JVariable var = lookupContainer.lookupVar(name);
                if (var == null) {
                    var = lookupContainer.addVar(CharSequenceToIntegerMap.class, name,
                            CTXT.newExpr(CharSequenceToIntegerMap.class,
                                    CTXT.intLiteral(ecd.getValues().length)));
                    stringMap = lookupContainer.access(var);
                    final JCompoundStatement stmt = lookupContainer.getInitStmt();
                    for (int i = 0; i < ecd.getValues().length; i++) {
                        final EnumValue enumValue = ecd.getValues()[i];
                        if (enumValue != null)
                            stmt.add(stringMap.call("put", CTXT.stringLiteral(enumValue.symbol), CTXT.intLiteral((int) enumValue.value)));
                    }
                } else
                    stringMap = lookupContainer.access(var);

            } else if (MdUtil.isIntegerType(javaBaseType)) {
                final EnumClassDescriptor ecd = qType.dt.descriptor;
                if (!ecd.isBitmask()) {
                    // create set of allowed values (int) for validation
                    primitiveClass = CodecUtils.getPrimitiveClass(CodecUtils.primitiveTypeBySize(size));
                    final String name = "enum_value_set_" + trimBadChars(ecd.getName());
                    JVariable var = lookupContainer.lookupVar(name);
                    if (var == null) {
                        String boxedType = CodecUtils.getTypeBySize(size);
                        if (boxedType.equals("Int"))
                            boxedType = "Integer";

                        final JClass arrayListClass = CTXT.newClass(0, LongArrayList.class.getPackage().getName(), boxedType + "ArrayList", (JClass) null);
                        var = lookupContainer.addVar(arrayListClass, name, CTXT.newExpr(arrayListClass, CTXT.intLiteral(ecd.getValues().length)));
                        valueSet = lookupContainer.access(var);
                        final JCompoundStatement stmt = lookupContainer.getInitStmt();
                        for (EnumValue enumValue : ecd.getValues()) {
                            final JExpr value = (size == 8) ? CTXT.longLiteral(enumValue.value) :
                                    primitiveClass == int.class ? CTXT.intLiteral((int) enumValue.value) :
                                            CTXT.intLiteral((int) enumValue.value).cast(primitiveClass);
                            stmt.add(valueSet.call("add", value));
                        }
                    } else
                        valueSet = lookupContainer.access(var);
                }
            }
            else {
                final EnumClassDescriptor ecd = qType.dt.descriptor;

                // I have to analyze it (twice) each time on encoding to keep hasInvalidOrdinals correct
                final EnumAnalyzer ea0 = new EnumAnalyzer();
                hasNotBindingField = ea0.analyze(getJavaBaseType(), ecd, lookupContainer.isEncoding);

                // create ordinal-to-schemaValue mapping enumValuesMap
                final String name = "enum_map_" + getJavaBaseType().getSimpleName();
                JVariable var = lookupContainer.lookupVar(name);
                if (var == null) {
                    final long[] map = ea0.getEnumMap();
                    final Class<?>  clazz = getEncodingClass(size);
                        if (map != null) {
                            final JArrayInitializer initializer = CTXT.arrayInitializer(clazz);
                            var = lookupContainer.addVar(clazz, name, initializer);
                            for (long i : map) {
                                initializer.add((size == 8) ? CTXT.longLiteral(i) : CTXT.intLiteral((int) i));
                            }
                        } else
                            return;
                }
                enumValuesMap = lookupContainer.access(var);

                if (hasNotBindingField) {
                    final String bindingName = "enum_invalid_binding_map_" + getJavaBaseType().getSimpleName();
                    JVariable variable = lookupContainer.lookupVar(bindingName);

                    if (variable == null) {
                        // create invalid binding mapping
                        final LongArrayList map = ea0.getBindingMap();
                        if (map != null) {
                            final JClass arrayListClass = CTXT.newClass(0, LongArrayList.class.getPackage().getName(), "IntegerArrayList", (JClass) null);
                            variable = lookupContainer.addVar(arrayListClass, bindingName, CTXT.newExpr(arrayListClass, CTXT.intLiteral(map.size())));
                            enumInvalidBindingMap = lookupContainer.access(variable);

                            final JCompoundStatement stmt = lookupContainer.getInitStmt();
                            for (long i : map) {
                                final JExpr value = CTXT.intLiteral((int) i);
                                stmt.add(enumInvalidBindingMap.call("add", value));
                            }
                        }
                    } else {
                        enumInvalidBindingMap = lookupContainer.access(variable);
                    }
                }
            }
        } else { // decoding
            final EnumClassDescriptor ecd = qType.dt.descriptor;
            final EnumValue[] enumValues = ecd.getValues();

            // bound to CharSequence/string
            if (MdUtil.isStringType(javaBaseType)) {
                // create String[] array
                final String name = "enum_strings_" + trimBadChars(ecd.getName());
                JVariable var = lookupContainer.lookupVar(name);
                if (var == null) {
                    if (useSparseArray) {
                        var = lookupContainer.addVar(CharSequence[].class, name, CTXT.newArrayExpr(CharSequence.class, (int) sparseArraySize));
                        strings = lookupContainer.access(var);

                        final JCompoundStatement stmt = lookupContainer.getInitStmt();
                        for (EnumValue enumValue : enumValues) {
                            stmt.add(
                                strings.index(CTXT.intLiteral((int) enumValue.value)).assign(CTXT.stringLiteral(enumValue.symbol))
                            );
                        }
                    } else {
                        final JClass mapStrings = CTXT.newClass(0, LongArrayList.class.getPackage().getName(), "LongToObjectHashMap<CharSequence>", (JClass) null);
                        var = lookupContainer.addVar(mapStrings, name, CTXT.newExpr(mapStrings, CTXT.intLiteral(enumValues.length)));
                        strings = lookupContainer.access(var);

                        final JCompoundStatement stmt = lookupContainer.getInitStmt();
                        for (EnumValue enumValue : enumValues) {
                            stmt.add(strings.call("put", CTXT.longLiteral(enumValue.value), CTXT.stringLiteral(enumValue.symbol)));
                        }
                    }
                }
                strings = lookupContainer.access(var);
                return;
            }

            // create index-to-Enum enumValuesMap
            final String name = "enum_values_" + getJavaBaseType().getSimpleName();
            JVariable var = lookupContainer.lookupVar(name);
            if (var == null) {
                if (javaBaseType.isEnum()) {

                    final EnumAnalyzer ea = new EnumAnalyzer();
                    hasNotBindingField = ea.analyze(javaBaseType, qType.dt.descriptor, lookupContainer.isEncoding);
                    if (hasNotBindingField)
                        throw new IllegalArgumentException("Class " + javaBaseType.getName() + " must contains all schema values for decoding!");
                    final LongToObjectHashMap<Enum> mappingEnums = ea.getEnumValues();

                    if (useSparseArray) {
                        Class<?> javaBaseTypeArray = Array.newInstance(javaBaseType, 0).getClass();
                        var = lookupContainer.addVar(javaBaseTypeArray, name, CTXT.newArrayExpr(javaBaseType, (int) sparseArraySize));
                        values = lookupContainer.access(var);

                        final JCompoundStatement stmt = lookupContainer.getInitStmt();
                        final LongEnumeration enumeration = mappingEnums.keys();
                        while (enumeration.hasMoreElements()) {
                            long key = enumeration.nextLongElement();
                            stmt.add(
                                values.index(CTXT.intLiteral((int) key)).assign(CTXT.enumLiteral(mappingEnums.get(key, null)))
                            );
                        }
                    } else {
                        final JClass mapClass = CTXT.newClass(0, LongToObjectHashMap.class.getPackage().getName(),
                            "LongToObjectHashMap<" + javaBaseType.getCanonicalName() + ">", (JClass) null);
                        var = lookupContainer.addVar(mapClass, name, CTXT.newExpr(mapClass, CTXT.intLiteral(mappingEnums.size())));
                        values = lookupContainer.access(var);

                        final JCompoundStatement stmt = lookupContainer.getInitStmt();
                        final LongEnumeration enumeration = mappingEnums.keys();
                        while (enumeration.hasMoreElements()) {
                            long key = enumeration.nextLongElement();
                            stmt.add(values.call("put", CTXT.longLiteral(key), CTXT.enumLiteral(mappingEnums.get(key, null))));
                        }
                    }
                } else {
                    final EnumAnalyzer ea = new EnumAnalyzer();
                    ea.analyze(javaBaseType, qType.dt.descriptor, false);
                }
            } else {
                values = lookupContainer.access(var);
            }

        }
    }

    // TODO: refactor to a shared utility function
    static String trimBadChars(String enumName) {
        if (enumName.indexOf('.') != -1 || enumName.indexOf('$') != -1 || enumName.indexOf(':') != -1)
            return enumName.replace(".", "_").replace("$", "_").replace(":", "_");
        else
            return enumName;
    }

    private static Class<?> getEncodingClass(int size) {
        switch (size) {
            case 1:
                return byte[].class;
            case 2:
                return short[].class;
            case 4:
                return int[].class;
            case 8:
                return long[].class;
            default:
                throw new IllegalStateException("unexpected size " + size);
        }
    }

    @Override
    public void encode(JExpr output, JCompoundStatement addTo) {
        JExpr value = accessor.read();
        JExpr expr;
        if (javaBaseType.isEnum()) {
            expr = value.call("ordinal");

            //validate binding
            if (hasNotBindingField) {
                addTo.add(
                        CTXT.ifStmt(CTXT.binExpr(readIsNull(false), "&&", enumInvalidBindingMap.call("contains", expr)),
                                CTXT.newExpr(IllegalArgumentException.class,
                                        CTXT.sum(CTXT.stringLiteral("value is absent in schema: " + accessor.getFieldName() + " == "), value)).throwStmt()
                        )
                );
            }

            if (enumValuesMap != null)
                expr = enumValuesMap.index(expr);
        } else if (MdUtil.isIntegerType(javaBaseType)) {
            // TODO: support the others type then byte
            expr =  value;
        } else if (MdUtil.isStringType(javaBaseType)) {
            assert stringMap != null;
            final JCompoundStatement stmt = CTXT.compStmt();
            final JLocalVariable var = stmt.addVar(0, int.class, "i");
            final JCompoundStatement elseStmt = CTXT.compStmt();
            stmt.add(CTXT.ifStmt(readIsNull(true),
                    var.assign(CTXT.intLiteral(-1)),
                    elseStmt
            ));

            elseStmt.add(var.assign(stringMap.call("get", value, CTXT.intLiteral(-1))));
            elseStmt.add(
                    CTXT.ifStmt(CTXT.binExpr(var, "==", CTXT.intLiteral(-1)),
                            CTXT.newExpr(IllegalArgumentException.class,
                                    CTXT.sum(CTXT.stringLiteral(accessor.getFieldDescription() + " == "), value)).throwStmt()
                    )
            );
            stmt.add(output.call(QEnumType.getFunction(size, false), var));
            addTo.add(stmt);
            return;
        } else {
            expr =  value;
            // TODO: support long case
            expr = expr.cast(int.class);

            if (qType.dt.descriptor.isBitmask()) {
                throw new UnsupportedOperationException("Not supported operation with enum bitmask.");
            } else {
                if (enumValuesMap != null)
                    expr = enumValuesMap.index(expr);
            }
        }

        if (javaBaseType.isEnum() )
            expr = CTXT.condExpr(readIsNull(true), CTXT.intLiteral(-1), expr);

        addTo.add(output.call(QEnumType.getFunction(size, false), expr));
    }

    @Override
    public void decode(JExpr input, JCompoundStatement addTo) {
        JExpr initValue = input.call(QEnumType.getFunction(size, true));
        // TODO: why I did it for?

        if (strings != null) {
            final JLocalVariable var = addTo.addVar(0, getPrimitiveClass(), "b", initValue);

            if (useSparseArray) {
                generateDecodeSparseArray(addTo, var, strings);
            } else {
                generateDecodeMap(addTo, var, strings);
            }
            return;
        } else if (MdUtil.isIntegerType(javaBaseType)) {
            addTo.add(accessor.write(initValue));
            return;
        } else if (qType.dt.descriptor.isBitmask()) {
            throw new UnsupportedOperationException("Not supported operation with enum bitmask.");
        }

        if (valuesMap != null) {
            addTo.add(accessor.write(CTXT.staticCall(CodecUtils.class, "get", valuesMap, initValue).cast(javaBaseType)));
        } else if (values != null) {
            if (javaBaseType.isEnum()) {
                if (useSparseArray) {
                    final JLocalVariable var = addTo.addVar(0, getPrimitiveClass(), "b", initValue);
                    generateDecodeSparseArray(addTo, var, values);
                } else {
                    final JLocalVariable var = addTo.addVar(FINAL, getPrimitiveClass(), "b", initValue);
                    generateDecodeMap(addTo, var, values);
                }
            } else {
                addTo.add(accessor.write(values.index(initValue)));
            }
        } else
            super.decode(input, addTo);
    }

    private void generateDecodeSparseArray(JCompoundStatement addTo, JLocalVariable value, JExpr collection) {
        /*
            final byte b = in0.readByte ();
            if (b == -1) {
                msg.side == null;
            } else if (b >= 0 && b < enum_values_MyQuoteSide.length) {
                msg.side = enum_values_MyQuoteSide[b];
                if (msg.side == null) {
                    throw new java.lang.IllegalArgumentException ("value is out of range "+b);
                }
            } else {
                throw new java.lang.IllegalArgumentException ("value is out of range "+b);
            }
         */
        JStatement then1 = accessor.write(CTXT.nullLiteral());
        JCompoundStatement then2 = CTXT.compStmt();
        then2.add(accessor.write(collection.index(value.cast(int.class))));
        then2.add(CTXT.ifStmt(
            CTXT.binExpr(accessor.read(), "==", CTXT.nullLiteral()),
            CTXT.newExpr(IllegalArgumentException.class,
                CTXT.sum(CTXT.stringLiteral("value is out of range "), value)).throwStmt()
        ));

        addTo.add(
            CTXT.ifStmt(
                CTXT.binExpr(value, "==", CTXT.intLiteral(-1)), then1,
                CTXT.binExpr(
                    CTXT.binExpr(value, ">=", CTXT.intLiteral(0)),
                    "&&",
                    CTXT.binExpr(value, "<", collection.field("length"))
                ), then2,
                CTXT.newExpr(IllegalArgumentException.class,
                    CTXT.sum(CTXT.stringLiteral("value is out of range "), value)).throwStmt()
            )
        );
    }

    private void generateDecodeMap(JCompoundStatement addTo, JLocalVariable value, JExpr collection) {
        /*
            final byte b = in0.readByte ();
            if (b == -1) {
                msg.side == null;
            } else {
                msg.side = this.enum_values_MyQuoteSide.get (b, null);
                if (msg.side == null) {
                    throw new java.lang.IllegalArgumentException ("value is out of range "+b);
                }
            }
         */
        JStatement then = accessor.write(CTXT.nullLiteral());
        JCompoundStatement elseStmt = CTXT.compStmt();
        elseStmt.add(accessor.write(collection.call("get", value, CTXT.nullLiteral())));
        elseStmt.add(CTXT.ifStmt(
            CTXT.binExpr(accessor.read(), "==", CTXT.nullLiteral()),
            CTXT.newExpr(IllegalArgumentException.class,
                CTXT.sum(CTXT.stringLiteral("value is out of range "), value)).throwStmt()
        ));
        addTo.add(CTXT.ifStmt(CTXT.binExpr(value, "==", CTXT.intLiteral(-1)), then, elseStmt));
    }

    @Override
    protected boolean hasNullLiteralImpl() {
        return javaBaseType.isEnum() ||
                (javaBaseType == byte.class || (MdUtil.isIntegerType(javaBaseType))) ||
                MdUtil.isStringType(javaBaseType);
    }

    @Override
    public JExpr getNullLiteral() {
        if (javaBaseType.isEnum() || MdUtil.isStringType(javaBaseType))
            return CTXT.nullLiteral();
        else
            return CTXT.intLiteral(-1);
    }

    @Override
    protected JExpr makeConstantExpr(Object obj) {
        if (obj instanceof Number) {
            final int idx = ((Number) obj).intValue();
            if (javaBaseType.isEnum()) {
                final Enum[] values = (Enum[]) javaBaseType.getEnumConstants();
                return CTXT.staticVarRef(javaBaseType, values[idx].name());
            } else if (MdUtil.isIntegerType(javaBaseType))
                return CTXT.intLiteral(idx);
            else if (MdUtil.isStringType(javaBaseType))
                return CTXT.stringLiteral(qType.dt.descriptor.longToString(idx));
            else
                throw new IllegalArgumentException("unexpected object type " + obj.getClass().getName());
        } else
            throw new IllegalArgumentException("unexpected object type " + obj.getClass().getName());
    }

    @Override
    public boolean hasConstraint() {
        return (MdUtil.isIntegerType(javaBaseType) || MdUtil.isStringType(javaBaseType));
    }

    @Override
    public JExpr readIsConstraintViolated() {
        JExpr v = accessor.read();
        final JExpr longExpr =  v;

        if (qType.dt.descriptor.isBitmask()) {
            if (MdUtil.isStringType(javaBaseType)) {
                return CTXT.binExpr(CTXT.staticCall(CodecUtils.class, "getBitmaskValue", stringMap, v), "==", CTXT.intLiteral(-1));
            } else {
                long m = 0;
                for (EnumValue enumValue : qType.dt.descriptor.getValues()) {
                    m |=  enumValue.value;
                }

                final JExpr notValidCondition;
                 notValidCondition = CTXT.binExpr(CTXT.binExpr(CTXT.longLiteral(~m), "&", longExpr), "!=", CTXT.longLiteral(0));

                return hasNullLiteral() ?
                        CTXT.binExpr(readIsNull(false), "&&", notValidCondition) :
                        notValidCondition;
            }
        } else if (MdUtil.isStringType(javaBaseType)) {
            return CTXT.binExpr(CTXT.binExpr(v, "!=", CTXT.nullLiteral()), "&&",
                    stringMap.call("containsKey", v).not());
        } else {
            if (javaBaseType != primitiveClass)
                v = v.cast(primitiveClass);

            final JExpr containsExpr = valueSet.call("contains", v);


            if (javaBaseType == primitiveClass)
                return CTXT.binExpr(readIsNull(false), "&&", containsExpr.not());
            else {
                final long max = CodecUtils.getLimit4BaseClass(true, primitiveClass);
                final JExpr notValidCondition = CTXT.binExpr(
                        CTXT.binExpr(longExpr, ">", CTXT.longLiteral(max)), "||", containsExpr.not()
                );
                return hasNullLiteral() ?
                        CTXT.binExpr(readIsNull(false), "&&", notValidCondition) :
                        notValidCondition;
            }
        }
    }

    private Class<?> getPrimitiveClass() {
        switch (size) {
            case 1:
                return byte.class;
            case 2:
                // use short in case of byte to compare with (-1)
                return short.class;
            case 4:
                return int.class;
            case 8:
                return long.class;
            default:
                throw new IllegalStateException("unexpected size " + size);
        }
    }

    public long findMaxValue(EnumClassDescriptor descriptor) {
        return Arrays.stream(descriptor.getValues())
            .mapToLong(v -> v.value).max().orElse(0);
    }

    public boolean hasNegativeValues(EnumClassDescriptor descriptor) {
        for (EnumValue v : descriptor.getValues()) {
            if (v.value < 0) {
                return true;
            }
        }

        return false;
    }

}