package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.orders;

import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.AggressorSide;
import com.epam.deltix.timebase.messages.SchemaElement;

@SchemaElement(name = "deltix.orders.ExecutionInfo")
public class ExecutionInfo {

    private float price;
    private float size;
    private AggressorSide side;
    private CommissionInfo commissionInfo;

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

    @SchemaElement(title = "side")
    public AggressorSide getSide() {
        return side;
    }

    public void setSide(AggressorSide side) {
        this.side = side;
    }

    @SchemaElement(title = "commissionInfo")
    public CommissionInfo getCommissionInfo() {
        return commissionInfo;
    }

    public void setCommissionInfo(CommissionInfo commissionInfo) {
        this.commissionInfo = commissionInfo;
    }

    @Override
    public String toString() {
        return "ExecutionInfo{" +
            "price=" + price +
            ", size=" + size +
            ", side=" + side +
            '}';
    }
}
