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
package com.epam.deltix.qsrv.hf.topic.consumer;

import com.epam.deltix.util.lang.Disposable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * Executes message processing in current thread.
 *
 * Note: thread will be blocked till processing is explicitly stopped by other thread.
 *
 * Only one "process*" method can be used during processors lifecycle.
 *
 * @author Alexei Osipov
 */
public interface SubscriptionWorker extends Disposable {
    /**
     * Executes message processing (blocks on this method) till processing is stopped from another thread (via call to close()).
     * Closes processor upon completion.
     */
    void processMessagesUntilStopped();

    /**
     * Executes message processing (blocks on this method) while {@code condition} is {@code true}
     * and till processing is stopped from another thread (via call to close()).
     *
     * Closes processor upon completion.
     *
     * @param condition processing will be stopped if this condition returns false. Note: it's not guarantied that condition is checked after each message.
     */
    void processMessagesWhileTrue(BooleanSupplier condition);
}
