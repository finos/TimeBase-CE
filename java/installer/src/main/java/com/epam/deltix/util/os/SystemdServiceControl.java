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
package com.epam.deltix.util.os;

import com.epam.deltix.izpack.LinuxOS;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

public class SystemdServiceControl extends ServiceControl {

    public static final SystemdServiceControl INSTANCE = new SystemdServiceControl();

    @Override
    public void load(String home) {
    }

    @Override
    public void addDependency(String id, String dependId) throws IOException, InterruptedException {
        // empty
    }

    @Override
    public void create(String id, String description, String binPath, CreationParameters params) throws IOException {
        LinuxOS.command("sudo", "cp", binPath, "/etc/systemd/system");
        LinuxOS.command("sudo", "systemctl", "daemon-reload");
        LinuxOS.command("sudo", "systemctl", "enable", id);
    }

    @Override
    public void delete(String id) throws IOException, InterruptedException {
        stopAndWait(id, false, 600000);
        LinuxOS.command("sudo", "systemctl", "disable", id);
        Path path = Paths.get("/etc/systemd/system", id + ".service");
        path.toFile().delete();
    }

    @Override
    public boolean exists(String id) throws IOException, InterruptedException {
        Path path = Paths.get("/etc/systemd/system", id + ".service");
        return path.toFile().exists();
    }

    @Override
    public String getExecutablePath(String id) throws InvocationTargetException, IllegalAccessException {
        final Path service = Paths.get("/etc/systemd/system/" + id + ".service");

        if (!Files.exists(service)) {
            return null;
        }

        if (!Files.isSymbolicLink(service)) {
            return service.toString();
        }

        try {
            return service.toRealPath().toString();
        } catch (IOException e) {
            throw new InvocationTargetException(e);
        }
    }

    @Override
    public String queryStatusName(String id) throws IOException, InterruptedException {
        StringBuilder sb = new StringBuilder();
        LinuxOS.command(sb, "sudo", "systemctl", "is-active", id);
        return sb.toString();
    }

    @Override
    public void start(String id) throws IOException, InterruptedException {
        LinuxOS.command("sudo", "systemctl", "start", id);
    }

    @Override
    public void startAndWait(String id, boolean ignoreQueryErrors, long timeout) throws IOException, InterruptedException {
        long limit = System.currentTimeMillis() + timeout;

        start(id);

        for (;;) {
            StringBuilder builder = new StringBuilder();
            LinuxOS.command(builder, "sudo", "systemctl", "is-active", id);
            String status = builder.toString();
            if (status.endsWith("\n")) {
                status = status.substring(0, status.length() - 1);
            }

            if (status.equals("active")) {
                break;
            }

            if (status.equals("inactive")) {
                start(id);
            }

            if (System.currentTimeMillis() >= limit) {
                throw new InterruptedException("Service failed to start after period of time (" + Instant.ofEpochMilli(limit) + ").");
            }

            Thread.sleep(500);
        }
    }

    @Override
    public void stop(String id) throws IOException, InterruptedException {
        LinuxOS.command("sudo", "systemctl", "stop", id);
    }

    @Override
    public void stopAndWait(String id, boolean ignoreQueryErrors, long timeout) throws IOException, InterruptedException {
        long limit = System.currentTimeMillis() + timeout;

        for (;;) {
            StringBuilder builder = new StringBuilder();
            try {
                LinuxOS.command(builder, "sudo", "systemctl", "is-active", id);
            } catch (Exception exc) {
                break;
            }
            String status = builder.toString();
            if (status.endsWith("\n")) {
                status = status.substring(0, status.length() - 1);
            }

            if (status.equals("inactive")) {
                break;
            }

            if (status.equals("active")) {
                stop(id);
            }

            if (System.currentTimeMillis() >= limit) {
                throw new InterruptedException("Service failed to stop after period of time (" + Instant.ofEpochMilli(limit) + ").");
            }

            Thread.sleep(500);
        }
    }

    @Override
    public void setFailureAction(String id, FailureAction action, int delay, String command) throws IOException, InterruptedException {
        // not implemented
    }
}