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
package com.epam.deltix.qsrv.hf.tickdb.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public final class DebugFlags {
    public static final Logger LOGGER = Logger.getLogger ("deltix.tickdb.debug");

    public static final boolean     DEBUG_MSG_WRITE = Boolean.getBoolean("deltix.tickdb.debug.write");
    public static final boolean     DEBUG_MSG_READ = Boolean.getBoolean("deltix.tickdb.debug.read");
    public static final boolean     DEBUG_MSG_LOSS = Boolean.getBoolean("deltix.tickdb.debug.loss");
    public static final boolean     DEBUG_MSG_DISCARD = Boolean.getBoolean("deltix.tickdb.debug.discard");

    public static void              discard(String msg) {
        LOGGER.log(Level.INFO, msg);
    }

    public static void              loss(String msg) {
        LOGGER.log(Level.INFO, msg);
    }

    public static void              write(String msg) {
        System.out.println(msg);
    }

    public static void              read(String msg) {
        System.out.println(msg);
    }

}
