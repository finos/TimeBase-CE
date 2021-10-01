package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages;

import com.epam.deltix.timebase.messages.SchemaElement;
import com.epam.deltix.util.collections.generated.IntegerArrayList;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

@SchemaElement(name = "deltix.ExtendedAttribute")
public class ExtendedAttribute {
    private int id;
    private IntegerArrayList keys = new IntegerArrayList();
    private ObjectArrayList<CharSequence> values = new ObjectArrayList();

    @SchemaElement
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @SchemaElement
    public IntegerArrayList getKeys() {
        return keys;
    }

    public void setKeys(IntegerArrayList keys) {
        this.keys = keys;
    }

    @SchemaElement
    public ObjectArrayList<CharSequence> getValues() {
        return values;
    }

    public void setValues(ObjectArrayList<CharSequence> values) {
        this.values = values;
    }

}
