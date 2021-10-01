package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages;

import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.timebase.messages.SchemaElement;

public class BarMessageExtended extends BarMessage {

    private float customValue;

    @SchemaElement
    public float getCustomValue() {
        return customValue;
    }

    public void setCustomValue(float customValue) {
        this.customValue = customValue;
    }

}
