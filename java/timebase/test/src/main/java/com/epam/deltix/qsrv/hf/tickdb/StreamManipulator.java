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

import java.util.*;

import com.epam.deltix.qsrv.hf.tickdb.comm.client.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;

public class StreamManipulator {

    public static void main (String[] args) {

        final Thread thread = new Thread (new Manipulator ());
        thread.start ();
    }

    private static class Manipulator implements Runnable {

        @Override
        public void run () {
            DXTickDB db = new TickDBClient ("localhost",
                                            1001);

            db.open (false);

            Stack<DXTickStream> stack = new Stack<DXTickStream> ();
            StreamOptions options = StreamOptions.fixedType (StreamScope.DURABLE,
                                                             null,
                                                             null,
                                                             0,
                                                             StreamConfigurationHelper.mkEventMessageDescriptor ());
            for (;;) {
                try {
                    if (Thread.interrupted ()) {
                        throw new InterruptedException ();
                    }

                    for (int i = 0; i < 10; i++) {
                        final DXTickStream stream = db.createStream ("Stream" + UUID.randomUUID (),
                                                                     options);
                        stack.push (stream);
                    }

                    synchronized (Manipulator.this) {
                        Manipulator.this.wait (10000L);
                    }

                    while (stack.size () > 0) {
                        final DXTickStream stream = stack.pop ();
                        stream.delete ();
                    }

                } catch (final InterruptedException e) {

                } finally {
                    while (stack.size () > 0) {
                        final DXTickStream stream = stack.pop ();
                        stream.delete ();
                    }
                }

            }
        }

    }
}