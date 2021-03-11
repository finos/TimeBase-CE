package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicType;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for XML-based serialization of a Topic.
 *
 * @author Alexei Osipov
 */
@XmlRootElement(name = "topic")
public class TopicDTO {
    @XmlElement(name = "key")
    private String topicKey;

    // Deprecated in favor of publisherChannel and subscriberChannel
    @XmlElement(name = "channel")
    private String channel;

    @XmlElement(name = "channelSettings")
    private Map<String, String> channelSettings = new HashMap<>();

    // Deprecated in favor of "topicType"
    @XmlElement(name = "isMulticast")
    private boolean isMulticast;

    @XmlElement(name = "topicType")
    private TopicType topicType;

    @XmlElement(name = "metadata")
    private RecordClassSet rd = new RecordClassSet();

    @XmlElementWrapper(name="symbols")
    @XmlElement(name = "symbol")
    List<String> symbols;

    @XmlElement(name = "copyToStream")
    private String copyToStream;

    public TopicDTO() {
    }

    public String getTopicKey() {
        return topicKey;
    }

    public void setTopicKey(String topicKey) {
        this.topicKey = topicKey;
    }

    public String getChannel() {
        return channel;
    }

    public boolean isMulticast() {
        return isMulticast;
    }

    public void setMulticast(boolean multicast) {
        isMulticast = multicast;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getCopyToStream() {
        return copyToStream;
    }

    public void setCopyToStream(String copyToStream) {
        this.copyToStream = copyToStream;
    }

    public Map<String, String> getChannelSettings() {
        return channelSettings;
    }

    public void setChannelSettings(Map<String, String> channelSettings) {
        this.channelSettings = channelSettings;
    }

    public TopicType getTopicType() {
        return topicType;
    }

    public void setTopicType(TopicType topicType) {
        this.topicType = topicType;
    }

    public List<RecordClassDescriptor> getTypes() {
        return Arrays.asList(rd.getTopTypes());
    }

    public void setTypes(List<RecordClassDescriptor> types) {
        this.rd.clear();
        this.rd.addContentClasses(types.toArray(new RecordClassDescriptor[0]));
    }

    public List<IdentityKey> getEntities() {
        List<IdentityKey> result = new ArrayList<>();
        for (String symbol : symbols) {
            result.add(new ConstantIdentityKey(symbol));
        }
        return result;
    }

    public void setEntities(List<ConstantIdentityKey> entities) {
        List<String> symbols = new ArrayList<>(entities.size());
        for (ConstantIdentityKey entity : entities) {
            symbols.add(entity.getSymbol());
        }
        this.symbols = symbols;
    }
}
