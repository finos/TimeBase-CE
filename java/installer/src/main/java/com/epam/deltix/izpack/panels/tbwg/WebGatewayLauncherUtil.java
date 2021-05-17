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
package com.epam.deltix.izpack.panels.tbwg;

import com.epam.deltix.izpack.LinuxOS;
import com.epam.deltix.izpack.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class WebGatewayLauncherUtil {

    public static File generateWindowsCmd(String installPath, String port, String timebaseUrl) throws IOException {
        File cmd = new File(installPath, "tbwg/StartWebGateway.cmd");
        Utils.mkDirIfNeeded(cmd.getParentFile());
        try (PrintWriter writer = new PrintWriter(new FileWriter(cmd))) {
            writer.println("@echo off");
            writer.println("title \"WebGateway on " + port + "\"");
            writer.println("set JAVA_OPTS=-Dserver.port=" + port + " -Dtimebase.url=" + timebaseUrl);
            writer.println("call \"" + installPath + "\\bin\\tbwg.cmd\"");
            writer.println("pause");
        }

        return cmd;
    }


    public static File generateLinuxCmd(String installPath, String port, String timebaseUrl) throws IOException {
        File cmd = new File(installPath, "tbwg/StartWebGateway.cmd");
        Utils.mkDirIfNeeded(cmd.getParentFile());
        try (PrintWriter writer = new PrintWriter(new FileWriter(cmd))) {
            writer.println("#!/usr/bin/env sh");
            writer.println("export JAVA_OPTS=\"-Dserver.port=" + port + " -Dtimebase.url=" + timebaseUrl + "\"");
            writer.println("" + installPath + "/bin/tbwg.sh");
            writer.println("read -p \"Press enter to exit\" nothing");
        }

        LinuxOS.commandNoError("chmod", "a+rwx", cmd.getAbsolutePath());

        return cmd;
    }
}
