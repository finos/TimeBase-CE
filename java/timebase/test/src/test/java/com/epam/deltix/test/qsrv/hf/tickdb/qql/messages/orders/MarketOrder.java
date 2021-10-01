package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.orders;

import com.epam.deltix.timebase.messages.SchemaElement;

@SchemaElement(name = "deltix.orders.MarketOrder")
public class MarketOrder extends Order {

    private MarketOrderInfo info;
    private Execution execution;

    @SchemaElement(title = "info")
    public MarketOrderInfo getInfo() {
        return info;
    }

    public void setInfo(MarketOrderInfo info) {
        this.info = info;
    }

    @SchemaElement(title = "execution")
    public Execution getExecution() {
        return execution;
    }

    public void setExecution(Execution execution) {
        this.execution = execution;
    }

    @Override
    public String toString() {
        return "MarketOrder{" +
            "id=" + id +
            '}';
    }
}
