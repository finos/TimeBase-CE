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
package com.epam.deltix.test.qsrv.hf.tickdb.perfomancedrop;

/**
 * Config for test DB that contains data for the problem.
 * @author Alexei Osipov
 */
class LocalTestBaseConfig {
    static final String STREAM_KEY = "ticks"; // Name of main stream.
    static final String TRANSIENT_KEY = "live_trans42"; // Name of transient stream with live messages.
    static final String HOME = "C:\\dev\\Russell3000_SSD\\timebase"; // TimBase location. Must contain stream "ticks".
}