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
package com.epam.deltix.qsrv.hf.tickdb.comm.server;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogLevel;
import com.epam.deltix.util.time.TimeKeeper;

import java.io.*;
import java.security.Principal;
import java.util.Date;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class UserLogger {

    public static final String CONNECT_PATTERN        = "CONNECT";
    public static final String CONNECTED_PATTERN      = "CONNECTED";
    public static final String DISCONNECT_PATTERN     = "DISCONNECT";

    public static final String CREATE_STREAM_PATTERN  = "CREATE STREAM (%s)";
    public static final String DELETE_STREAM_PATTERN  = "DELETE STREAM (%s)";
    public static final String RENAME_STREAM_PATTERN  = "RENAME STREAM (%s)";

    public static final String CREATE_CURSOR_PATTERN  = "CREATE CURSOR %s";
    public static final String CREATE_LOADER_PATTERN  = "CREATE LOADER (%s)";

    public static final String CLOSE_CURSOR_PATTERN   = "CLOSE CURSOR %s: [%s]";
    public static final String CLOSE_LOADER_PATTERN   = "CLOSE LOADER (%s)";

    public static final String SUBSCRIBE_PATTERN      = "SUBSCRIBE %s: %s";
    public static final String UNSUBSCRIBE_PATTERN    = "UNSUBSCRIBE %s: %s";

    private static final String PATTERN               = "[user=%s, ip=%s, app=%s] %s";

    private static Log logger = com.epam.deltix.gflog.api.LogFactory.getLog("deltix.user.logger");

//    public static boolean   isEnabled(Principal user) {
//        return user != null && logger != null;
//    }

    public static boolean   canTrace(Principal user) {
        return logger.isEnabled(LogLevel.DEBUG);
    }

    public static void      trace (Principal user, String address, String appId, String pattern, Object ... args) {
        log(LogLevel.DEBUG, user, address, appId, pattern, args);
    }

    private static String   getUserName(Principal user) {
        return user != null ? user.getName() : "<none>";
    }

    public static void      log (LogLevel level, Principal user, String address, String appId, String pattern, Object ... args) {
        if (logger.isEnabled(level))
            logger.log(level).append(String.format(PATTERN, getUserName(user), address, appId, String.format(pattern, args))).commit();
    }

    public static void      log (LogLevel level, Principal user, String address, String appId, String message) {
        if (logger.isEnabled(level))
            logger.log(level).append(String.format(PATTERN, getUserName(user), address, appId, message)).commit();
    }

    public static void      warn (Principal user, String address, String appId, String message, Throwable error) {
        log(LogLevel.WARN, user, address, appId, message, error);
    }

    public static void      severe (Principal user, String address, String appId, String message, Throwable error) {
        log(LogLevel.FATAL, user, address, appId, message, error);
    }

    public static void      log (LogLevel level, Principal user, String address, String appId, String message, Throwable error) {
        if (logger.isEnabled(level))
            logger.log(level).append(String.format(PATTERN, getUserName(user), address, appId, message)).append(error).commit();
    }

    private static class Formatter extends SimpleFormatter {
        private final Date time = new Date();

        @Override
        public synchronized String format(LogRecord record) {
            String message = formatMessage(record);
            String throwable = "";

            time.setTime(TimeKeeper.currentTime);

            Throwable thrown = record.getThrown();

            if (thrown != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                pw.println();
                thrown.printStackTrace(pw);
                pw.close();
                throwable = sw.toString();
                return String.format("%1$tm-%1$te-%1$tY %1$tH:%1$tM:%1$tS, %2$s %3$s%n", time, message, throwable);
            }

            return String.format("%1$tm-%1$te-%1$tY %1$tH:%1$tM:%1$tS, %2$s%n", time, message);
        }
    }
}