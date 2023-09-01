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
package com.epam.deltix.qsrv.hf.tickdb.tests.procs;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;

import javax.annotation.Nonnull;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class TestProcess implements Runnable {

    private final static Log LOG = LogFactory.getLog(TestProcess.class);

    protected final Runnable runnable;

    private TestProcess(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void run() {
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setDaemon(true);
            return thread;
        });
        executor.execute(runnable);

        // gracefully exit, cause standard Process does not support it via methods like destroy, etc.
        new Scanner(System.in).nextLine();
        LOG.info().append("Received exit signal.").commit();
//        try {
//            executor.awaitTermination(10, TimeUnit.SECONDS);
//        } catch (InterruptedException e) {
//            LOG.error().append(e).commit();
//        }
//        System.exit(0);
    }

    public static TestProcess create(Runnable runnable) {
        return new TestProcess(runnable);
    }

}