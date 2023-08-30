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

package com.epam.deltix.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;

import java.util.concurrent.TimeUnit;

public class AllTypesStreamCreator {

    public static void main(String[] args) {
        generateWithGaps("1min-1h-1h-3", TimeUnit.HOURS.toMillis(1), TimeUnit.HOURS.toMillis(1),
                TimeUnit.MINUTES.toMillis(1), TimeUnit.HOURS.toMillis(12), "S1", "S2", "S3");
    }

    public static void generate(String key) {
        try (DXTickDB db = TickDBFactory.openFromUrl("dxtick://localhost:8011", false)) {
            TestMessagesHelper helper = new TestMessagesHelper(Generator.createRandom(10, 10));
            DXTickStream stream = helper.createStream(db, key);
            helper.loadMessages(stream, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7),
                    System.currentTimeMillis() + TimeUnit.HOURS.toMillis(6),
                    TimeUnit.SECONDS.toMillis(20), "S1", "S2");
        }
    }

    public static void generateWithGaps(String key, long dataInterval, long gapInterval, long msgInterval, long duration,
                                        String... symbols) {
        try (DXTickDB db = TickDBFactory.openFromUrl("dxtick://localhost:8011", false)) {
            TestMessagesHelper helper = new TestMessagesHelper(Generator.createRandom(10, 10));
            DXTickStream stream = helper.createStream(db, key);
            helper.loadMessages(stream, System.currentTimeMillis() - duration, System.currentTimeMillis(),
                    msgInterval, dataInterval, gapInterval, symbols);
        }
    }

    public static void generate(String key, int n) {
        try (DXTickDB db = TickDBFactory.openFromUrl("dxtick://localhost:8011", false)) {
            TestMessagesHelper helper = new TestMessagesHelper(Generator.createRandom(10, 10));
            DXTickStream stream = helper.createStream(db, key);
            helper.loadMessages(stream, n);
        }
    }

}
