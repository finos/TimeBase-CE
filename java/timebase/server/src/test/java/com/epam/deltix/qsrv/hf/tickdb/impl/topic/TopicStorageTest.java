package com.epam.deltix.qsrv.hf.tickdb.impl.topic;

import com.epam.deltix.qsrv.dtb.fs.local.LocalFS;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.impl.TopicDTO;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexei Osipov
 */
public class TopicStorageTest {

    @Test
    public void testWriteAndRead() throws Exception {
        AbstractPath temp = new LocalFS().createPath("temp").append("topics");
        temp.makeFolderRecursive();
        TopicStorage topicStorage = new TopicStorage(temp);

        TopicDTO topic = new TopicDTO();
        topic.setTopicKey("topicKey");
        topic.setChannel("a:cnannel?some=params&etc=20");
        topic.setTypes(Arrays.asList(getDescriptorForInstrumentMessage(), getDescriptorForInstrumentMessage(), getDescriptorForInstrumentMessage()));

        ConstantIdentityKey e1 = new ConstantIdentityKey("GOOG");
        ConstantIdentityKey e2 = new ConstantIdentityKey("APPL");
        ConstantIdentityKey e3 = new ConstantIdentityKey("DLTX");
        ConstantIdentityKey e4 = new ConstantIdentityKey("ABC");
        List<ConstantIdentityKey> originalEntities = Arrays.asList(e1, e2, e3, e4);
        topic.setEntities(originalEntities);


        AbstractPath topicFilePath = topicStorage.getTopicFilePath(topic.getTopicKey());
        TopicStorage.writeTopic(topicFilePath, topic);

        TopicDTO read = TopicStorage.readTopic(topicFilePath);
        assertEquals(topic.getTopicKey(), read.getTopicKey());
        assertEquals(topic.getChannel(), read.getChannel());
        assertEquals(topic.getTypes(), read.getTypes());
        assertEquals(new HashSet<>(originalEntities), new HashSet<>(read.getEntities()));

        topicFilePath.deleteExisting();

    }

    private static RecordClassDescriptor getDescriptorForInstrumentMessage() {
        Introspector ix = Introspector.createEmptyMessageIntrospector();
        try {
            return ix.introspectRecordClass("Get RD for InstrumentMessage", InstrumentMessage.class);
        } catch (Introspector.IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }
}