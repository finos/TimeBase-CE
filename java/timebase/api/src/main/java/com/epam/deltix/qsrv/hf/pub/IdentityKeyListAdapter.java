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

import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.Collection;
import java.util.HashSet;

/**
 * Adaptor to JAXB binding list of InstrumentIdentities
 *
 * User: TurskiyS
 * Date: 7/20/12
 */
public  class IdentityKeyListAdapter
        extends
        XmlAdapter<IdentityKeyAdapter[], IdentityKey[]> {
    @Override
    public IdentityKeyAdapter[] marshal (final IdentityKey[] v) throws Exception {
        if (v == null) {
            return null;
        }

        final IdentityKeyAdapter[] res = new IdentityKeyAdapter[v.length];
        int idx = 0;

        for (final IdentityKey id : v) {
            res[idx++] = new IdentityKeyAdapter (id);
        }

        return res;
    }

    @Override
    public IdentityKey[] unmarshal (final IdentityKeyAdapter[] v) throws Exception {
        if (v == null) {
            return null;
        }

        final Collection<IdentityKey> res = new HashSet<IdentityKey>();

        for (final IdentityKeyAdapter iia : v) {
            res.add (new ConstantIdentityKey(iia.symbol));
        }
        IdentityKey[] entities = new IdentityKey[res.size()];
        res.toArray(entities);
        if (entities.length == 0)
            return null;

        return entities;
    }
}
