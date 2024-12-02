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
import java.util.stream.Collectors;

public class SchemaMergeHelper {

    public static boolean isEquals(RecordClassDescriptor[] descriptors1, RecordClassDescriptor[] descriptors2) {
        if (descriptors1.length != descriptors2.length) {
            return false;
        }
        Map<String, RecordClassDescriptor> rcdMap1 = getRcdMap(descriptors1);
        Map<String, RecordClassDescriptor> rcdMap2 = getRcdMap(descriptors2);
        if (!rcdMap1.keySet().containsAll(rcdMap2.keySet())) {
            return false;
        }
        for (Map.Entry<String, RecordClassDescriptor> entry : rcdMap1.entrySet()) {
            RecordClassDescriptor d1 = entry.getValue();
            RecordClassDescriptor d2 = rcdMap2.get(entry.getKey());
            if (!isEquals(d1, d2)) {
                return false;
            }
        }
        return true;
    }

    public static boolean containsAll(RecordClassDescriptor[] target, RecordClassDescriptor[] source) {
        Map<String, RecordClassDescriptor> rcdMap1 = getRcdMap(target);
        Map<String, RecordClassDescriptor> rcdMap2 = getRcdMap(source);
        return containsAll(rcdMap1, rcdMap2);
    }

    private static boolean containsAll(Map<String, RecordClassDescriptor> target, Map<String, RecordClassDescriptor> source) {
        for (Map.Entry<String, RecordClassDescriptor> entry : source.entrySet()) {
            RecordClassDescriptor sourceRcd = entry.getValue();
            RecordClassDescriptor targetRcd = target.get(entry.getKey());
            if (!containsAll(targetRcd, sourceRcd)) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsAll(RecordClassDescriptor target, RecordClassDescriptor source) {
        if (target == null && source == null) {
            return true;
        }
        if (target == null) {
            return false;
        }
        if (!target.getName().equals(source.getName()) || target.getFields().length < source.getFields().length) {
            return false;
        }

        for (DataField field : source.getFields()) {
            if (!containsAll(target.getField(field.getName()), field)) {
                return false;
            }
        }
        return containsAll(target.getParent(), source.getParent());
    }

    private static boolean containsAll(DataField target, DataField source) {
        if (target == null && source == null) {
            return true;
        }
        if (target == null) {
            return false;
        }
        if (target instanceof StaticDataField && source instanceof StaticDataField) {
            if (!Objects.equals(((StaticDataField) target).getStaticValue(), ((StaticDataField) source).getStaticValue())) {
                return false;
            }
        } else if (target instanceof StaticDataField || source instanceof StaticDataField) {
            return false;
        }
        return isConvertible(target.getType(), source.getType());
    }

    private static boolean isConvertible(DataType target, DataType source) {
        if (target.getCode() == source.getCode()) {
            if (target instanceof ClassDataType) {
                RecordClassDescriptor[] subTarget = ((ClassDataType) target).getDescriptors();
                RecordClassDescriptor[] subSource = ((ClassDataType) source).getDescriptors();
                return containsAll(subTarget, subSource);
            } else if (target instanceof ArrayDataType) {
                DataType elementTarget = ((ArrayDataType) target).getElementDataType();
                DataType elementSource = ((ArrayDataType) source).getElementDataType();
                return isConvertible(elementTarget, elementSource);
            } else if (target instanceof EnumDataType) {
                EnumClassDescriptor targetEcd = ((EnumDataType) target).getDescriptor();
                EnumClassDescriptor sourceEcd = ((EnumDataType) source).getDescriptor();
                HashSet<EnumValue> targetValues = new HashSet<>(Arrays.asList(targetEcd.getValues()));
                HashSet<EnumValue> sourceValues = new HashSet<>(Arrays.asList(sourceEcd.getValues()));
                return targetValues.containsAll(sourceValues);
            }
        }
        return  source.isConvertible(target) != DataType.ConversionType.NotConvertible;
    }

    public static boolean isEquals(RecordClassDescriptor rcd1, RecordClassDescriptor rcd2) {
        if (rcd1 == null && rcd2 == null) {
            return true;
        }
        if (rcd1 == null || rcd2 == null) {
            return false;
        }
        if (!rcd1.getName().equals(rcd2.getName()) || rcd1.getFields().length != rcd2.getFields().length) {
            return false;
        }

        for (DataField field : rcd1.getFields()) {
            if (!isEquals(rcd2.getField(field.getName()), field)) {
                return false;
            }
        }
        return isEquals(rcd1.getParent(), rcd2.getParent());
    }

    public static boolean isEquals(DataField field1, DataField field2) {
        if (field1 == null && field2 == null) {
            return true;
        }
        if (field1 == null || field2 == null) {
            return false;
        }
        if (!field1.isEquals(field2)) {
            return false;
        }
        if (field1 instanceof StaticDataField && field2 instanceof StaticDataField) {
            if (!Objects.equals(((StaticDataField) field1).getStaticValue(), ((StaticDataField) field2).getStaticValue())) {
                return false;
            }
        } else if (field1 instanceof StaticDataField || field2 instanceof StaticDataField) {
            return false;
        }

        return isEquals(field1.getType(), field2.getType());
    }

    public static boolean isEquals(DataType type1, DataType type2) {
        if (type1.getCode() != type2.getCode() ||
                type1.isNullable() != type2.isNullable() ||
                !Objects.equals(type1.getEncoding(), type2.getEncoding())) {
            return false;
        }
        if (type1 instanceof IntegerDataType) {
            return ((IntegerDataType) type1).getSize() == ((IntegerDataType) type2).getSize();
        }
        if (type1 instanceof FloatDataType) {
            return ((FloatDataType) type1).getScale() == ((FloatDataType) type2).getScale();
        }
        if (type1 instanceof BooleanDataType || type1 instanceof VarcharDataType || type1 instanceof CharDataType) {
            return true;
        }
        if (type1 instanceof DateTimeDataType) {
            return true;
        }
        if (type1 instanceof TimeOfDayDataType) {
            return true;
        }
        if (type1 instanceof ArrayDataType) {
            return isEquals(((ArrayDataType) type1).getElementDataType(), ((ArrayDataType) type2).getElementDataType());
        }
        if (type1 instanceof ClassDataType) {
            RecordClassDescriptor[] rcd1 = ((ClassDataType) type1).getDescriptors();
            RecordClassDescriptor[] rcd2 = ((ClassDataType) type2).getDescriptors();
            return isEquals(rcd1, rcd2);
        }
        if (type1 instanceof BinaryDataType) {
            return ((BinaryDataType) type1).getCompressionLevel() == ((BinaryDataType) type2).getCompressionLevel() &&
                    ((BinaryDataType) type1).getMaxSize() == ((BinaryDataType) type2).getMaxSize();
        }
        if (type1 instanceof EnumDataType) {
            EnumClassDescriptor ecd1 = ((EnumDataType) type1).getDescriptor();
            EnumClassDescriptor ecd2 = ((EnumDataType) type2).getDescriptor();
            return isEquals(ecd1, ecd2);
        }
        return false;
    }

    private static boolean isEquals(EnumClassDescriptor ecd1, EnumClassDescriptor ecd2) {
        if (!ecd1.getName().equals(ecd2.getName()) || ecd1.isBitmask() != ecd2.isBitmask()) {
            return false;
        }
        HashSet<EnumValue> values1 = new HashSet<>(Arrays.asList(ecd1.getValues()));
        HashSet<EnumValue> values2 = new HashSet<>(Arrays.asList(ecd2.getValues()));
        return values1.size() == values2.size() && values2.containsAll(values1);
    }

    public static Map<String, DataField> getAllFieldsMap(RecordClassDescriptor rcd) {
        Map<String, DataField> result = new HashMap<>();
        if (rcd == null) return result;
        Arrays.stream(rcd.getFields()).forEach(f -> result.put(f.getName(), f));
        return result;
    }

    public static Map<String, StaticDataField> getStaticFieldsMap(RecordClassDescriptor rcd) {
        Map<String, StaticDataField> result = new HashMap<>();
        if (rcd == null) return result;
        Arrays.stream(rcd.getFields())
                .filter(f -> f instanceof StaticDataField)
                .forEach(f -> result.put(f.getName(), (StaticDataField) f));
        return result;
    }

    public static Map<String, NonStaticDataField> getAllNonStaticFieldsMap(RecordClassDescriptor rcd) {
        Map<String, NonStaticDataField> result = new HashMap<>();
        if (rcd == null) return result;
        Arrays.stream(rcd.getFields())
                .filter(f -> f instanceof NonStaticDataField)
                .forEach(f -> result.put(f.getName(), (NonStaticDataField) f));
        return result;
    }

    public static Map<String, RecordClassDescriptor> getRcdMap(RecordClassDescriptor[] rcd) {
        return Arrays.stream(rcd).collect(Collectors.toMap(NamedDescriptor::getName, descriptor -> descriptor));
    }

}