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
package com.epam.deltix.qsrv.hf.tickdb.pub.query;

import com.epam.deltix.qsrv.hf.tickdb.pub.TimeConstants;

/**
 *
 */
public interface TimeController {
    /**
     *  This method affects subsequent "add subscription" methods,
     *  such as, for instance, addEntity (). New subscriptions start at
     *  the specified time.
     *
     *  @param time     The time to use, or {@link TimeConstants#USE_CURRENT_TIME}
     *                  to use server's system time, or
     *                  {@link TimeConstants#USE_CURSOR_TIME} to use cursor's
     *                  last read time.
     */
    public void                 setTimeForNewSubscriptions (long time);

    /**
     *  Reposition the message source to a new point in time, while
     *  preserving current subscription.
     * 
     *  @param time     The new position in time.
     */
    public void                 reset (long time);
}
