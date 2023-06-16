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
package com.epam.deltix.qsrv.util.cmd;

import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.util.Version;
import com.epam.deltix.util.cmdline.AbstractShell;
import com.epam.deltix.util.io.Home;

import java.util.regex.Matcher;

/**
 * Extends AbstractShell with built-in support for -home parameter that specifies QSHome
 */
public abstract class AbstractShellEx extends AbstractShell {

    protected AbstractShellEx(String[] args) {
        super(args);
    }

    @Override
    public String expandPath(String path) {
        return (path.replaceAll ("\\$\\{home\\}", Matcher.quoteReplacement (Home.get ())));
    }

    @Override
    protected boolean doSet(String option, String value) throws Exception {
        if (option.equalsIgnoreCase ("home")) {
            System.out.println("Setting " + QSHome.QSRV_HOME_SYS_PROP + "=" + value);
            QSHome.set(value);
            return (true);
        }
        return super.doSet(option, value);
    }

    @Override
    protected boolean doCommand(String key, String args) throws Exception {

        if (key.equalsIgnoreCase ("version")) {
            System.out.println ("Version " + Version.VERSION_STRING);
            return (true);
        }

        return super.doCommand(key, args);
    }
}