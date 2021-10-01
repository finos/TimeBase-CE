package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages;

import com.epam.deltix.timebase.messages.SchemaElement;

@SchemaElement(name = "deltix.FixAttribute")
public class FixAttribute extends Attribute {

    private int key;
    private String value;

    @SchemaElement(title = "key")
    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    @SchemaElement(title = "value")
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "FixAttribute{" +
            "key=" + key +
            ", value='" + value + '\'' +
            '}';
    }
}
