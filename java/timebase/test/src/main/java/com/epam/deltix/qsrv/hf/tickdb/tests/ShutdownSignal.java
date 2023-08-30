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
package com.epam.deltix.qsrv.hf.tickdb.tests;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by Alex Karpovich on 10/05/2021.
 */
public class ShutdownSignal {

    private final CountDownLatch latch = new CountDownLatch(1);

    public ShutdownSignal() {
        final Thread shutdownHook = new Thread(this::signal, "shutdown-signal-hook");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    public boolean isSignaled() {
        return latch.getCount() <= 0;
    }

    /**
     * Programmatically signal awaiting threads.
     */
    public void signal() {
        latch.countDown();
    }

    /**
     * Await the reception of the shutdown signal.
     */
    public void await() {
        try {
            latch.await();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean await(final long timeout, final TimeUnit unit) {
        try {
            return latch.await(timeout, unit);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}