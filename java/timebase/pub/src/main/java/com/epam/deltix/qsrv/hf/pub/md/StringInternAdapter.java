package com.epam.deltix.qsrv.hf.pub.md;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * User: BazylevD
 * Date: Feb 27, 2009
 * Time: 9:33:46 PM
 */
public class StringInternAdapter extends XmlAdapter<String, String> {

    public String unmarshal(String v) throws Exception {
        return v.intern();
    }

    public String marshal(String v) throws Exception {
        return v;
    }
}
