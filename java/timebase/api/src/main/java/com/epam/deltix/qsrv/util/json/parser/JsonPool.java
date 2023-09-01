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
package com.epam.deltix.qsrv.util.json.parser;

import com.epam.deltix.util.collections.generated.ByteArrayList;
import com.epam.deltix.util.collections.generated.LongArrayList;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.collections.generated.ObjectToObjectHashMap;
import org.apache.commons.io.output.StringBuilderWriter;

import java.util.LinkedList;

final class JsonPool {

    private final LinkedList<ObjectToObjectHashMap<String, Object>> mapsPool = new LinkedList<>();
    private final LinkedList<LongArrayList> longListsPool = new LinkedList<>();
    private final LinkedList<ObjectArrayList<Object>> objectListsPool = new LinkedList<>();
    private final LinkedList<StringBuilderWriter> stringBuilderWritersPool = new LinkedList<>();
    private final LinkedList<ByteArrayList> byteListsPool = new LinkedList<>();

    public ObjectToObjectHashMap<String, Object> getMap() {
        if (mapsPool.isEmpty()) {
            return new ObjectToObjectHashMap<>();
        } else {
            return mapsPool.pop();
        }
    }

    public void returnToPool(ObjectToObjectHashMap<String, Object> map) {
        map.clear();
        mapsPool.add(map);
    }

    public LongArrayList getLongList() {
        if (longListsPool.isEmpty()) {
            return new LongArrayList();
        } else {
            return longListsPool.pop();
        }
    }

    public void returnToPool(LongArrayList list) {
        list.clear();
        longListsPool.add(list);
    }

    public ByteArrayList getByteList() {
        if (byteListsPool.isEmpty()) {
            return new ByteArrayList();
        } else {
            return byteListsPool.pop();
        }
    }

    public void returnToPool(ByteArrayList list) {
        list.clear();
        byteListsPool.add(list);
    }

    public ObjectArrayList<Object> getObjectList() {
        if (objectListsPool.isEmpty()) {
            return new ObjectArrayList<>();
        } else {
            return objectListsPool.pop();
        }
    }

    public void returnToPool(ObjectArrayList<Object> list) {
        list.clear();
        objectListsPool.add(list);
    }

    public StringBuilderWriter getStringBuilderWriter() {
        if (stringBuilderWritersPool.isEmpty()) {
            return new StringBuilderWriter();
        } else {
            return stringBuilderWritersPool.pop();
        }
    }

    public void returnToPool(StringBuilderWriter writer) {
        writer.getBuilder().setLength(0);
        stringBuilderWritersPool.add(writer);
    }

    public void clear() {
        longListsPool.clear();
        objectListsPool.clear();
        mapsPool.clear();
        stringBuilderWritersPool.clear();
        byteListsPool.clear();
    }

}