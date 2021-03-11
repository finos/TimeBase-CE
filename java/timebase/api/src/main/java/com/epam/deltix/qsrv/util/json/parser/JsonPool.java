package com.epam.deltix.qsrv.util.json.parser;

import com.epam.deltix.util.collections.generated.*;
import org.apache.commons.io.output.StringBuilderWriter;

import java.util.LinkedList;

final class JsonPool {

    private final LinkedList<ObjectToObjectHashMap<String, Object>> mapsPool = new LinkedList<>();
    private final LinkedList<LongArrayList> longListsPool = new LinkedList<>();
    private final LinkedList<ObjectArrayList<Object>> objectListsPool = new LinkedList<>();
    private final LinkedList<BooleanArrayList> booleanListsPool = new LinkedList<>();
    private final LinkedList<StringBuilderWriter> stringBuilderWritersPool = new LinkedList<>();
    private final LinkedList<DoubleArrayList> doubleListsPool = new LinkedList<>();

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

    public DoubleArrayList getDoubleList() {
        if (doubleListsPool.isEmpty()) {
            return new DoubleArrayList();
        } else {
            return doubleListsPool.pop();
        }
    }

    public void returnToPool(DoubleArrayList list) {
        list.clear();
        doubleListsPool.add(list);
    }

    public BooleanArrayList getBooleanList() {
        if (booleanListsPool.isEmpty()) {
            return new BooleanArrayList();
        } else {
            return booleanListsPool.pop();
        }
    }

    public void returnToPool(BooleanArrayList list) {
        booleanListsPool.clear();
        booleanListsPool.add(list);
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
        booleanListsPool.clear();
        stringBuilderWritersPool.clear();
        doubleListsPool.clear();
    }

}
