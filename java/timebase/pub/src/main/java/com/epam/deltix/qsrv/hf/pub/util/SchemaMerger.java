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
package com.epam.deltix.qsrv.hf.pub.util;

import com.epam.deltix.qsrv.hf.pub.md.*;

import java.util.*;

import static com.epam.deltix.qsrv.hf.pub.md.DataType.*;
import static com.epam.deltix.qsrv.hf.pub.util.SchemaMergeHelper.*;

public class SchemaMerger {

    private final Map<String, RecordClassDescriptor> result = new HashMap<>();
    private final Map<String, RecordClassDescriptor> rcds1 = new HashMap<>();
    private final Map<String, RecordClassDescriptor> rcds2 = new HashMap<>();
    private Set<String> commonTypes;

    public RecordClassDescriptor[] merge(RecordClassDescriptor[] concreteRcds1, RecordClassDescriptor[] concreteRcds2) {
        result.clear();
        rcds1.clear();
        rcds2.clear();
        final Set<String> allContentClasses = new HashSet<>();

        // find commons descriptors
        final Set<String> types1 = new HashSet<>();
        for (RecordClassDescriptor rcd : concreteRcds1) {
            RecordClassDescriptor r = rcd;
            allContentClasses.add(rcd.getName());
            do {
                types1.add(r.getName());
                rcds1.put(r.getName(), r);
            } while ((r = r.getParent()) != null);
        }
        final Set<String> types2 = new HashSet<>();
        for (RecordClassDescriptor rcd : concreteRcds2) {
            RecordClassDescriptor r = rcd;
            allContentClasses.add(rcd.getName());
            do {
                types2.add(r.getName());
                rcds2.put(r.getName(), r);
            } while ((r = r.getParent()) != null);
        }
        types1.retainAll(types2);
        commonTypes = types1;

        for (RecordClassDescriptor rcd : concreteRcds1) {
            merge(rcd);
        }

        for (RecordClassDescriptor rcd : concreteRcds2) {
            merge(rcd);
        }

        final List<RecordClassDescriptor> concereteRCD = new ArrayList<>();
        for (RecordClassDescriptor rcd : this.result.values()) {
            final String rcdName = rcd.getName();
            if (allContentClasses.contains(rcdName))
                concereteRCD.add(rcd);
        }
        return concereteRCD.toArray(new RecordClassDescriptor[0]);
    }

    private void merge(RecordClassDescriptor rcd) {
        final String rcdName = rcd.getName();
        if (result.containsKey(rcdName))
            return;

        final RecordClassDescriptor mergedRCD;
        if (commonTypes.contains(rcdName)) {
            mergedRCD = merge(rcds1.get(rcdName), rcds2.get(rcdName));
        } else {
            mergedRCD = mergeParents(rcd);
        }
        result.put(rcdName, mergedRCD);
    }

    private RecordClassDescriptor merge(RecordClassDescriptor rcd1, RecordClassDescriptor rcd2) {

        if (result.containsKey(rcd1.getName()))
            return result.get(rcd1.getName());

        if (isEquals(rcd1, rcd2))
            return mergeParents(rcd1);

        try {
            checkCompatibility(rcd1, rcd2);
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Non-comparable types: %s. Reason: %s", rcd1.getName(), e.getMessage()));
        }

        final List<DataField> fields = mergeFields(rcd1, rcd2);

        final RecordClassDescriptor parent1 = rcd1.getParent();
        final RecordClassDescriptor parent2 = rcd2.getParent();
        final RecordClassDescriptor mergedParent;
        if (parent1 == null) {
            mergedParent = parent2;
        } else if (parent2 == null) {
            mergedParent = parent1;
        } else {
            mergedParent = merge(parent1, parent2);
        }

        final RecordClassDescriptor merged = newRCD(
                rcd1,
                mergedParent,
                fields.toArray(new DataField[0]));

        result.put(merged.getName(), merged);
        return merged;
    }


    private RecordClassDescriptor mergeParents(RecordClassDescriptor rcd) {
        if (result.containsKey(rcd.getName()))
            return result.get(rcd.getName());


        final RecordClassDescriptor p = rcd.getParent();
        if (p != null) {
            final RecordClassDescriptor mergedParent;
            if (commonTypes.contains(p.getName())) {
                mergedParent = merge(rcds1.get(p.getName()), rcds2.get(p.getName()));
            } else {
                mergedParent = mergeParents(p);
            }
            if (mergedParent != p)
                rcd = newRCD(rcd, mergedParent, rcd.getFields());
        }

        result.put(rcd.getName(), rcd);
        return rcd;
    }

    private RecordClassDescriptor newRCD(RecordClassDescriptor rcd, RecordClassDescriptor parent, DataField[] fields) {
        return new RecordClassDescriptor(
                rcd.getName(),
                rcd.getTitle(),
                rcd.isAbstract(),
                parent,
                fields);
    }

    private void checkCompatibility(RecordClassDescriptor rcd1, RecordClassDescriptor rcd2) {
        if (rcd1 != null && rcd2 != null && !rcd1.getName().equals(rcd2.getName())) {
            throw new IllegalStateException(String.format("Different descriptor type: %s and %s", rcd1.getName(), rcd2.getName()));
        }
        final Map<String, NonStaticDataField> nonStaticFields1 = getAllNonStaticFieldsMap(rcd1);
        final Map<String, NonStaticDataField> nonStaticFields2 = getAllNonStaticFieldsMap(rcd2);
        final Map<String, StaticDataField> staticFields1 = getStaticFieldsMap(rcd1);
        final Map<String, StaticDataField> staticFields2 = getStaticFieldsMap(rcd2);

        if (!Collections.disjoint(nonStaticFields1.keySet(), staticFields2.keySet())) {
            throw new IllegalStateException(String.format("%s: There are intersections in static and non-static fields", rcd1.getName()));
        }
        if (!Collections.disjoint(nonStaticFields2.keySet(), staticFields1.keySet())) {
            throw new IllegalStateException(String.format("%s: There are intersections in static and non-static fields", rcd1.getName()));
        }
        for (Map.Entry<String, StaticDataField> staticField1Entry : staticFields1.entrySet()) {
            if (staticFields2.containsKey(staticField1Entry.getKey()) &&
                    isNonConvertibleFields(staticField1Entry.getValue(), staticFields2.get(staticField1Entry.getKey()))) {
                throw new IllegalStateException(String.format("Can't merge fields %s: Non convertible types or static value", staticField1Entry.getKey()));
            }
        }

        for (Map.Entry<String, NonStaticDataField> field1Entry : nonStaticFields1.entrySet()) {
            if (nonStaticFields2.containsKey(field1Entry.getKey()) &&
                    isNonConvertibleFields(field1Entry.getValue(), nonStaticFields2.get(field1Entry.getKey()))) {
                throw new IllegalStateException(String.format("Non convertible data types in field: %s", field1Entry.getKey()));
            }
        }

        // check parents
        RecordClassDescriptor parent1 = rcd1 != null ? rcd1.getParent() : null;
        RecordClassDescriptor parent2 = rcd2 != null ? rcd2.getParent() : null;
        if (parent1 != null || parent2 != null) {
            checkCompatibility(parent1, parent2);
        }
    }

    private boolean isNonConvertibleFields(DataField field1, DataField field2) {
        if (field1 instanceof StaticDataField && field2 instanceof StaticDataField) {
            try {
                mergeStaticFields((StaticDataField) field1, (StaticDataField) field2);
            } catch (Exception e) {
                return true;
            }
        }
        return !itPossibleToMerge(field1.getType(), field2.getType());
    }

    private boolean itPossibleToMerge(DataType type1, DataType type2) {
        if (isObjectTypes(type1, type2)) {
            return true; // check ObjectTypes is only possible when merging fields
        } else if (isArrayTypes(type1, type2)) {
            return itPossibleToMerge(((ArrayDataType) type1).getElementDataType(), ((ArrayDataType) type2).getElementDataType());
        } else if (isEnumTypes(type1, type2)) {
            return itPossibleToMerge(((EnumDataType) type1).getDescriptor(), ((EnumDataType) type2).getDescriptor());
        }
        return isConvertibleTypes(type1, type2) || isConvertibleTypes(type2, type1);
    }

    private boolean itPossibleToMerge(EnumClassDescriptor ecd1, EnumClassDescriptor ecd2) {
        if (!ecd1.getName().equals(ecd2.getName()) || ecd1.isBitmask() != ecd2.isBitmask()) {
            return false;
        }
        HashSet<EnumValue> values1 = new HashSet<>(Arrays.asList(ecd1.getValues()));
        HashSet<EnumValue> values2 = new HashSet<>(Arrays.asList(ecd2.getValues()));
        return values1.containsAll(values2) || values2.containsAll(values1);
    }

    private boolean isConvertibleTypes(DataType to, DataType from) {
        ConversionType convertible = from.isConvertible(to);
        return (convertible == ConversionType.Lossless || convertible == ConversionType.Lossy);
    }


    private List<DataField> mergeFields(RecordClassDescriptor rcd1, RecordClassDescriptor rcd2) {
        final Map<String, DataField> resultMap = new HashMap<>();
        final Map<String, DataField> fields1 = getAllFieldsMap(rcd1);
        final Map<String, DataField> fields2 = getAllFieldsMap(rcd2);

        for (Map.Entry<String, DataField> field1Entry : fields1.entrySet()) {
            String fieldName = field1Entry.getKey();
            if (fields2.containsKey(fieldName)) {
                resultMap.put(fieldName, mergeField(field1Entry.getValue(), fields2.get(fieldName)));
            } else {
                resultMap.put(fieldName, updateNullability(field1Entry.getValue()));
            }
        }
        for (Map.Entry<String, DataField> field2Entry : fields2.entrySet()) {
            if (!resultMap.containsKey(field2Entry.getKey())) {
                resultMap.put(field2Entry.getKey(), updateNullability(field2Entry.getValue()));
            }
        }
        return new ArrayList<>(resultMap.values());
    }

    private DataField updateNullability(DataField field) {
        DataType type = field.getType();
        if (!type.isNullable()) {
            DataType newType = type.nullableInstance(true);

            if (field instanceof StaticDataField) {
                StaticDataField dataField = new StaticDataField(field.getName(), field.getTitle(), newType, ((StaticDataField) field).getStaticValue());
                dataField.setDescription(field.getDescription());
                dataField.setAttributes(field.getAttributes());
                return dataField;
            } else if (field instanceof NonStaticDataField){
                NonStaticDataField dataField = new NonStaticDataField(field.getName(), field.getTitle(), newType);
                dataField.setDescription(field.getDescription());
                dataField.setAttributes(field.getAttributes());
                return dataField;
            }

        }
        return field;
    }

    public static DataType createNullableType(DataType type) {
        switch (type.getCode()) {
            case T_BINARY_TYPE:
                BinaryDataType binaryDataType = (BinaryDataType) type;
                return new BinaryDataType(true, binaryDataType.getMaxSize(), binaryDataType.getCompressionLevel());
            case T_CHAR_TYPE:
                return new CharDataType(true);
            case T_STRING_TYPE:
                return new VarcharDataType(type.getEncoding(), true,  ((VarcharDataType) type).isMultiLine());
            case T_DATE_TIME_TYPE:
                return new DateTimeDataType(true, type.getEncoding());
            case T_BOOLEAN_TYPE:
                return new BooleanDataType(true);
            case T_TIME_OF_DAY_TYPE:
                return new TimeOfDayDataType(true);
            case T_INTEGER_TYPE:
                IntegerDataType integerDataType = (IntegerDataType) type;
                return new IntegerDataType(integerDataType.getEncoding(), true, integerDataType.getMin(), integerDataType.getMax());
            case T_FLOAT_TYPE:
            case T_DOUBLE_TYPE:
                FloatDataType floatDataType = (FloatDataType) type;
                return new FloatDataType(floatDataType.getEncoding(), true, floatDataType.getMin(), floatDataType.getMax());
            case T_ENUM_TYPE:
                EnumDataType enumDataType = (EnumDataType) type;
                return new EnumDataType(true, enumDataType.getDescriptor());
            case T_OBJECT_TYPE:
                ClassDataType classDataType = (ClassDataType) type;
                return new ClassDataType(true, classDataType.getDescriptors());
            case T_ARRAY_TYPE:
                ArrayDataType arrayDataType = (ArrayDataType) type;
                return new ArrayDataType(true, arrayDataType.getElementDataType());
            default:
                throw new IllegalArgumentException("Illegal type: " + type);
        }
    }

    private DataField mergeField(DataField field1, DataField field2) {
        if (field1 instanceof StaticDataField != field2 instanceof StaticDataField) {
            throw new IllegalStateException("Can't merge Static and NonStatic fields: " + field1.getName());
        }
        if (isEquals(field1, field2)) {
            return field1;
        }
        DataType type1 = field1.getType();
        DataType type2 = field2.getType();
        try {
            if (isObjectTypes(type1, type2)) {
                return mergeObjectsFields(field1, field2);
            } else if (isArrayTypes(type1, type2)) {
                DataType elementType1 = ((ArrayDataType) type1).getElementDataType();
                DataType elementType2 = ((ArrayDataType) type2).getElementDataType();
                if (isObjectTypes(elementType1, elementType2)) {
                    ClassDataType classDataType = mergeClassTypes((ClassDataType) elementType1, (ClassDataType) elementType2);
                    ArrayDataType mergedType = new ArrayDataType(type1.isNullable() || type2.isNullable(), classDataType);
                    return createMergedField(field1, field2, mergedType);
                }
            } else if (isEnumTypes(type1, type2)) {
                EnumDataType mergedType = mergeEnumTypes((EnumDataType) type1, (EnumDataType) type2);
                return createMergedField(field1, field2, mergedType);
            }

            if (field1 instanceof StaticDataField && field2 instanceof StaticDataField) {
                return mergeStaticFields((StaticDataField) field1, (StaticDataField) field2);
            }
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Can't merge fields %s. Reason: %s",
                    field1.getName(), e.getMessage()));
        }

        DataField result = null;
        if (type1.isConvertible(type2) == ConversionType.Lossless) {
            result = field2;
        } else if (type2.isConvertible(type1) == ConversionType.Lossless) {
            result = field1;
        } else if (type2.isConvertible(type1) == ConversionType.Lossy) {
            result = field1;
        } else if (type1.isConvertible(type2) == ConversionType.Lossy) {
            result = field2;
        }
        if (result != null) {
            if (type1.isNullable() || type2.isNullable()) {
                return updateNullability(result);
            } else {
                return result;
            }
        }
        throw new IllegalStateException(String.format("Can't merge fields %s: Non convertible types %s and %s",
                field1.getName(), type1.getBaseName(), type2.getBaseName()));
    }

    private EnumDataType mergeEnumTypes(EnumDataType type1, EnumDataType type2) {
        if (!itPossibleToMerge(type1.getDescriptor(), type2.getDescriptor())) {
            throw new IllegalStateException("Can't merge enum types");
        }
        EnumClassDescriptor d1 = type1.getDescriptor();
        EnumClassDescriptor d2 = type2.getDescriptor();
        HashSet<EnumValue> values1 = new HashSet<>(Arrays.asList(d1.getValues()));
        HashSet<EnumValue> values2 = new HashSet<>(Arrays.asList(d2.getValues()));
        HashSet<EnumValue> values = values1.size() > values2.size() ? values1 : values2;
        boolean nullable = type1.isNullable() || type2.isNullable();
        String title = d1.getTitle() != null ? d1.getTitle() : d2.getTitle();
        return new EnumDataType(nullable, new EnumClassDescriptor(d1.getName(), title, d1.isBitmask(), values.toArray(new EnumValue[0])));
    }

    private DataField mergeStaticFields(StaticDataField field1, StaticDataField field2) {
        String staticValue1 = field1.getStaticValue();
        String staticValue2 = field2.getStaticValue();
        String resultStaticValue = mergeStaticValues(staticValue1, staticValue2);
        DataType type1 = field1.getType();
        DataType type2 = field2.getType();
        if (Objects.equals(resultStaticValue, staticValue1) && type2.isConvertible(type1) == ConversionType.Lossless) {
            return field1;
        } else if (Objects.equals(resultStaticValue, staticValue2) && type1.isConvertible(type2) == ConversionType.Lossless) {
            return field2;
        } else if (Objects.equals(resultStaticValue, staticValue1) && type2.isConvertible(type1) == ConversionType.Lossy) {
            return field1;
        } else if (Objects.equals(resultStaticValue, staticValue2) && type1.isConvertible(type2) == ConversionType.Lossy) {
            return field2;
        } else {
            throw new IllegalStateException(String.format("Can't merge fields %s: Non convertible types or static value", field1.getName()));
        }
    }

    private DataField mergeObjectsFields(DataField field1, DataField field2) {
        ClassDataType type1 = (ClassDataType) field1.getType();
        ClassDataType type2 = (ClassDataType) field2.getType();
        ClassDataType mergedType = mergeClassTypes(type1, type2);
        return createMergedField(field1, field2, mergedType);
    }

    private DataField createMergedField(DataField field1, DataField field2, DataType mergedType) {
        String title = field1.getTitle() != null ? field1.getTitle() : field2.getTitle();
        if (field1 instanceof NonStaticDataField && field2 instanceof NonStaticDataField) {
            return new NonStaticDataField(field1.getName(), title, mergedType);
        } else if (field1 instanceof StaticDataField && field2 instanceof StaticDataField) {
            String staticValue1 = ((StaticDataField) field1).getStaticValue();
            String staticValue2 = ((StaticDataField) field2).getStaticValue();
            String resultStaticValue = mergeStaticValues(staticValue1, staticValue2);
            return new StaticDataField(field1.getName(), title, mergedType, resultStaticValue);
        }
        throw new IllegalStateException("Can't merge Static and NonStatic fields: " + field1.getName());
    }

    private static String mergeStaticValues(String staticValue1, String staticValue2) {
        String resultStaticValue;
        if (staticValue1 != null) {
            if (staticValue2 != null) {
                if (staticValue2.equals(staticValue1)) {
                    resultStaticValue = staticValue1;
                } else {
                    throw new IllegalStateException("Can't merge Static values");
                }
            } else {
                resultStaticValue = staticValue1;
            }
        } else resultStaticValue = staticValue2;
        return resultStaticValue;
    }

    private ClassDataType mergeClassTypes(ClassDataType type1, ClassDataType type2) {
        SchemaMerger sm = new SchemaMerger();
        RecordClassDescriptor[] mergedDescriptors = sm.merge(type1.getDescriptors(), type2.getDescriptors());
        boolean nullable = type1.isNullable() || type2.isNullable();
        return new ClassDataType(nullable, mergedDescriptors);
    }

    private boolean isObjectTypes(DataType type1, DataType type2) {
        return type1.getCode() == T_OBJECT_TYPE && type2.getCode() == T_OBJECT_TYPE;
    }

    private boolean isArrayTypes(DataType type1, DataType type2) {
        return type1.getCode() == T_ARRAY_TYPE && type2.getCode() == T_ARRAY_TYPE;
    }

    private boolean isEnumTypes(DataType type1, DataType type2) {
        return type1.getCode() == T_ENUM_TYPE && type2.getCode() == T_ENUM_TYPE;
    }
}