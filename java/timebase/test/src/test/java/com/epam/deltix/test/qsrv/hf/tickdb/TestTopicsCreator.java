package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicSettings;

/**
 * Let's you create topics of different types for tests.
 * @author Alexei Osipov
 */
public class TestTopicsCreator {
    // timebase_host port topic_key topic_type [publisherAddress]
    public static void main(String[] args) {
        String timeBaseHost = args[0];
        int timeBasePort = Integer.parseInt(args[1]);
        String topicKey = args[2];
        String topicType = args[3];


        RemoteTickDB client = TickDBFactory.connect(timeBaseHost, timeBasePort, false);
        client.open(false);

        TopicSettings settings;
        switch (topicType) {
            case "ipc":
                settings = new TopicSettings();
                break;
            case "multicast":
                settings = new TopicSettings().setMulticastSettings(null);
                break;
            case "singleudp":
                String publisherAddress = args[4];
                settings = new TopicSettings().setSinglePublisherUdpMode(publisherAddress);
                break;
            default:
                throw new IllegalArgumentException("Wrong topicType");
        }


        TopicDB topicDB = client.getTopicDB();
        topicDB.createTopic(topicKey, new RecordClassDescriptor[]{TestTopicsStandalone.makeTradeMessageDescriptor()}, settings);

        client.close();
    }
}
