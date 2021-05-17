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
package com.epam.deltix.qsrv.hf.tickdb.schema.xml;

import com.epam.deltix.util.collections.generated.LongEnumeration;
import com.epam.deltix.util.collections.generated.LongToLongHashMap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.ArrayList;

public class LongToLongMapAdapter extends XmlAdapter<LongToLongMapAdapter.LongMapList, LongToLongHashMap> {

    @Override
    public LongToLongHashMap unmarshal(LongMapList v) throws Exception {
        LongToLongHashMap map = new LongToLongHashMap(v.entries.size());
        for (LongEntry entry : v.entries) {
            map.put(entry.key, entry.value);
        }
        return map;
    }

    @Override
    public LongMapList marshal(LongToLongHashMap v) {
        LongEnumeration enumeration = v.keys();
        ArrayList<LongEntry> list = new ArrayList<>();
        while (enumeration.hasMoreElements()) {
            long key = enumeration.nextLongElement();
            list.add(new LongEntry(key, v.get(key,-1)));
        }
        return new LongMapList(list);
    }

    public static class LongMapList {
        @XmlElements(@XmlElement(type=LongEntry.class))
        public ArrayList<LongEntry> entries;

        protected LongMapList() { } // for jaxb

        public LongMapList(ArrayList<LongEntry> entries) {
            this.entries = entries;
        }
    }

    public static class LongEntry {
        @XmlElement
        public long key;

        @XmlElement
        public long value;

        protected LongEntry() { } // for jaxb

        public LongEntry(long key, long value) {
            this.key = key;
            this.value = value;
        }
    }
}
