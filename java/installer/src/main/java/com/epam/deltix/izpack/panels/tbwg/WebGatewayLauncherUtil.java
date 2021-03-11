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
