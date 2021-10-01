/*
 * Copyright 2021 EPAM Systems, Inc
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