package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.entries;

import com.epam.deltix.timebase.messages.SchemaElement;

@SchemaElement(name = "deltix.entries.L1Entry")
public class L1Entry extends PriceEntry {

    private QuoteSide side;

    @SchemaElement(title = "side")
    public QuoteSide getSide() {
        return side;
    }

    public void setSide(QuoteSide side) {
        this.side = side;
    }

    public PackageEntry copyFrom(PackageEntry source) {
        super.copyFrom(source);

        if (source instanceof L1Entry) {
            final L1Entry obj = (L1Entry) source;
            side = obj.side;
        }
        return this;
    }

    @Override
    public String toString() {
        return "L1Entry{" +
            "side=" + side +
            ", price=" + price +
            ", size=" + size +
            '}';
    }
}
