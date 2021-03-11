package com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.Messages;

/**
 * @author Alexei Osipov
 */
public class StubData {
    public static RecordClassDescriptor makeErrorMessageDescriptor ()
    {
        return Messages.ERROR_MESSAGE_DESCRIPTOR;
    }
}
