package com.epam.deltix.test.qsrv.hf.tickdb.topic;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.test.qsrv.hf.tickdb.TDBTestBase;
import com.epam.deltix.test.qsrv.hf.tickdb.TestTopicsStandalone;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.TickDBClient;
import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.exception.TopicNotFoundException;
import com.epam.deltix.thread.affinity.AffinityConfig;
import com.epam.deltix.thread.affinity.AffinityLayout;
import com.epam.deltix.util.JUnitCategories.TickDBFast;
import com.epam.deltix.util.io.idlestrat.YieldingIdleStrategy;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.BitSet;

/**
 * @author Alexei Osipov
 */
@Category(TickDBFast.class)
public class Test_AffinityConfig extends TDBTestBase {

    public Test_AffinityConfig() {
        super(true);
    }

    @Test //(timeout = 30_000)
    public void testBlock () {
        RemoteTickDB db = (RemoteTickDB) getTickDb();
        AffinityConfig affinityConfig = new AffinityConfig(new AffinityLayout() {
            @Override
            public BitSet getMask(Thread thread) {
                System.out.println("Thread name: " + thread.getName());
                BitSet bitSet = new BitSet();
                bitSet.set(1);
                return bitSet;
            }
        });
        ((TickDBClient) db).setAffinityConfig(affinityConfig);

        String topicKey = "Test_AffinityConfig";
        try {
            db.getTopicDB().deleteTopic(topicKey);
        } catch (TopicNotFoundException ignored) {
        }

        db.getTopicDB().createTopic(topicKey, new RecordClassDescriptor[]{TestTopicsStandalone.makeTradeMessageDescriptor()}, null);

        MessageChannel<InstrumentMessage> publisher = db.getTopicDB().createPublisher(topicKey, null, new YieldingIdleStrategy());
        publisher.close();
    }
}
