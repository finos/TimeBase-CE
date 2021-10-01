package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.orders;

import com.epam.deltix.timebase.messages.SchemaElement;

@SchemaElement(name = "deltix.orders.ExecutedLimitOrderInfoA")
public class ExecutedLimitOrderInfoA extends ExecutedInfo {

    private int infoIdA;
    private float customInfo;

    @SchemaElement
    public float getCustomInfo() {
        return customInfo;
    }

    public void setCustomInfo(float customInfo) {
        this.customInfo = customInfo;
    }

    @SchemaElement
    public int getInfoIdA() {
        return infoIdA;
    }

    public void setInfoIdA(int infoIdA) {
        this.infoIdA = infoIdA;
    }

    @Override
    public String toString() {
        return "ExecutedLimitOrderInfoA{" +
            "infoIdA=" + infoIdA +
            ", customInfo=" + customInfo +
            ", avgPrice=" + avgPrice +
            ", totalQuantity=" + totalQuantity +
            '}';
    }
}
