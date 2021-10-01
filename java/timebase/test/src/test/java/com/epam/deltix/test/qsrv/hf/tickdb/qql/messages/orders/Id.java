package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.orders;

import com.epam.deltix.timebase.messages.SchemaElement;

@SchemaElement(name = "deltix.orders.Id")
public class Id {

    private String source;
    private int correlationId;
    private ExternalId external;

    @SchemaElement(title = "source")
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @SchemaElement(title = "correlation id")
    public int getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(int correlationId) {
        this.correlationId = correlationId;
    }

    @SchemaElement(title = "external")
    public ExternalId getExternal() {
        return external;
    }

    public void setExternal(ExternalId external) {
        this.external = external;
    }

    @Override
    public String toString() {
        return "Id{" +
            "source='" + source + '\'' +
            ", correlationId=" + correlationId +
            '}';
    }
}
