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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.VarcharDataType;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.NamedObjectType;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Test_EnvironmentFrame {

    @Test
    public void testConcurrentRead() throws InterruptedException {
        StdEnvironment stdEnvironment = new StdEnvironment(null);

        AtomicBoolean asserted = new AtomicBoolean();
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < 20; ++t) {
            threads.add(new Thread(() -> {
                EnvironmentFrame env = new EnvironmentFrame(stdEnvironment);
                for (int i = 0; i < 100000; ++i) {
                    if (i % 2 == 0) {
                        Object obj = env.lookUp(NamedObjectType.TYPE, "VARCHAR", 0);
                        if (!(obj instanceof VarcharDataType)) {
                            asserted.set(true);
                            break;
                        }
                    } else {
                        Object obj = env.lookUp(NamedObjectType.TYPE, "FLOAT", 0);
                        if (!(obj instanceof FloatDataType)) {
                            asserted.set(true);
                            break;
                        }
                    }
                }
            }));
        }

        for (Thread t : threads) {
            t.start();
        }

        for (Thread t: threads) {
            t.join();
        }

        Assert.assertFalse(asserted.get());
    }

}
