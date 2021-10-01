package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.entries;

import com.epam.deltix.timebase.messages.SchemaElement;

@SchemaElement(name = "deltix.entries.PriceEntry")
public class PriceEntry extends PackageEntry {

    protected float price;
    protected float size;

    @SchemaElement(title = "price")
    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    @SchemaElement(title = "size")
    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    public PackageEntry copyFrom(PackageEntry source) {
        super.copyFrom(source);

        if (source instanceof PriceEntry) {
            final PriceEntry obj = (PriceEntry) source;
            price = obj.price;
            size = obj.size;
        }
        return this;
    }

    @Override
    public String toString() {
        return "PriceEntry{" +
            "price=" + price +
            ", size=" + size +
            '}';
    }
}
