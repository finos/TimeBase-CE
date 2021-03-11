package com.epam.deltix.qsrv.hf.tickdb.http;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.epam.deltix.timebase.messages.IdentityKey;

/**
 *
 */
@XmlRootElement(name = "asStruct")
public class SelectAsStructRequest extends DownloadRequest {
    static int SIZE_1MB = 0x100000; // 1MB (must feet L3 CPU cache)

    /** Stream key */
    @XmlElement()
    public String stream;

    @XmlElement()
    @XmlJavaTypeAdapter(IdentityKeyListAdapter.class)
    public IdentityKey[] instruments;

    @XmlElement()
    public int symbolLength = 10;

    @XmlElement(name = "type")
    public RecordType[] types;

    @XmlElementWrapper
    @XmlElement(name = "item")
    public String[] concreteTypes;

    @XmlElement
    public int bufferSize = SIZE_1MB;
}
