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
package com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Alexei Osipov
 */
public class CreateTopicResult {
    private final AtomicBoolean topicDeletedSignal;
    private final CountDownLatch copyToThreadStopLatch;

    CreateTopicResult(AtomicBoolean topicDeletedSignal, CountDownLatch copyToThreadStopLatch) {
        this.topicDeletedSignal = topicDeletedSignal;
        this.copyToThreadStopLatch = copyToThreadStopLatch;
    }

    public AtomicBoolean getTopicDeletedSignal() {
        return topicDeletedSignal;
    }

    public CountDownLatch getCopyToThreadStopLatch() {
        return copyToThreadStopLatch;
    }
}