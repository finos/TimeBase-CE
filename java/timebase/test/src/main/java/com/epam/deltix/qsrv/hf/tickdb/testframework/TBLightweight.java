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
package com.epam.deltix.qsrv.hf.tickdb.testframework;

import com.epam.deltix.qsrv.hf.tickdb.comm.server.TestServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.DataCacheOptions;
import com.epam.deltix.util.cmdline.DefaultApplication;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.io.StreamPump;
import com.epam.deltix.util.lang.Util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

/**
 *
 */
public class TBLightweight extends DefaultApplication {
    private TestServer server;

    static public class TBProcess {
        private final String        workFolder;
        private final String[]      command;
        private final String[]      reconnectCommand;

        private Process       process;
        private StreamPump streamPump = null;

        public TBProcess(String workFolder, String tbFolder, int port) {
            this.workFolder = workFolder;
            //String runJava = Home.getPath(Util.IS_WINDOWS_OS ? "bin\\runjava.cmd" : "bin/runjava");
            String javaHome = System.getenv("JAVA_HOME");
            if (javaHome == null || !new File(javaHome).exists())
                javaHome = Home.getPath("jre", "bin", Util.IS_WINDOWS_OS ? "java.exe" : "java");
            else
                javaHome = Paths.get(javaHome, "bin", Util.IS_WINDOWS_OS ? "java.exe" : "java").toString();

            command =
                new String [] {
                    javaHome,
                    "-jar", Home.getPath("bin" , "runjava.jar"),
                    TBLightweight.class.getName(),
                    "-location",
                    tbFolder,
                    "-port",
                    String.valueOf(port)
                };

            reconnectCommand =
                new String [] {
                    javaHome,
                    "-jar", Home.getPath("bin", "runjava.jar"),
                    TBLightweight.class.getName(),
                    "-location",
                    tbFolder,
                    "-port",
                    String.valueOf(port),
                    "-nformat"
                };
        }

        public void                 start() throws IOException {
            process = startProcess(command);
        }

        public void                 stop() {
            stopProcess(process);
            process = null;
        }

        public void                 restart() throws IOException {
            stop();
            process = startProcess(reconnectCommand);
        }

        private Process             startProcess(String[] cmd) throws IOException {
            StringBuilder   sb = new StringBuilder("Starting ");
            for (String s : cmd) {
                sb.append(" ");
                sb.append(s);
            }
            sb.append(" ...");
            System.out.println(sb);

            ProcessBuilder      pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            pb.directory(new File(workFolder));
            Process proc = pb.start();

            streamPump = new StreamPump(proc.getInputStream(), System.out, true, false);
            streamPump.start();

            return proc;
        }

        private  void               stopProcess(Process proc) {
            if (proc == null) {
                return;
            }

            System.out.println("Destroying process ...");
            proc.destroy();
            if (streamPump != null) {
                streamPump.interrupt();
                try {
                    streamPump.join();
                } catch (InterruptedException e) { }
            }
        }
    }

    protected TBLightweight(String [] args) {
        super(args);
    }

    protected void          run() throws Throwable {
        System.out.println("Starting TimeBase...");

        String location = getArgValue("-location");

        if (location == null)
            throw new NullPointerException("-location parameter isn't specified");

        int port = getIntArgValue("-port", 7788);
        boolean format = !isArgSpecified("-nformat");

        File timebase = new File(location);
        if (!timebase.exists() && !timebase.mkdirs())
            throw new NullPointerException("Cannot create folder: " + location);

        server = new TestServer(port, null, null, new DataCacheOptions(), timebase);
        int actual = server.start();

        System.out.println("Started on port " + actual);
        if (format == false)
            System.out.println("Without formatting");

        while (true) {
            Thread.sleep(100);
        }
    }

    public static void      main(String [] args) {
      new TBLightweight(args).start();
    }
}