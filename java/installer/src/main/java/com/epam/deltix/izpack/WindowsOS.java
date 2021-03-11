package com.epam.deltix.izpack;

import java.io.File;
import java.io.IOException;

public class WindowsOS {

    public static void execWindowsCmd(File file, String title) throws IOException {
        execWindowsCmd(null, title, quote(file.getPath()));
    }

    public static void execWindowsCmd(File dir, String title, String... cmd) throws IOException {
        if (title == null)
            title = cmd[0];

        ProcessBuilder builder =
            new ProcessBuilder("cmd.exe", "/C", "start", quote(title));

        for (String s : cmd)
            builder.command().add(s);

        if (dir != null)
            builder.directory(dir);

        builder.start();
    }

    public static String quote(String value) {
        String result = "\"\"";
        if (value.length() > 0 && value.charAt(0) != '"')
            result = "\"" + value;
        if (value.length() > 0 && value.charAt(value.length() - 1) != '"')
            result += "\"";

        return result;
    }

}
