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
package com.epam.deltix.qsrv.hf.pub.monitor;

public enum LatencyMetric {
    M1 ("M1 (T2-T1)"),
    M2 ("M2 (T3-T2)"),
    M3 ("M3 (T4-T3)"),
    M4 ("M4 (T5-T4)"),
    M5 ("M5 (T6-T5)"),
    M6 ("M6 (T7-T6)"),
    M7 ("M7 (T8-T7)"),
    M8 ("M8 (T9-T8)"),
    M9 ("M9 (T10-T9)"),
    M10 ("M10 (T11-T10)"),
    M11 ("M11 (T12-T11)"),
    M12 ("M12 (T13-T12)"),
    M13 ("M13 (T14-T13)"),
    M14 ("M14 (T15-T14)"),
    M15 ("M15 (T16-T15)");

    LatencyMetric(String title) {
        this.title = title;
    }

    private final String title;

    public String getTitle() {
        return title;
    }
}
