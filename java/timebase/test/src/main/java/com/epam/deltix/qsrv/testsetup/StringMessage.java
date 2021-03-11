package com.epam.deltix.qsrv.testsetup;

import com.epam.deltix.timebase.messages.InstrumentMessage;

/**
 *
 */
public class StringMessage extends InstrumentMessage {
    public static final String      TYPE_NAME = StringMessage.class.getName ();
    
    public String           data;

    @Override
    public String               toString () {
        return (super.toString () + ",data: " + data);
    }
}
