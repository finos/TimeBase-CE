package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.orders;

import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.AggressorSide;
import com.epam.deltix.timebase.messages.*;
import com.epam.deltix.timebase.messages.SchemaElement;
import com.epam.deltix.util.collections.generated.ObjectArrayList;


@SchemaElement(name = "deltix.orders.MarketOrderInfo")
public class MarketOrderInfo extends OrderInfo {

    private String userId;
    private float size;
    private AggressorSide side;
    private ExecutedMarketOrderInfo executedInfo;
    private ObjectArrayList<ExecutedMarketOrderInfo> executedInfoHistory;

    @SchemaElement(title = "userId")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @SchemaElement(title = "size")
    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    @SchemaElement(title = "side")
    public AggressorSide getSide() {
        return side;
    }

    public void setSide(AggressorSide side) {
        this.side = side;
    }

    @SchemaElement
    public ExecutedMarketOrderInfo getExecutedInfo() {
        return executedInfo;
    }

    public void setExecutedInfo(ExecutedMarketOrderInfo executedInfo) {
        this.executedInfo = executedInfo;
    }

    @SchemaElement
    @SchemaArrayType(
        isNullable = false,
        isElementNullable = false,
        elementTypes =  {
            ExecutedMarketOrderInfo.class
        }
    )
    public ObjectArrayList<ExecutedMarketOrderInfo> getExecutedInfoHistory() {
        return executedInfoHistory;
    }

    public void setExecutedInfoHistory(ObjectArrayList<ExecutedMarketOrderInfo> executedInfoHistory) {
        this.executedInfoHistory = executedInfoHistory;
    }

    @Override
    public String toString() {
        return "MarketOrderInfo{" +
            "userId='" + userId + '\'' +
            ", size=" + size +
            ", side=" + side +
            '}';
    }
}
