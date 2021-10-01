package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.orders;

import com.epam.deltix.timebase.messages.SchemaElement;
import com.epam.deltix.util.collections.generated.LongArrayList;

@SchemaElement(name = "deltix.orders.ExecutedMarketOrderInfo")
public class ExecutedMarketOrderInfo extends ExecutedInfo {

    private int infoId;
    private LongArrayList customInfo;

    @SchemaElement
    public LongArrayList getCustomInfo() {
        return customInfo;
    }

    public void setCustomInfo(LongArrayList customInfo) {
        this.customInfo = customInfo;
    }

    @SchemaElement
    public int getInfoId() {
        return infoId;
    }

    public void setInfoId(int infoId) {
        this.infoId = infoId;
    }

    @Override
    public String toString() {
        return "ExecutedMarketOrderInfo{" +
            "infoId=" + infoId +
            ", avgPrice=" + avgPrice +
            ", totalQuantity=" + totalQuantity +
            '}';
    }
}
