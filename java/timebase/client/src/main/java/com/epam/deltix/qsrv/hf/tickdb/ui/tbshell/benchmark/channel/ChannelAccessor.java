package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.benchmark.channel;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;

/**
 * @author Alexei Osipov
 */
public interface ChannelAccessor {
    void createChannel(RemoteTickDB tickDB, String channelKey, RecordClassDescriptor[] rcd);
    void deleteChannel(RemoteTickDB tickDB, String channelKey);

    MessageChannel<InstrumentMessage> createLoader(RemoteTickDB tickDB, String channelKey);

    MessageSource<InstrumentMessage> createConsumer(RemoteTickDB tickDB, String channelKey);
}
