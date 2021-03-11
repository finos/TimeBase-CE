package com.epam.deltix.qsrv.hf.tickdb.http.stream;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.epam.deltix.util.time.Periodicity;

/**
 *
 */
@XmlRootElement(name = "getPeriodicityResponse")
public class GetPeriodicityResponse {
    @XmlElement()
    public Periodicity periodicity = Periodicity.mkIrregular();
}
