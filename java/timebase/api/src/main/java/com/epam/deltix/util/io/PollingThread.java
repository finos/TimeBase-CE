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
package com.epam.deltix.util.io;

import com.epam.deltix.util.io.idlestrat.BusySpinIdleStrategy;
import com.epam.deltix.util.io.idlestrat.IdleStrategy;
import com.epam.deltix.util.lang.Pollable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

/**
 *
 */
public class PollingThread extends Thread {
    private final IdleStrategy      idleStrategy;

    private final List<Pollable>    pollableList = new ArrayList<>();
    private volatile boolean        stopped = false;

    public PollingThread() {
        this(new BusySpinIdleStrategy());
    }

    public PollingThread(IdleStrategy idleStrategy) {
        super("POLLING THREAD");
        setDaemon(true);
        this.idleStrategy = idleStrategy;
    }

    public void                 add(Pollable pollable) {
        synchronized (pollableList) {
            pollableList.add(pollable);
        }

        wakeUp();
    }

    public void                 remove(Pollable pollable) {
        synchronized (pollableList) {
            pollableList.remove(pollable);
        }

        wakeUp();
    }

    @Override
    public void                 run() {
        while (!stopped) {
            int size = 0;
            synchronized (pollableList) {
                size = pollableList.size();
            }

            if (size == 0) {
                LockSupport.park();

                if (stopped)
                    break;
            }

            synchronized (pollableList) {
                for (int i = 0; i < pollableList.size(); ++i)
                    pollableList.get(i).poll();
            }

            idleStrategy.idle();
        }
    }

    private void                wakeUp() {
        LockSupport.unpark(this);
    }

    public void                 shutdown() {
        synchronized (pollableList) {
            pollableList.clear();
        }
        stopped = true;

        wakeUp();
    }
}
