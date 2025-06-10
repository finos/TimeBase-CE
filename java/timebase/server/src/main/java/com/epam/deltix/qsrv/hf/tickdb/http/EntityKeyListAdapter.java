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
package com.epam.deltix.qsrv.hf.tickdb.http;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import com.epam.deltix.qsrv.hf.tickdb.http.EntityKeyListAdapter.EntityKeyAdapter;

/**
 *
 */
public class EntityKeyListAdapter extends
    XmlAdapter<EntityKeyAdapter[], EntityKey[]> {
    @Override
    public EntityKeyAdapter[] marshal(final EntityKey[] v) throws Exception {
        if (v == null) {
            return null;
        }

        final EntityKeyAdapter[] res = new EntityKeyAdapter[v.length];
        int idx = 0;

        for (final EntityKey id : v) {
            res[idx++] = new EntityKeyAdapter(id);
        }

        return res;
    }

    @Override
    public EntityKey[] unmarshal(final EntityKeyAdapter[] v) throws Exception {
        if (v == null)
            return null;

        final Collection<EntityKey> res = new HashSet<EntityKey>();

        for (final EntityKeyAdapter iia : v) {
            res.add(new EntityKey(iia.symbol));
        }

        EntityKey[] entities = new EntityKey[res.size()];
        res.toArray(entities);

        return entities;
    }

    public static class EntityKeyAdapter {

        // Added to ignore unknown fields
        @XmlAnyElement(lax = true)
        private List<Object> any;

        @XmlElement(namespace = "")
        public String symbol;

        public EntityKeyAdapter() {
        }

        public EntityKeyAdapter(final EntityKey id) {
            super();
            symbol = id.getSymbol().toString();
        }
    }
}
