package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.orders;

import com.epam.deltix.timebase.messages.SchemaElement;

@SchemaElement(name = "deltix.orders.ExecutedLimitOrderInfoB")
public class ExecutedLimitOrderInfoB extends ExecutedInfo {

    private int infoIdB;

    @SchemaElement
    public int getInfoIdB() {
        return infoIdB;
    }

    public void setInfoIdB(int infoIdB) {
        this.infoIdB = infoIdB;
    }

    @Override
    public String toString() {
        return "ExecutedLimitOrderInfoB{" +
            "infoIdB=" + infoIdB +
            ", avgPrice=" + avgPrice +
            ", totalQuantity=" + totalQuantity +
            '}';
    }
}
