package com.epam.deltix.test.qsrv.hf.tickdb.topic;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.test.qsrv.hf.tickdb.TDBTestBase;
import com.epam.deltix.test.qsrv.hf.tickdb.TestTopicsStandalone;
import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.PublisherPreferences;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.exception.TopicNotFoundException;
import com.epam.deltix.util.JUnitCategories.TickDBFast;
import com.epam.deltix.util.io.idlestrat.YieldingIdleStrategy;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;
import java.util.List;

/**
 * @author Alexei Osipov
 */
@Category(TickDBFast.class)
public class Test_PublisherInitialEntities extends TDBTestBase {


    public Test_PublisherInitialEntities() {
        super(true);
    }

    @Test(timeout = 30_000)
    public void testBlock () {
        RemoteTickDB db = (RemoteTickDB) getTickDb();

        String topicKey = "Test_PublisherInitialEntities";
        try {
            db.getTopicDB().deleteTopic(topicKey);
        } catch (TopicNotFoundException ignored) {
        }

        db.getTopicDB().createTopic(topicKey, new RecordClassDescriptor[]{TestTopicsStandalone.makeTradeMessageDescriptor()}, null);

        List<IdentityKey> initialEntitySet = Arrays.asList(
                new ConstantIdentityKey("GOOG"),
                new ConstantIdentityKey("AAPL")
        );
        MessageChannel<InstrumentMessage> publisher = db.getTopicDB().createPublisher(topicKey, new PublisherPreferences().setInitialEntitySet(initialEntitySet), new YieldingIdleStrategy());
        publisher.close();
    }


}
