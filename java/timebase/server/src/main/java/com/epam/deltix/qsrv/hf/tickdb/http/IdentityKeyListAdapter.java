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
package com.epam.deltix.qsrv.hf.tickdb.http;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.Collection;
import java.util.HashSet;

import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;

/**
 *
 */
public class IdentityKeyListAdapter extends XmlAdapter<String[], IdentityKey[]> {
    @Override
    public String[] marshal(final IdentityKey[] v) throws Exception {
        if (v == null) {
            return null;
        }

        final String[] res = new String[v.length];
        int idx = 0;

        for (final IdentityKey id : v) {
            res[idx++] = id.getSymbol().toString();
        }

        return res;
    }

    @Override
    public IdentityKey[] unmarshal(final String[] v) throws Exception {
        if (v == null)
            return null;

        final Collection<IdentityKey> res = new HashSet<IdentityKey>();

        for (final String iia : v) {
            res.add(new ConstantIdentityKey(iia));
        }

        IdentityKey[] entities = new IdentityKey[res.size()];
        res.toArray(entities);

        return entities;
    }
}
