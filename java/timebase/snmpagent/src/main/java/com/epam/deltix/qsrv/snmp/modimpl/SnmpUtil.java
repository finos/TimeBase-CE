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
package com.epam.deltix.qsrv.snmp.modimpl;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.epam.deltix.util.time.GMT;

/**
 *
 */
public abstract class SnmpUtil {
    private static final String DATETIME_MILLIS_FORMAT_STR = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final SimpleDateFormat DTFX = new SimpleDateFormat(DATETIME_MILLIS_FORMAT_STR);

    static {
        DTFX.setTimeZone(GMT.TZ);
    }

    static String formatDateTimeMillis(long timestamp) {
        // yyyy-mm-dd hh:mm:ss.sss GMT
        if (timestamp > 0)
            synchronized (DTFX) {
                return DTFX.format(new Date(timestamp));
            }
        else
            return "";
    }
}