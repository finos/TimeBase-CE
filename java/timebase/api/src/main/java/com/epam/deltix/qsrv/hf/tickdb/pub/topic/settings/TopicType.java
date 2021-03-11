package com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings;

/**
 * @author Alexei Osipov
 */
public enum TopicType {
    IPC, // Inter process communication only

    MULTICAST, // Transfer data using UDP multicast. Support of multicast in the network is required.

    UDP_SINGLE_PUBLISHER //


    //CUSTOM // Topics created with custom channel configuration
}
