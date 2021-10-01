package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.orders;

import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.AggressorSide;
import com.epam.deltix.timebase.messages.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

@SchemaElement(name = "deltix.orders.LimitOrderInfo")
public class LimitOrderInfo extends OrderInfo {

    private int userId;
    private float size;
    private float price;
    private AggressorSide side;
    private ExecutedInfo executedInfo;
    private ObjectArrayList<ExecutedInfo> executedInfoHistory;

    @SchemaElement(title = "size")
    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    @SchemaElement(title = "price")
    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    @SchemaElement(title = "side")
    public AggressorSide getSide() {
        return side;
    }

    public void setSide(AggressorSide side) {
        this.side = side;
    }

    @SchemaElement(title = "userId")
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    @SchemaElement(title = "executedInfo")
    @SchemaType(
        dataType = SchemaDataType.OBJECT,
        nestedTypes = {
            ExecutedLimitOrderInfoA.class, ExecutedLimitOrderInfoB.class
        }
    )
    public ExecutedInfo getExecutedInfo() {
        return executedInfo;
    }

    public void setExecutedInfo(ExecutedInfo executedInfo) {
        this.executedInfo = executedInfo;
    }

    @SchemaElement
    @SchemaArrayType(
        isNullable = false,
        isElementNullable = false,
        elementTypes =  {
            ExecutedLimitOrderInfoA.class, ExecutedLimitOrderInfoB.class
        }
    )
    public ObjectArrayList<ExecutedInfo> getExecutedInfoHistory() {
        return executedInfoHistory;
    }

    public void setExecutedInfoHistory(ObjectArrayList<ExecutedInfo> executedInfoHistory) {
        this.executedInfoHistory = executedInfoHistory;
    }

    @Override
    public String toString() {
        return "LimitOrderInfo{" +
            "userId=" + userId +
            ", size=" + size +
            ", price=" + price +
            ", side=" + side +
            '}';
    }
}
