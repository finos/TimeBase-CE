/*
 * Copyright 2024 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.parsers.Element;

import java.util.Hashtable;
import java.util.Map;

/**
 *
 */
public final class TagsElement extends Element {

    public final Hashtable<String, String> tags;

    public TagsElement(long location, Hashtable<String, String> tags) {
        super(location);
        this.tags = tags;
    }

    @Override
    public void print(StringBuilder s) {
        if (tags != null) {
            s.append(" TAGS (");
            boolean first = true;
            for (Map.Entry<String, String> tag : tags.entrySet()) {
                if (first) {
                    first = false;
                } else {
                    s.append(",");
                }

                GrammarUtil.escapeVarId(tag.getKey(), s);
                s.append(":");
                GrammarUtil.escapeVarId(tag.getValue(), s);
            }
            s.append(")");
        }
    }


}
