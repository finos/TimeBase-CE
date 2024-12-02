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
package com.epam.deltix.qsrv.hf.tickdb.pub.topic;

import com.epam.deltix.streaming.MessageChannel;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public interface TopicDataLossHandler {
    /**
     * WARING: This method must not execute blocking operations.
     *
     * <p>Will be executed in case of a detected data loss event on topic-level protocol.
     *
     * <p>Main cause of data loss event is an unexpected (non-graceful) shutdown of publisher.
     * Ensure, that data producer executes {@link MessageChannel#close()} on publisher.
     *
     * <p>Also data loss message can be caused by termination or restart of Aeron driver.
     *
     * @return true if data loss was handled (or at least acknowledged) and poller should continue
     */
    boolean handleDataLoss();
}