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
package com.epam.deltix.qsrv.hf.tickdb.lang.runtime;

import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;
import org.ehcache.spi.serialization.Serializer;
import org.ehcache.spi.serialization.SerializerException;

import java.nio.ByteBuffer;

/**
 *
 */
public abstract class GroupByFilterState extends FilterState implements Serializer<GroupByFilterState> {

    protected final MemoryDataOutput mdo = new MemoryDataOutput();
    protected final MemoryDataInput mdi = new MemoryDataInput();

    public GroupByFilterState(FilterIMSImpl filter) {
        super(filter);
    }

    public abstract GroupByFilterState copy(GroupByFilterState to);

    @Override
    public boolean equals(GroupByFilterState groupByFilterState, ByteBuffer byteBuffer) throws ClassNotFoundException, SerializerException {
        GroupByFilterState newGroupByState = read(byteBuffer);
        return newGroupByState.equals(groupByFilterState);
    }

    protected ByteBuffer getBuffer() {
        return ByteBuffer.wrap(mdo.toByteArray());
    }

    protected void setBuffer(ByteBuffer buffer) {
        mdi.setBytes(buffer.array());
    }
}