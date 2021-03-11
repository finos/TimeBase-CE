package com.epam.deltix.test.qsrv.hf.tickdb.topicdemo;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.util.DemoConf;
import com.epam.deltix.util.io.idlestrat.BusySpinIdleStrategy;

/**
 * @author Alexei Osipov
 */
public class WriteTopic extends WriteBase {
    @Override
    protected MessageChannel<InstrumentMessage> createLoader(RemoteTickDB client) {
        return client.getTopicDB().createPublisher(DemoConf.DEMO_MAIN_TOPIC, null, new BusySpinIdleStrategy());
    }
}
