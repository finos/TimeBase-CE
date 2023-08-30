/*
 * Copyright 2023 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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