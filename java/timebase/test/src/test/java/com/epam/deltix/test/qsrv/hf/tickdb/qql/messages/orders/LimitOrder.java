package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.orders;

import com.epam.deltix.timebase.messages.SchemaElement;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

@SchemaElement(name = "deltix.orders.LimitOrder")
public class LimitOrder extends Order {

    private LimitOrderInfo info;
    private ObjectArrayList<Execution> executions = new ObjectArrayList<>();
    private ObjectArrayList<CharSequence> customTags = new ObjectArrayList<>();

    @SchemaElement(title = "info")
    public LimitOrderInfo getInfo() {
        return info;
    }

    public void setInfo(LimitOrderInfo info) {
        this.info = info;
    }

    @SchemaElement(title = "executions")
    public ObjectArrayList<Execution> getExecutions() {
        return executions;
    }

    public void setExecutions(ObjectArrayList<Execution> executions) {
        this.executions = executions;
    }

    @SchemaElement
    public ObjectArrayList<CharSequence> getCustomTags() {
        return customTags;
    }

    public void setCustomTags(ObjectArrayList<CharSequence> customTags) {
        this.customTags = customTags;
    }

    @Override
    public String toString() {
        return "LimitOrder{" +
            "id=" + id +
            '}';
    }
}
