package com.epam.deltix.test.qsrv.hf.tickdb.topic;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.net.NetworkInterfaceUtil;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.MulticastTopicSettings;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicSettings;
import com.epam.deltix.util.JUnitCategories;
import org.junit.Ignore;
import org.junit.experimental.categories.Category;

/**
 * @author Alexei Osipov
 */
@Category(JUnitCategories.TickDB.class)
public class Test_TopicSinglePublisherPoller extends Test_TopicPollerBase {

    @Ignore("TODO: Find Race condition")
    public void test() throws Exception {
        executeTest();
    }

    @Override
    protected void createTopic(TopicDB topicDB, String topicKey, RecordClassDescriptor[] types) {
        MulticastTopicSettings settings = new MulticastTopicSettings();
        settings.setTtl(1);
        topicDB.createTopic(topicKey, types,  new TopicSettings().setSinglePublisherUdpMode(NetworkInterfaceUtil.getOwnPublicAddressAsText()));
    }
}