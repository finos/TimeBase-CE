package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages;

import com.epam.deltix.timebase.messages.SchemaElement;

@SchemaElement(name = "deltix.CustomAttribute")
public class CustomAttribute extends Attribute {

    private String key;
    private String value;

    @SchemaElement(title = "key")
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
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
        return "CustomAttribute{" +
            "key='" + key + '\'' +
            ", value='" + value + '\'' +
            '}';
    }
}
