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
package com.epam.deltix.izpack.panels.timebase;

import com.epam.deltix.izpack.LinuxOS;
import com.epam.deltix.izpack.Utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Logger;

public class TimebaseLauncherUtil {

    public final static Logger LOGGER = Logger.getLogger(TimebaseLauncherUtil.class.getName());

    public static File generateWindowsCmd(String installPath, String home, String port) throws IOException {
        File cmd = new File(home, "service/timebase.cmd");
        Utils.mkDirIfNeeded(cmd.getParentFile());
        try (PrintWriter writer = new PrintWriter(new FileWriter(cmd))) {
            writer.println("@echo off");
            writer.println("title \"TimeBase on " + port + "\"");
            writer.println("set DELTIX_HOME=" + installPath);
            writer.println("set PATH=%DELTIX_HOME%/bin;%PATH%");
            writer.println(":start");
            writer.println("set JAVA_OPTS=-verbose:gc -XX:+HeapDumpOnOutOfMemoryError -Xmx2g -Xms1g");
            writer.println("call \"" + installPath + "\\bin\\tdbserver.cmd\" -home \"" + home + "\" -port " + port);
            writer.println("pause");
        }

        return cmd;
    }

    public static File generateWindowsService(String installPath, String home, String port) throws IOException {
        File cfg = new File(home, "service/config.txt");
        Utils.mkDirIfNeeded(cfg.getParentFile());
        try (PrintWriter writer = new PrintWriter(new FileWriter(cfg))) {
            writer.println("jvm=" + Utils.getJVMPath());
            writer.println("class=com/epam/deltix/qsrv/comm/cat/TBService");
            writer.println();

            Arrays.stream(Objects.requireNonNull(new File(installPath, "lib").listFiles((dir, name) -> name.endsWith(".jar"))))
                .forEach(f -> writer.println("cp=" + f.getAbsolutePath()));
            writer.println();

            writer.println("-Ddeltix.home=" + installPath);
            writer.println("-Ddeltix.qsrv.home=" + home);
            writer.println("-Ddeltix.qsrv.port=" + port);
            writer.println("-Ddeltix.qsrv.service.name=tb" + port);
            writer.println("-Djava.library.path=" + installPath + "\\bin");
            writer.println("-Dgflog.debug.file=" + home + "\\logs\\gflog.log");
            writer.println("-Dgflog.console.appender.wrap=true");
            writer.println("-Xmx2483027968");
            writer.println("-Xms1644167168");
            writer.println("-verbose:gc");
            writer.println("-XX:+HeapDumpOnOutOfMemoryError");
            writer.println();
            writer.println("-Xrs");
        }

        File outputBin = Paths.get(home, "service", "TimeBaseSvcLauncher.exe").toFile();
        outputBin.delete();
        Files.copy(
            Paths.get(installPath, "bin", "launcher" + System.getProperty("os.arch") + ".exe"),
            outputBin.toPath(),
            StandardCopyOption.REPLACE_EXISTING
        );

        return outputBin;
    }

    public static File generateLinuxCmd(String installPath, String home, String port) throws IOException {
        /*
        #!/usr/bin/env sh
        export DELTIX_HOME=c:\projects\gitlab\QuantServer
        export JAVA_OPTS="-Ddeltix.home=c:\projects\gitlab\QuantServer -Xmx2483027968 -Xms1644167168 -verbose:gc -XX:+HeapDumpOnOutOfMemoryError"
        c:\projects\gitlab\QuantServer/bin/tomcat.sh -home "C:\QSHome\three" -tb
         */

        File cmd = new File(home, "service/timebase.sh");
        Utils.mkDirIfNeeded(cmd.getParentFile());
        try (PrintWriter writer = new PrintWriter(new FileWriter(cmd))) {
            writer.println("#!/usr/bin/env sh");
            writer.println("export DELTIX_HOME=" + installPath);
            writer.println("export JAVA_OPTS=\"-Ddeltix.home=" + installPath + " -Xmx2g -Xms1g\"");
            writer.println("" + installPath + "/bin/tdbserver.sh -home \"" + home + "\" -port " + port);
            writer.println("read -p \"Press enter to exit\" nothing");
        }

        LinuxOS.commandNoError("chmod", "a+rwx", cmd.getAbsolutePath());

        return cmd;
    }

    public static File generateLinuxService(String installPath, String home, String port, String user) throws IOException {
        /*
        [Unit]
        Description=${description}
        After=network.target
        StartLimitIntervalSec=0

        [Service]
        Type=simple
        Restart=always
        LimitNOFILE=65536
        User=${user}
        WorkingDirectory=${workingDir}
        Environment="DELTIX_HOME=${DELTIX_HOME}"
        Environment="JAVA_OPTS=${JAVA_OPTS}"
        ExecStart=${executable}

        [Install]
        WantedBy=multi-user.target
         */
        File service = new File(home, "service/tb" + port + ".service");
        Utils.mkDirIfNeeded(service.getParentFile());
        try (PrintWriter writer = new PrintWriter(new FileWriter(service))) {
            writer.println("[Unit]");
            writer.println("Description=timebase service on " + port);
            writer.println("After=network.target");
            writer.println("StartLimitIntervalSec=0");
            writer.println("");
            writer.println("[Service]");
            writer.println("Type=simple");
            writer.println("Restart=always");
            writer.println("LimitNOFILE=65536");
            writer.println("User=" + user);
            writer.println("WorkingDirectory=" + home);
            writer.println("Environment=\"DELTIX_HOME=" + installPath + "\"");
            writer.println("ExecStart=" + installPath + "/bin/tdbserver.sh -home \"" + home + "\" -port " + port);
            writer.println("");
            writer.println("[Install]");
            writer.println("WantedBy=multi-user.target");
        }

        return service;
    }

}