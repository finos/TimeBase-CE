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
package com.epam.deltix.qsrv.hf.tickdb.comm;

import com.epam.deltix.qsrv.hf.tickdb.impl.TickStreamProperties;
import com.epam.deltix.util.concurrent.Signal;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 *
 */
public class StreamState {
    private final boolean[] properties = new boolean[TickStreamProperties.COUNT];
    private final AtomicReferenceArray<Signal> locks =
            new AtomicReferenceArray<Signal>(TickStreamProperties.COUNT);

    public StreamState(boolean state) {
        this();
        set(state);
    }

    public StreamState() {
        for (int i = 0; i < locks.length(); i++)
            locks.set(i, new Signal());
    }
    
    public void         set(boolean state) {
        synchronized (properties) {
            for (int i = 0; i < properties.length; i++)
                 properties[i] = state;
        }
    }

    public void         reset(int property) {
        synchronized (properties) {
            properties[property] = false;
        }
    }

    public boolean      set(int property) {
        synchronized (properties) {
            if (properties[property])
                return false;
            properties[property] = true;
        }

        return true;
    }

    public void         setNotify(int property) {
        set(property);
        getLock(property).set();
    }

    private Signal       getLock(int property) {
        return locks.get(property);
    }
    
    public void         notifyLocks() {
        for (int i = 0; i < locks.length(); i++)
            locks.get(i).set();
    }

    public  void        wait(int property) throws InterruptedException {
        getLock(property).await();
    }

    public boolean      get(int property) {
        synchronized (properties) {
            return properties[property];
        }
    }

    public void monitor(int property) {
        getLock(property).reset();
    }

}
