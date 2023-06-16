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
package com.epam.deltix.izpack;

import com.epam.deltix.izpack.panels.timebase.TimebaseLauncherUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LinuxOS {

    public final static Logger LOGGER = Logger.getLogger(TimebaseLauncherUtil.class.getName());

    public static boolean isSuperUser() {
        StringBuilder sb = new StringBuilder();
        commandNoError(sb, "id", "-u");

        try {
            String value = sb.toString();
            value = value.substring(0, value.indexOf('\n')).trim();
            return Integer.parseInt(value) == 0;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void execLinuxCmd(File file, String title) {
        execLinuxCmd("sh", title, file);
    }

    public static void execLinuxCmd(String shell, String title, File script) {
        try {
            Runtime.getRuntime().exec(paramsForStartScriptInTerminal(shell, title, script));
        } catch (Throwable x) {
            throw new RuntimeException(x);
        }
    }

    public static String[] paramsForStartScriptInTerminal(String shell, String title, File script, String... parameters) {

        List<String> cmdarray = new ArrayList<>();
        if (new File("/usr/bin/gnome-terminal").exists()) {
            cmdarray.add("/usr/bin/gnome-terminal");
            //cmdarray.add("-t");
            //cmdarray.add(title);
            cmdarray.add("-e");
        } else if (new File("/usr/bin/xterm").exists()) {
            cmdarray.add("/usr/bin/xterm");
            cmdarray.add("-T");
            cmdarray.add(title);
            cmdarray.add("-e");
        }
        //FIXME: do not delete - for a future use
//        else if (new File("/usr/bin/konsole").exists()) {
//            cmdarray.add("/usr/bin/konsole");
//            cmdarray.add("--title");
//            cmdarray.add(title);
//            cmdarray.add("-e");
//        }
        else {
            throw new IllegalStateException("Cann't find supported terminal. Please install 'gnome-terminal' or 'xtrem'.");
        }
        String command = shell + " -c \"'" + script.getPath() + "'";

        if (parameters != null) {
            for (String parameter : parameters) {
                command += " ";
                command += parameter;
            }
        }
        command += "\"";
        cmdarray.add(command);
        return cmdarray.toArray(new String[cmdarray.size()]);
    }

    public static void commandNoError(String... parameters) {
        commandNoError(null, parameters);
    }

    public static void commandNoError(StringBuilder sb, String... parameters) {
        try {
            command(sb, parameters);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An error while execution", e);
        }
    }

    public static void chown(File file, String user, boolean deep) throws IOException {
        if (deep) {
            command(null, new String[] {"chown", "-R", user, file.getAbsolutePath()});
        } else {
            command(null, new String[] {"chown", user, file.getAbsolutePath()});
        }
    }

    public static void command(String... parameters) throws IOException {
        command(System.out, parameters);
    }

    public static <T extends Appendable> T command(T out, String... parameters) throws IOException {
        try {
            if (parameters == null || parameters.length == 0)
                return out;
            final ProcessBuilder pb = new ProcessBuilder (parameters);

            pb.redirectErrorStream (true);

            final Process proc = pb.start ();
            final BufferedReader rd = new BufferedReader (new InputStreamReader(proc.getInputStream ()));

            for (; ;) {
                final String line = rd.readLine ();

                if (line == null)
                    break;

                if (out != null) {
                    out.append(line);
                    out.append("\n");
                }
            }

            final int exitVal = proc.waitFor ();
            if (exitVal != 0) {
                throw new RuntimeException(parameters[0] + " function failed with error code " + exitVal);
            }

        } catch (InterruptedException e) {
            throw new IOException (e);
        }

        return out;
    }

}