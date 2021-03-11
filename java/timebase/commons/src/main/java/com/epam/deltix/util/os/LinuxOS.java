package com.epam.deltix.util.os;

import com.epam.deltix.gflog.Log;
import com.epam.deltix.gflog.LogFactory;
import com.epam.deltix.util.concurrent.UncheckedInterruptedException;
import com.epam.deltix.util.lang.Util;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class LinuxOS {

    protected static final Log LOG = LogFactory.getLog(LinuxOS.class);

    public static boolean isX64 () {
        try {
            final ProcessBuilder pb = new ProcessBuilder ("uname",
                                                          "-m");

            pb.redirectErrorStream (true);

            final Process proc = pb.start ();
            final BufferedReader rd = new BufferedReader (new InputStreamReader (proc.getInputStream ()));

            String out = "";
            for (;;) {
                final String line = rd.readLine ();

                if (line == null)
                    break;
                out += line;
            }

            final int exitVal = proc.waitFor ();
            if (exitVal != 0)
                throw new IOException ("isX64 function failed with error code " + exitVal);

            return out.contains("64");
        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);

        } catch (InterruptedException e) {
            throw new UncheckedInterruptedException (e);
        }
    }

    public static void browse(URI uri) throws IOException {
        assert uri != null;

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
            Desktop.getDesktop().browse(uri);
        else
            throw new RuntimeException("Browse functionality is not supported");
    }

    public static void open(File dir) throws IOException {
        assert dir != null;

        DesktopApi.open(dir);

    }

    public static void startScriptInTerminal(String shell, String title, File script) {
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

//    public static LiveProcess[] getProcessList() throws Exception {
//        final List<LiveProcess> result = new ArrayList<>();
//
//        final AlignedNoWhitespacesTable psTable = new AlignedNoWhitespacesTable(
//                    command(new StringBuilder(), "ps", "-eo", "pid,comm,cmd")
//                );
//
//        final int pid = psTable.getColumn("PID").getIndex();
//        final int name = psTable.getColumn("COMMAND").getIndex();
//        final int cmd = psTable.getColumn("CMD").getIndex();
//
//        for (Row row : psTable) {
//            result.add(
//                    new LiveProcess(Integer.parseInt(row.getValue(pid).toString()),
//                    row.getValue(name).toString(),
//                    row.getValue(cmd).toString()));
//
//        }
//
//        return result.toArray(new LiveProcess[result.size()]);
//    }

    public static void commandNoError (String... parameters) {
        try {
            command(null, parameters);
        } catch (Exception e) {
            LOG.debug().append("An error while execution ").append(Arrays.toString(parameters)).append(e).commit();
        }
    }
    
    public static void chown (File file, String user, boolean deep) throws IOException {
        if (deep) {
            command(null, new String[] {"chown", "-R", user, file.getAbsolutePath()});
        } else {
            command(null, new String[] {"chown", user, file.getAbsolutePath()});
        }
    }

    public static void command (String... parameters) throws IOException {
        command(System.out, parameters);
    }
    
    public static <T extends Appendable> T command (T out, String... parameters) throws IOException {
        try {

            if (Util.IS_WINDOWS_OS || parameters == null || parameters.length == 0)
                return out;
            final ProcessBuilder pb = new ProcessBuilder (parameters);

            pb.redirectErrorStream (true);

            final Process proc = pb.start ();
            final BufferedReader rd = new BufferedReader (new InputStreamReader (proc.getInputStream ()));
           
            for (; ;) {
                final String line = rd.readLine ();

                if (line == null)
                    break;
                
                if (out != null) {
                    out.append (line);
                    out.append (Util.NATIVE_LINE_BREAK);
                }
            }

            final int exitVal = proc.waitFor ();
            if (exitVal != 0)
                throw new ExecutionException(parameters[0] + " function failed with error code " + exitVal, exitVal);

        } catch (InterruptedException e) {
            throw new IOException (e);
        }
        
        return out;
    }        
    

}
