package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.entries;

import com.epam.deltix.timebase.messages.SchemaElement;

@SchemaElement(name = "deltix.entries.L2Entry")
public class L2Entry extends PriceEntry {

    private int level;
    private QuoteSide side;

    @SchemaElement(title = "level")
    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    @SchemaElement(title = "side")
    public QuoteSide getSide() {
        return side;
    }

    public void setSide(QuoteSide side) {
        this.side = side;
    }

    public PackageEntry copyFrom(PackageEntry source) {
        super.copyFrom(source);

        if (source instanceof L2Entry) {
            final L2Entry obj = (L2Entry) source;
            level = obj.level;
            side = obj.side;
        }
        return this;
    }

    @Override
    public String toString() {
        return "L2Entry{" +
            "level=" + level +
            ", side=" + side +
            ", price=" + price +
            ", size=" + size +
            '}';
    }
}
