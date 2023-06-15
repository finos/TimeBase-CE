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
package com.epam.deltix.test.qsrv.hf.tickdb.testframework;

import com.epam.deltix.qsrv.comm.cat.TBServerCmd;
import com.epam.deltix.qsrv.util.servlet.ShutdownServlet;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.io.StreamPump;
import com.epam.deltix.util.io.URLConnectionFactory;
import com.epam.deltix.util.lang.Util;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Paths;

/**
 *
 */
public class TBServerProcess {

    private final String        workFolder;
    private final String[]      command;
    private final String[]      reconnectCommand;

    private Process             process;
    private StreamPump          streamPump = null;

    private int                 port;


    public TBServerProcess(String workFolder, int port) {
        this(TBServerCmd.class.getName(), workFolder, port);
    }

    public TBServerProcess(String className, String workFolder, int port) {
        this(className, workFolder, createTbFolder(workFolder), port);
    }

    public TBServerProcess(String className, String workFolder, String tbFolder, int port) {
        this.workFolder = workFolder;
        this.port = port;
        //String runJava = Home.getPath(Util.IS_WINDOWS_OS ? "bin\\runjava.cmd" : "bin/runjava");
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome == null || !new File(javaHome).exists())
            javaHome = Home.getPath("jre", "bin", Util.IS_WINDOWS_OS ? "java.exe" : "java");
        else
            javaHome = Paths.get(javaHome, "bin", Util.IS_WINDOWS_OS ? "java.exe" : "java").toString();


        final File clsCp = new File(TBServerProcess.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        command =
            new String[]{
                javaHome,
                "-cp", Home.getPath("lib\\*") + ";" + clsCp.getAbsolutePath(),
                "-DTimeBase.version=5.0",
                className,
                "-tb", "-home", tbFolder, "-port", String.valueOf(port)
            };

        reconnectCommand =
            new String[]{
                javaHome,
                "-cp", Home.getPath("lib\\*") + ";" + clsCp.getAbsolutePath(),
                "-DTimeBase.version=5.0",
                className,
                "-tb", "-home", tbFolder, "-port", String.valueOf(port), "-nformat"
            };
    }

    private static String       createTbFolder(String workFolder) {
        File tbFolder = new File(workFolder, "tickdb");
        tbFolder.mkdirs();
        return tbFolder.getAbsolutePath();
    }

    public void                 start() throws IOException {
        process = startProcess(command);
    }

    public void                 stop(boolean graceful) throws IOException {
        if (graceful) {
            if (callShutdownEndpoint() == ShutdownServlet.SHUTDOWN_PARAM) {
                process = null;
            } else {
                throw new RuntimeException("Service refused to stop (wrong password or wrong service)");
            }
        } else {
            stopProcess(process);
            process = null;
        }
    }

    public boolean              isStarted() {
        try {
            String home = callHomeEndpoint();
            return !home.isEmpty();
        } catch (IOException e) {
        }

        return false;
    }

    public void                 restart() throws IOException {
        stop(false);
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

    private int callShutdownEndpoint() throws IOException {
        URLConnection connection = URLConnectionFactory.create("localhost", port, "/shutdown...", false);
        connection = URLConnectionFactory.verify(connection, null, null);
        return connection.getInputStream().read();
    }

    private String callHomeEndpoint() throws IOException {
        URLConnection connection = URLConnectionFactory.create("localhost", port, "/gethome", false);
        connection = URLConnectionFactory.verify(connection, null, null);
        return new DataInputStream(connection.getInputStream()).readUTF();
    }

}