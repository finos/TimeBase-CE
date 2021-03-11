package com.epam.deltix.qsrv.hf.tickdb.lang.runtime;

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import java.util.HashSet;

/**
 *
 */
public final class RawMessageSet  {
    private final HashSet <RawMessage>  ms = new HashSet <RawMessage> ();

    public boolean          alreadyContains (RawMessage msg) {
        if (ms.contains (msg))
            return (true);

        ms.add ((RawMessage) msg.clone ());
        return (false);
    }
}
