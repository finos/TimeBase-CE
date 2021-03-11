package com.epam.deltix.qsrv.hf.tickdb.http;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.epam.deltix.timebase.messages.IdentityKey;

/**
 *
 */
@XmlRootElement(name = "select")
public class SelectRequest extends DownloadRequest {
    @XmlElementWrapper
    @XmlElement(name = "item")
    public String[]             streams;

    @XmlElementWrapper
    @XmlElement(name = "item")
    public String[]             symbols;

    @XmlElementWrapper
    @XmlElement(name = "item")
    public String[]             types;

    @XmlElement()
    public TypeTransmission     typeTransmission = TypeTransmission.GUID;

    @XmlElement()
    public boolean              reverse = false;

    @XmlElement()
    public boolean              live = false;

    @XmlElement()
    public boolean              allowLateOutOfOrder = false;

    @XmlElement()
    public boolean              realTimeNotification = false;



}
