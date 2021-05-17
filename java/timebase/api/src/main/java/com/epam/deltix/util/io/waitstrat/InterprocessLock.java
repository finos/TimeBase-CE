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
package com.epam.deltix.util.io.waitstrat;

import com.epam.deltix.util.lang.Disposable;

/**
 *
 */
public class InterprocessLock implements Disposable {

//    static {
//        System.load(Home.getFile("bin").getAbsolutePath() +
//                    "/interproc" +
//                    System.getProperty("os.arch") + ".dll");
//    }

    private long                    handle;

    /**
     * Named system event.
     * Create or open system event.
     * @param name - name of event.
     */
    public InterprocessLock(String name) {
        handle = init0(name, true);
    }

    /**
     * Wait for 'set' state.
     * Than event auto-resets.
     */
    public void                     waitFor() {
        wait0(handle);
    }

    public void                     signal() {
        set0(handle);
    }

    public void                     close() {
        close0(handle);
    }

    private native long             init0(String name, boolean autoreset);
    private native boolean          wait0(long handle);
    private native boolean          set0(long handle);
    private native boolean          reset0(long handle);
    private native void             close0(long handle);
}
