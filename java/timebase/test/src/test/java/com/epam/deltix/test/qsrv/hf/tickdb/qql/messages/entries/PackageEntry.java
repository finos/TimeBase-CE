package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.entries;

import com.epam.deltix.timebase.messages.SchemaElement;

@SchemaElement(name = "deltix.entries.PackageEntry")
public class PackageEntry {

    private String exchange;

    @SchemaElement(title="exhcnage")
    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public PackageEntry copyFrom(PackageEntry source) {
        final PackageEntry obj = source;
        exchange = obj.exchange;
        return this;
    }

    @Override
    public String toString() {
        return "PackageEntry{" +
            "exchange='" + exchange + '\'' +
            '}';
    }
}
