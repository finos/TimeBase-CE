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

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.lang.Util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 *
 */
public class WatchdogService {
    private static final Log LOG = LogFactory.getLog(WatchdogService.class);
    public final static double          VERSION                 = 1.0;
    public final static String          SERVICE_NAME            = "DXWatchdog";
    public final static String          SERVICE_DESC            = "Deltix Watchdog service";
    public final static File            SERVICE_BIN             = Home.getFile("tools/DXWatchdog/" + SERVICE_NAME + ".exe");
    public final static File            SERVICE_CONFIG_BIN      = Home.getFile("tools/DXWatchdog/" + SERVICE_NAME + ".exe.config");
    public final static File            SERVICE_BIN32           = Home.getFile("tools/DXWatchdog/" + SERVICE_NAME + "32.exe");
    public final static File            SERVICE_CONFIG_BIN32    = Home.getFile("tools/DXWatchdog/" + SERVICE_NAME + "32.exe.config");
    public final static File            SERVICE_BIN_INST        = Home.getFile("../Utilities/DXWatchdog/" + SERVICE_NAME + ".exe");
    public final static File            SERVICE_CONFIG_BIN_INST = Home.getFile("../Utilities/DXWatchdog/" + SERVICE_NAME + ".exe.config");
    public final static File            SERVICE_UNINST          = Home.getFile("../Utilities/DXWatchdog/uninstall.bat");

    public final static String          WATCHDOG_PIPE           = "\\\\.\\pipe\\deltixWatchdogPipe";
    public final static String          EMPTY_RESPONSE          = "---EMPTY---";
    public final static int             RESTART_DELAY           = 10000;
    public final static int             RESTART_ATTEMPTS        = 30;

    public static WatchdogService       INSTANCE = new WatchdogService();

    private WatchdogService() {
    }

    public String                       sendRequest(String request) {
        try (NamedPipe pipe = new NamedPipe(WATCHDOG_PIPE, "rw")) {
            pipe.writeString(request);
            return pipe.readString();
        } catch (Throwable e) {
            LOG.warn("Error send DXWatchdog request: %s").with(e);
        }
        return "";
    }

    //Watchdog requests
    public double                       version() {
        double res = 0;
        try {
            res = Double.valueOf(sendRequest("VERSION"));
        } catch (NumberFormatException e) {
            LOG.warn("Error request version of DXWatchdog: %s").with(e);
        }

        return res;
    }

    public boolean                      register(String service) {
        return register("REG " + service, RESTART_DELAY, RESTART_ATTEMPTS);
    }

    public boolean                      register(String service, int restartDelay, int restartAttempts) {
        if (restartDelay < 0)
            restartDelay = RESTART_DELAY;
        if (restartAttempts < 0)
            restartAttempts = RESTART_ATTEMPTS;
        return sendCommand("REG " + service
            + "," + String.valueOf(restartDelay)
            + "," + String.valueOf(restartAttempts));
    }

    public boolean                      unregister(String service) {
        return sendCommand("UNREG " + service);
    }

    public String[]                     list() {
        String response = sendRequest("LIST");
        if (response.isEmpty() || response.equals(EMPTY_RESPONSE))
            return null;

        return response.split("\n");
    }

    private boolean                     sendCommand(String command) {
        String response = sendRequest(command);
        if (!response.equals("OK")) {
            LOG.warn("Watchdog response: %s").with(response);
            return false;
        }

        return true;
    }

    private boolean                     installFiles() {
        //copy files
        try {
            SERVICE_BIN_INST.getParentFile().mkdirs();
            if (Util.IS32BIT) {
                Files.copy(SERVICE_BIN32.toPath(), SERVICE_BIN_INST.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Files.copy(SERVICE_CONFIG_BIN32.toPath(), SERVICE_CONFIG_BIN_INST.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.copy(SERVICE_BIN.toPath(), SERVICE_BIN_INST.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Files.copy(SERVICE_CONFIG_BIN.toPath(), SERVICE_CONFIG_BIN_INST.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOG.warn("Error installing files: %s").with(e);
            return false;
        }

        //create uninstall file
        try {
            PrintWriter writer = new PrintWriter(SERVICE_UNINST.getCanonicalPath(), "UTF-8");
            writer.println("sc stop " + SERVICE_NAME);
            writer.println("sc delete " + SERVICE_NAME);
            writer.println("timeout /t 1 /nobreak > NUL");
            writer.println("del " + SERVICE_BIN_INST.getCanonicalPath());
            writer.println("del " + SERVICE_CONFIG_BIN_INST.getCanonicalPath());
            writer.close();
        } catch (IOException e) {
            LOG.warn("Error creating uninstall file: %s").with(e);

            return false;
        }

        return true;
    }

    //Service control operations
    public boolean                      install() {
        if (!Util.IS_WINDOWS_OS)
            throw new RuntimeException("Operation available only for WindowsOS");

        if (installFiles() == false)
            return false;

        //register service
        ServiceControl.CreationParameters params = new ServiceControl.CreationParameters();
        params.displayName = SERVICE_NAME;
        params.startMode = ServiceControl.StartMode.auto;

        try {
            ServiceControlFactory.getInstance().create(SERVICE_NAME, SERVICE_DESC, SERVICE_BIN_INST.getCanonicalPath(), params);
        } catch (Exception e) {
            LOG.warn("Error installing service control operation: %s").with(e);
            return false;
        }

        return true;
    }

    public boolean                      uninstall() {
        if (!Util.IS_WINDOWS_OS)
            throw new RuntimeException("Operation available only for WindowsOS");

        try {
            ServiceControlFactory.getInstance().delete(SERVICE_NAME);
        } catch (Exception e) {
            LOG.warn("Error uninstalling operation: %s").with(e);
            return false;
        }

        return true;
    }

    public boolean                      start() {
        try {
            ServiceControlFactory.getInstance().startAndWait(SERVICE_NAME, true, 10000);
        } catch (Exception e) {
            LOG.warn("Error starting service: %s").with(e);
            return false;
        }

        return true;
    }


    public boolean                      stop() {
        try {
            ServiceControlFactory.getInstance().stopAndWait(SERVICE_NAME, true, 10000);
        } catch (Exception e) {
            LOG.warn("Error stopping service: %s").with(e);
            return false;
        }

        return true;
    }

    public boolean                      exists() {
        boolean exists = false;
        try {
            exists = ServiceControlFactory.getInstance().exists(SERVICE_NAME);
        } catch (Exception e) {
            LOG.warn("Error checking if service exist: %s").with(e);
        }

        return exists;
    }

    public boolean                      running() {
        boolean running = false;
        try {
            running = ServiceControlFactory.getInstance()
                .queryStatusName(SERVICE_NAME)
                .equalsIgnoreCase(ServiceControl.STATUS_RUNNING);
        } catch (Exception e) {
            LOG.warn("Error checking if service is running: %s").with(e);
        }

        return running;
    }

    //cmd operations
    static public void                  printHelpInfo() {
        System.out.println(" Commands:");
        System.out.println("\tVERSION - get Watchdog's version.");
        System.out.println("\tREG service name [,start delay (millis)] [,start attempts] - register service.");
        System.out.println("\tUNREG service name - unregister service.");
        System.out.println("\tLIST [INF] - get registred services (use inf argument to get additional info).");
        System.out.println("\tAlso use START, STOP, INSTALL, UNINSTALL commands to control service.");
    }

    static private void                 installServiceCmd() {
        if (!WatchdogService.INSTANCE.exists()) {
            if (WatchdogService.INSTANCE.install()) {
                WatchdogService.INSTANCE.start();
                System.out.println("DXWatchdog installed successfully.");
            } else
                System.out.println("Error install DXWatchdog.");
        } else {
            System.out.println("Service already uninstalled.");
        }
    }

    static private void                 uninstallServiceCmd() {
        if (WatchdogService.INSTANCE.exists()) {
            WatchdogService.INSTANCE.stop();
            if (WatchdogService.INSTANCE.uninstall())
                System.out.println("DXWatchdog uninstalled successfully.");
            else
                System.out.println("Error uninstall DXWatchdog.");
        } else {
            System.out.println("Service already uninstalled.");
        }
    }

    static public void                  main(String[] args) {
        //launch with params
        if (args.length > 0) {
            if (args[0].compareToIgnoreCase("install") == 0) {
                installServiceCmd();
            }
            else if (args[0].compareToIgnoreCase("uninstall") == 0) {
                uninstallServiceCmd();
            } else {
                System.out.println("Unknown parameter: " + args[0]);
            }
            return;
        }

        //without params we get console control
        BufferedReader con = new BufferedReader(new InputStreamReader(System.in));
        System.out.format("Deltix Service Watchdog Console v. %.2f (try 'help' for information)", WatchdogService.INSTANCE.VERSION);
        String request = "";
        do {
            try {
                System.out.print("\n >");
                request = con.readLine();
                if (request == null) break;
                if (request.compareToIgnoreCase("HELP") == 0)
                    printHelpInfo();
                else if (request.compareToIgnoreCase("INSTALL") == 0)
                    installServiceCmd();
                else if (request.compareToIgnoreCase("UNINSTALL") == 0)
                    uninstallServiceCmd();
                else if (request.compareToIgnoreCase("START") == 0) {
                    if (WatchdogService.INSTANCE.start())
                        System.out.println("Started successfully");
                } else if (request.compareToIgnoreCase("STOP") == 0) {
                    if (WatchdogService.INSTANCE.stop())
                        System.out.println("Stopped successfully");
                } else if (request.compareToIgnoreCase("QUIT") == 0)
                    break;
                else
                    System.out.print(WatchdogService.INSTANCE.sendRequest(request));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } while (true);
    }
}