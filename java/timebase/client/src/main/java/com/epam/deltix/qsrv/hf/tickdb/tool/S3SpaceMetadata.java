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
package com.epam.deltix.qsrv.hf.tickdb.tool;

import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.timebase.messages.InstrumentKey;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

public class S3SpaceMetadata {

    public static final long DEFAULT_START_TIME = Long.MIN_VALUE;

    /**
     * Version of S3 utilities set.
     */
    public String version = "0.1";

    /**
     * First replicated message timestamp.
     */
    public long startTime = DEFAULT_START_TIME;

    /**
     * List of instrument identities stored in current space.
     */
    public ObjectArrayList<InstrumentKey> symbols = new ObjectArrayList<>();

    public static S3SpaceMetadata fromStream(DXTickStream stream, String space, long firstTimestamp) {
        S3SpaceMetadata metadata = new S3SpaceMetadata();
        for (IdentityKey IdentityKey : stream.listEntities(space)) {
            metadata.symbols.add(new InstrumentKey(IdentityKey));
        }
        metadata.version = "0.1";
        metadata.startTime = firstTimestamp;
        return metadata;
    }

}
