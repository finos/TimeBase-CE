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
package com.epam.deltix.qsrv.hf.tickdb.impl.queue;

import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.util.concurrent.IntermittentlyAvailableResource;
import com.epam.deltix.util.memory.MemoryDataInput;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author Alexei Osipov
 */
public interface QueueMessageReader extends Closeable, IntermittentlyAvailableResource {
    /**
     * Attempts to read a message. In case of success message is stored in this reader.
     *
     * Return {@code true} if message was received.
     * If there is no message then:
     * <pre>
     * {@literal
     * if !live => returns false
     * if live && async => throws UnavailableResourceException
     * if live && !async => awaits for message (eventually returning true)
     * }
     * </pre>
     * @return true if message was received, false is there is no data and this is not live reader.
     */
    boolean read() throws IOException;

    /**
     * @return number of bytes already loaded into reader buffer
     */
    // TODO: Consider removing this method from the interface.
    long available();

    DXTickStream        getStream();

    boolean             isLive();

    boolean             isTransient();

    long                getTimestamp();

    long                getNanoTime();

    MemoryDataInput     getInput();
}