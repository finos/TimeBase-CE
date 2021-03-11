package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.virtualplayer.time;

import com.epam.deltix.timebase.messages.InstrumentMessage;

/**
 * Keeps original message timestamp (no conversion).
 *
 * @author Alexei Osipov
 */
public class OriginalMessageTimeConverter implements MessageTimeConverter {
    @Override
    public void convertTime(InstrumentMessage message) {
        // do nothing, keep original time
    }
}
