package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.virtualplayer.time;

import com.epam.deltix.timebase.messages.InstrumentMessage;

/**
 * @author Alexei Osipov
 */
public interface MessageTimeConverter {
    void convertTime(InstrumentMessage message);
}
