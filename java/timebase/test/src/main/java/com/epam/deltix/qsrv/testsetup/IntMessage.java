package com.epam.deltix.qsrv.testsetup;

import com.epam.deltix.timebase.messages.InstrumentMessage;

/**
 *
 */
public class IntMessage extends InstrumentMessage {
    public static final String      TYPE_NAME = IntMessage.class.getName ();

    public long         data;

    @Override
    public String               toString () {
        return (super.toString () + ",data: " + data);
    }
}
