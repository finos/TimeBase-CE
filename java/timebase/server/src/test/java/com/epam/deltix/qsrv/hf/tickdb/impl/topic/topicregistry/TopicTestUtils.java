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

import com.epam.deltix.qsrv.QSHome;
import io.aeron.Aeron;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * @author Alexei Osipov
 */
class TopicTestUtils {

    static void initTempQSHome() throws IOException {
        Path tempDirWithPrefix = Files.createTempDirectory("deltix-test-qshome");
        QSHome.set(tempDirWithPrefix.toString());
    }

    static Aeron createAeron() {
        Aeron.Context context = new Aeron.Context();
        context.aeronDirectoryName("/home/deltix/aeron_test");

        // Set high timeouts to simplify debugging. In fact we don't use Aeron's timeouts.
        context.driverTimeoutMs(TimeUnit.MINUTES.toNanos(5));

        return Aeron.connect(context);
    }
}