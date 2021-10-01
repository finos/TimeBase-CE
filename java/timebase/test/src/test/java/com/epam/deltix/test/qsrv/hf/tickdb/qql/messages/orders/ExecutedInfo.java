package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.orders;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.SchemaElement;

@SchemaElement(name = "deltix.orders.ExecutedInfo")
public class ExecutedInfo extends InstrumentMessage {

    protected float avgPrice;
    protected float totalQuantity;

    @SchemaElement
    public float getAvgPrice() {
        return avgPrice;
    }

    public void setAvgPrice(float avgPrice) {
        this.avgPrice = avgPrice;
    }

    @SchemaElement
    public float getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(float totalQuantity) {
        this.totalQuantity = totalQuantity;
    }
}
