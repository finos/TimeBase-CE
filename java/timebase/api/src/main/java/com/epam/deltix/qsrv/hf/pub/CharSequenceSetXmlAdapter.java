package com.epam.deltix.qsrv.hf.pub;

import com.epam.deltix.util.collections.CharSequenceSet;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Converts a {@link deltix.util.collections.CharSequenceSet} object to a String and vice versa.
 */
public class CharSequenceSetXmlAdapter extends XmlAdapter<String, CharSequenceSet> {

    public CharSequenceSet unmarshal(String s) throws Exception {
        if (s.length() == 0)
            return new CharSequenceSet();
        else {
            final String[] values = s.split(",");
            final CharSequenceSet set = new CharSequenceSet(values.length);
            for (String value : values) {
                set.add(value);
            }
            return set;
        }
    }

    public String marshal(CharSequenceSet strings) throws Exception {
        if (strings.size() == 0)
            return "";

        final StringBuilder sb = new StringBuilder();
        for (String s : strings) {
            sb.append(s).append(',');
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
}
