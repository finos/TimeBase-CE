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
package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.io.StreamPump;
import com.epam.deltix.util.lang.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ProcessLauncher {

    private Process                   process;
    private String                    workFolder;
    private String[]                  command;
    private static final String       PREFIX = "####### >> ";

    public void                 setUp(String className, String workFolder, String[] params) throws Throwable {
        this.workFolder = workFolder;
        IOUtil.mkDirIfNeeded(new File(workFolder));

        /*
        tbFolder =
            System.getProperty(
                "test.tbdeath.tbFolder",
                workFolder + "/tickdb");
                */


        List<String> commandList = new ArrayList<>();
        commandList.add(Home.getPath(Util.IS_WINDOWS_OS ? "jre\\bin\\java.exe" : "jre/bin/java"));
        commandList.add("-jar");
        commandList.add(Home.getPath("bin/runjava.jar"));
        commandList.add(className);
        for (String param : params)
            commandList.add(param);

        command = new String[commandList.size()];
        commandList.toArray(command);
    }

    public void                 startProcess() throws IOException {
        StringBuilder   sb = new StringBuilder("Starting ");
        for (String s : command) {
            sb.append(" ");
            sb.append(s);
        }
        sb.append(" ...");
        log(sb);

        ProcessBuilder      pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        pb.directory(new File(workFolder));
        process = pb.start();

        new StreamPump(process.getInputStream(), System.out, true, false).start();
    }

    public  void               stopProcess() {
        if (process == null) {
            log("TB is already stopped");
            return;
        }

        try {
            log("Destroying process ...");
            process.destroy();
        } catch (Throwable x) {
            x.printStackTrace();
            System.exit(1003);
        }
    }

    private static void          log(Object ... args) {
        synchronized (System.out) {
            System.out.print(PREFIX);

            for (Object arg : args)
                System.out.print (arg);

            System.out.println ();
        }
    }
}