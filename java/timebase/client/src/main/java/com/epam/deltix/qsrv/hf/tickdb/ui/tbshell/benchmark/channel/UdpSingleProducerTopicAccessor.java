package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.benchmark.channel;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicSettings;
import com.epam.deltix.util.net.NetworkInterfaceUtil;

/**
 * @author Alexei Osipov
 */
public class UdpSingleProducerTopicAccessor extends TopicAccessor {
    private final String publisherAddress;

    public UdpSingleProducerTopicAccessor() {
        String ownPublicAddressAsText = NetworkInterfaceUtil.getOwnPublicAddressAsText();
        if (ownPublicAddressAsText == null) {
            throw new RuntimeException("Failed to determine public client address");
        }
        this.publisherAddress = ownPublicAddressAsText;
    }

    public UdpSingleProducerTopicAccessor(String publisherAddress) {
        this.publisherAddress = publisherAddress;
    }

    @Override
    public void createChannel(RemoteTickDB tickDB, String channelKey, RecordClassDescriptor[] rcd) {
        TopicSettings settings = new TopicSettings();
        settings.setSinglePublisherUdpMode(publisherAddress);

        tickDB.getTopicDB().createTopic(channelKey, rcd, settings);
    }
}
