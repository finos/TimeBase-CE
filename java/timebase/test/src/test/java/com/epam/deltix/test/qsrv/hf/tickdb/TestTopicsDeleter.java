package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;

/**
 * Simple tool that deletes specified topic.
 *
 * @author Alexei Osipov
 */
public class TestTopicsDeleter {
    // timebase_host port topic_key
    public static void main(String[] args) {
        String timeBaseHost = args[0];
        int timeBasePort = Integer.parseInt(args[1]);
        String topicKey = args[2];


        RemoteTickDB client = TickDBFactory.connect(timeBaseHost, timeBasePort, false);
        client.open(false);


        TopicDB topicDB = client.getTopicDB();
        topicDB.deleteTopic(topicKey);
        client.close();
    }
}
