package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.orders;

import com.epam.deltix.timebase.messages.SchemaElement;

@SchemaElement(name = "deltix.orders.ExternalId")
public class ExternalId {

    private String id;

    @SchemaElement(title = "id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
