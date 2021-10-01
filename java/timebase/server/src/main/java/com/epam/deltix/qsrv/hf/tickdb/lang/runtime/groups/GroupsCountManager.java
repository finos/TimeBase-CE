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
package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.groups;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;

public class GroupsCountManager {

    private static final Log LOGGER = LogFactory.getLog(GroupsCountManager.class);

    static final int QQL_MAX_GROUPS_COUNT = Integer.getInteger("TimeBase.qql.maxGroupsCount", 1_000_000);
    static final int MAX_GROUPS_CHECK_STEP = (int) (0.05f * (float) QQL_MAX_GROUPS_COUNT);

    private boolean groupsCountExceeds;
    private boolean freeMemoryExceeds;
    private int threshold = MAX_GROUPS_CHECK_STEP;
    private long lastFreeMem;

    private final Runtime RUNTIME = Runtime.getRuntime();

    private String warningCause;

    public void startProcess() {
        groupsCountExceeds = false;
        freeMemoryExceeds = false;
        threshold = MAX_GROUPS_CHECK_STEP;
        lastFreeMem = getFreeMem();
    }

    public boolean canCreateNew(int count) {
        if (freeMemoryExceeds || groupsCountExceeds) {
            return false;
        }

        if (count >= QQL_MAX_GROUPS_COUNT) {
            warningCause = "QQL groups count is limited by " + count + ".";
            LOGGER.warn().append(warningCause).commit();
            groupsCountExceeds = true;
            return false;
        } else if (count >= threshold) {
            long freeMemory = getFreeMem();
            long growMem = lastFreeMem - freeMemory;
            lastFreeMem = freeMemory;
            if (growMem * 1.5f > freeMemory) {
                warningCause = "QQL groups count is limited by " + count +
                    ". Cause: memory limit exceeded (free memory: " + freeMemory / (1024 * 1024) +
                    "Mb; Memory grow speed: " + growMem / (1024 * 1024) + "Mb per " + MAX_GROUPS_CHECK_STEP + " groups)";
                LOGGER.warn().append(warningCause).commit();
                freeMemoryExceeds = true;
                return false;
            } else {
                threshold += MAX_GROUPS_CHECK_STEP;
            }
        }

        return true;
    }

    public String warningCause() {
        return warningCause;
    }

    private long getFreeMem() {
        return RUNTIME.maxMemory() - (RUNTIME.totalMemory() - RUNTIME.freeMemory());
    }
}