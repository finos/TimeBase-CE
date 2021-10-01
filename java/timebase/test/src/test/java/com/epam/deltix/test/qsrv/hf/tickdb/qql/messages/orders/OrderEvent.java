package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.orders;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.*;

@SchemaElement(name = "deltix.orders.OrderEvent")
public class OrderEvent extends InstrumentMessage {

    private Order order;

    @SchemaElement(title = "order")
    @SchemaType(
        dataType = SchemaDataType.OBJECT,
        nestedTypes = {
            LimitOrder.class, MarketOrder.class
        }
    )
    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }
}
