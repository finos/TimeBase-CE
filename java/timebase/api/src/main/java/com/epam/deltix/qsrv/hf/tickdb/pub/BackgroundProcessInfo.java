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
package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.util.progress.ExecutionStatus;
import com.epam.deltix.util.progress.ExecutionMonitor;

import javax.xml.bind.annotation.XmlElement;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public final class BackgroundProcessInfo {
    private ExecutionMonitor monitor; // for server-side only

    @XmlElement()
    String name;

    @XmlElement()
    public ExecutionStatus status = ExecutionStatus.None;

    public List<String> affectedStreams = new ArrayList<String>();

    @XmlElement()
    public double progress = 0;

    @XmlElement()
    public long startTime;

    @XmlElement()
    public long endTime;

    public Throwable error;

    public BackgroundProcessInfo() {
    }

    public BackgroundProcessInfo(String name) {
        this.name = name; 
    }

    public ExecutionMonitor getMonitor() {
        return monitor;
    }

    public String getName() {
        return name;
    }

    public BackgroundProcessInfo(String name, ExecutionMonitor monitor, String ... affectedStreams) {
        this.name = name;
        this.affectedStreams = Arrays.asList(affectedStreams);
        this.monitor = monitor; 
    }

    public void update() {
        if (monitor != null) {
            startTime = monitor.getStartTime();
            progress = monitor.getProgress();
            status = monitor.getStatus();
            endTime = monitor.getEndTime();
            error = monitor.getError();
        }
    }

    public boolean isFinished() {
        return status == ExecutionStatus.Completed ||
               status == ExecutionStatus.Aborted ||
               status == ExecutionStatus.Failed;
    }
}
