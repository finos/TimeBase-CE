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
package com.epam.deltix.qsrv.hf.tickdb.impl.bytedisruptor;

/**
 * Controls space allocation and write execution for {@link ByteRingBuffer}.
 *
 * <p>Users of this interface will always call {@link #prepare} and then {@link #write}.
 *
 * <p>Users of this interface must supply to {@link #write} exactly same {@code length} as was returned by preceding {@link #prepare} call.
 *
 * @author Alexei Osipov
 */
public interface InteractiveEventWriter<T> {
    /**
     * Stores event in internal buffer and returns it's size in bytes.
     *
     * @param event event to serialize
     * @return length of serialized event representation in bytes
     */
    int prepare(T event);

    /**
     * Writes data to the {@link ByteDataReceiver}. May not block or wait.
     *
     * @param byteRingBuffer data destination
     * @param startSequence starting sequence to write data to
     * @param length numbers of bytes to write. Must match to value returned by {@link #prepare}.
     */
    void write(ByteDataReceiver byteRingBuffer, long startSequence, int length);
}