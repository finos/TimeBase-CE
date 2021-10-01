package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.orders;

import com.epam.deltix.timebase.messages.SchemaElement;

@SchemaElement(name = "deltix.orders.CommissionInfo")
public class CommissionInfo {

    private float commission;
    private String currency;

    @SchemaElement(title = "commission")
    public float getCommission() {
        return commission;
    }

    public void setCommission(float commission) {
        this.commission = commission;
    }

    @SchemaElement(title = "currency")
    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
