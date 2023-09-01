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
package com.epam.deltix.qsrv.hf.codec;

import com.epam.deltix.qsrv.hf.pub.md.EnumClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.EnumValue;
import com.epam.deltix.util.collections.generated.LongArrayList;
import com.epam.deltix.util.collections.generated.LongToObjectHashMap;
import com.epam.deltix.util.lang.Util;

/**
 * Date: 5/30/12
 *
 * @author BazylevD
 */
public class EnumAnalyzer {
    private static final Class<?>[] ENUM_ALLOWED_CLASSES =
            new Class<?>[]{
                    byte.class, short.class, int.class, long.class, CharSequence.class, String.class,
            };

    private Class<?> enumClass;
    private EnumClassDescriptor ecd;
    private boolean isEncoding;

    private long[] ordinalToSchemaValueMapping;
    private LongToObjectHashMap<Enum> schemaValueToEnumValueMapping;
    private LongArrayList invalidEnumValues;

    public boolean analyze(Class<?> enumClass, EnumClassDescriptor ecd, boolean isEncoding) {
        this.enumClass = enumClass;
        this.ecd = ecd;
        this.isEncoding = isEncoding;
        boolean hasNotBindingField = false;

        final Object[] enumClassConstants = enumClass.getEnumConstants();
        final EnumValue[] enumValues = ecd.getValues();

        // TODO: if not enum, skip validation ???
        if (Util.indexOf(ENUM_ALLOWED_CLASSES, enumClass) != -1) {
            return false;
        } else if (enumClass.isEnum()) {

            // whether mapping is necessary?

            // compare enum with ECD
            if (isEncoding) { // encoder
                ordinalToSchemaValueMapping = new long[enumClassConstants.length];
                this.invalidEnumValues = new LongArrayList(enumClassConstants.length);

                int         num = enumValues.length;
                final String[] values = new String [num];
                for (int ii = 0; ii < num; ii++)
                    values [ii] = enumValues [ii].symbol.toLowerCase();

                for (int i = 0; i < enumClassConstants.length; i++) {
                    final Object enumValue = enumClassConstants[i];
                    final String value = enumValue.toString().toLowerCase();
                    final int idx = Util.indexOf(values, value);
                    ordinalToSchemaValueMapping[i] = (idx != -1) ? enumValues[idx].value : (-1);
                    if (idx == -1) {
                        hasNotBindingField = (true);
                        this.invalidEnumValues.add(((Enum) enumValue).ordinal());
                    }
                }
            }
            // compare ECD with enum
            else {      // decoder
                schemaValueToEnumValueMapping = new LongToObjectHashMap<>(enumValues.length);

                final String[] values = new String [enumClassConstants.length];
                for (int ii = 0; ii < enumClassConstants.length; ii++)
                    values [ii] = enumClassConstants[ii].toString().toLowerCase();

                for (EnumValue enumValue : enumValues) {
                    @SuppressWarnings ("unchecked")
                    final int idx = Util.indexOf(values, enumValue.symbol.toLowerCase());
                    if (idx != - 1)
                        schemaValueToEnumValueMapping.put(enumValue.value, (Enum) enumClassConstants[idx]);
                    else
                        hasNotBindingField = (true);
                }
            }
        }

        return hasNotBindingField;
    }

    public long[] getEnumMap() {
        return ordinalToSchemaValueMapping;
    }

    public LongArrayList getBindingMap() {
        return invalidEnumValues;
    }

    // return enum values taking into account ECD mapping
    public LongToObjectHashMap<Enum> getEnumValues() {
        if (enumClass.isEnum()) {
            return schemaValueToEnumValueMapping;
        } else return null;
    }

}