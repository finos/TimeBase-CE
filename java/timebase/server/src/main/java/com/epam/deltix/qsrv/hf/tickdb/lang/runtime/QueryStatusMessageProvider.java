/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb.lang.runtime;

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.TypeLoaderImpl;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.FixedBoundEncoder;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.messages.QueryStatus;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.messages.QueryStatusMessage;
import com.epam.deltix.util.memory.MemoryDataOutput;

class QueryStatusMessageProvider {

    private final RawMessage queryStatusMessage;
    private final FixedBoundEncoder queryStatusMessageEncoder;
    private final MemoryDataOutput mdo = new MemoryDataOutput();
    private final QueryStatusMessage object = new QueryStatusMessage();

    QueryStatusMessageProvider(RecordClassDescriptor[] outputTypes) {
        for (int i = 0; i < outputTypes.length; ++i) {
            if (QueryStatusMessage.CLASS_NAME.equals(outputTypes[i].getName())) {
                queryStatusMessageEncoder =
                    CodecFactory.newCompiledCachingFactory().createFixedBoundEncoder(
                        new TypeLoaderImpl(Thread.currentThread().getContextClassLoader()),
                        outputTypes[i]
                    );
                queryStatusMessage = new RawMessage(outputTypes[i]);
                return;
            }
        }

        queryStatusMessage = null;
        queryStatusMessageEncoder = null;
    }

    RawMessage prepareQueryStatusMessage(QueryStatus status, String cause) {
        if (queryStatusMessage == null) {
            return null;
        }

        object.setStatus(status);
        object.setCause(cause);

        mdo.reset();
        queryStatusMessageEncoder.encode(object, mdo);
        queryStatusMessage.copyBytes(mdo, 0);

        return queryStatusMessage;
    }
}