package com.epam.deltix.qsrv.hf.tickdb.http.stream;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.tickdb.http.IdentityKeyListAdapter;

/**
 *
 */
@XmlRootElement(name = "listEntitiesResponse")
public class ListEntitiesResponse {
    @XmlElement()
    @XmlJavaTypeAdapter(IdentityKeyListAdapter.class)
    public IdentityKey[] identities;

    public ListEntitiesResponse() {
    }

    public ListEntitiesResponse(IdentityKey[] identities) {
        this.identities = identities;
    }
}
