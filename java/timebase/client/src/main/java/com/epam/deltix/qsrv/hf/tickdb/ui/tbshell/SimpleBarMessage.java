package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell;

import com.epam.deltix.qsrv.hf.pub.ExchangeCodec;
import com.epam.deltix.timebase.messages.*;

/**
 * Created by Alex Karpovich on 08/10/2021.
 */
public class SimpleBarMessage extends InstrumentMessage {

    @SchemaElement(title = "Exchange Code")
    @SchemaType(encoding = "ALPHANUMERIC(10)", dataType = SchemaDataType.VARCHAR)
    public long exchangeCode = ExchangeCodec.NULL;

    @SchemaElement(title = "Close")
    public double close;

    @RelativeTo("close")
    @SchemaElement(title = "Open")
    public double open;

    @RelativeTo("close")
    @SchemaElement(title = "High")
    public double high;

    @RelativeTo("close")
    @SchemaElement(title = "Low")
    public double low;

    @SchemaElement(title = "Volume")
    public double volume;

    @Override
    public String toString() {
        return "SimpleBarMessage {" +
                ", exchangeCode=" + exchangeCode +
                ", close=" + close +
                ", open=" + open +
                ", high=" + high +
                ", low=" + low +
                ", volume=" + volume +
                '}';
    }
}
