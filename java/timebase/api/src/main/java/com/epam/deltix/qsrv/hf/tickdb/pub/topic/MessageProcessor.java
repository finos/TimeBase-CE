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
package com.epam.deltix.qsrv.hf.tickdb.pub.topic;

import com.epam.deltix.timebase.messages.InstrumentMessage;

/**
 * Interface for code that processes single {@link InstrumentMessage}.
 *
 * @author Alexei Osipov
 */
@FunctionalInterface
public interface MessageProcessor {
    /**
     * Process single message.
     * Method implementations should not block.
     *
     * Method should not throw any exceptions unless it want to abort any further processing.
     *
     * @param message message to process
     */
    void process(InstrumentMessage message);
}
