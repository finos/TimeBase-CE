package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages;

import com.epam.deltix.timebase.messages.SchemaElement;

@SchemaElement(name = "deltix.AttributeId")
public class AttributeId {

    private int id;

    @SchemaElement
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

}
