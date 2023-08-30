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
package com.epam.deltix.qsrv.hf.tickdb.schema;

import com.epam.deltix.qsrv.hf.pub.md.EnumClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.EnumDataType;
import com.epam.deltix.qsrv.hf.pub.md.EnumValue;
import com.epam.deltix.qsrv.hf.tickdb.schema.xml.LongToLongMapAccessor;
import com.epam.deltix.util.collections.generated.LongToLongHashMap;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

import javax.xml.bind.annotation.XmlElement;
import java.util.HashMap;

public class EnumMapping {

    public EnumMapping() { } // for jaxb

    @XmlElement
    public final HashMap<String, LongToLongMapAccessor> map = new HashMap<>();

    public void addMapping(EnumClassDescriptor from, EnumClassDescriptor to, SchemaMapping mapping) {
        ObjectArrayList<EnumValue> fromValues = new ObjectArrayList<>(from.getValues());
        ObjectArrayList<String> fromSymbols = new ObjectArrayList<>(from.getSymbols());
        ObjectArrayList<EnumValue> toValues = new ObjectArrayList<>(to.getValues());
        ObjectArrayList<String> toSymbols = new ObjectArrayList<>(to.getSymbols());

        LongToLongHashMap longMap = new LongToLongHashMap(fromSymbols.size());
        map.put(from.getGuid(), new LongToLongMapAccessor(longMap));

        for (int i = 0; i < fromValues.size(); i++) {
            EnumValue enumValue = mapping.enumValues.get(fromValues.get(i));
            if (enumValue != null && toValues.indexOf(enumValue) != -1) {
                longMap.put(fromValues.get(i).value, enumValue.value);
            } else {
                int index = toSymbols.indexOf(fromSymbols.get(i));
                if (index != -1) {
                    longMap.put(fromValues.get(i).value, toValues.get(i).value);
                } else {
                    longMap.put(fromValues.get(i).value, EnumDataType.NULL);
                }
            }
        }
    }

    public long getMapped(EnumClassDescriptor ecd, long value, long defaultValue) {
        LongToLongMapAccessor longMapAccessor = map.get(ecd.getGuid());
        return longMapAccessor == null ? defaultValue: longMapAccessor.map.get(value, defaultValue);
    }
}