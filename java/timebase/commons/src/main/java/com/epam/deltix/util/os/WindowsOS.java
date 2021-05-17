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

import com.github.sarxos.winreg.HKey;
import com.epam.deltix.util.io.ProcessHelper;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.lang.StringUtils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;


/**
 *
 */
public final class WindowsOS {
    public static final boolean IS_X64;
    public static final boolean IS_X86;
    public static final boolean IS_VISTA;

    static {
        String PROC_ID = System.getenv ("PROCESSOR_IDENTIFIER");

        IS_X64 = PROC_ID != null && PROC_ID.contains ("64");

        IS_X86 = !IS_X64;

        String osName = System.getProperty ("os.name");
        IS_VISTA = osName.startsWith ("Windows Vista");
    }

    static String               mkshortcut = "set WshShell = WScript.CreateObject(\"WScript.Shell\" )\n"
                                             + "set oShellLink = WshShell.CreateShortcut(\"%s.lnk\")\n"
                                             + "oShellLink.TargetPath = %s \n" + "oShellLink.WorkingDirectory = %s\n"
                                             + "oShellLink.IconLocation = %s\n" + "oShellLink.WindowStyle = 1\n"
                                             + "oShellLink.Save";

    public static String            getSystemDrive () {
        String sysdrive = System.getenv("SYSTEMDRIVE");

        if (sysdrive == null)
            sysdrive = "C:";

        return (sysdrive);
    }

    public static  String           getProgramFiles () {
        String pf = System.getenv ("ProgramFiles");

        if (pf == null)
            pf = getSystemDrive () + "\\Program Files";

        return (pf);
    }

    public static String            getSystemRoot () {
        String pf = System.getenv ("SystemRoot");

        if (pf == null)
            pf = getSystemDrive () + "\\Windows";

        return (pf);
    }

    public static String            getAllUsersProfile () {
        String pf = System.getenv ("ALLUSERSPROFILE");

        if (pf == null)
            pf = getSystemDrive () + "\\Documents and Settings\\All Users";

        return (pf);
    }

    public static String            getPublic () {
        String p = System.getenv ("PUBLIC");

        if (p == null)
            p = getSystemDrive () + "\\Users\\Public";
        return (p);

    }

    public static String            getUserName () {
        String pf = System.getenv ("USERNAME");

        if (pf == null)
            pf = "Administrator";

        return (pf);
    }

    public static String            getUserProfile () {
        String pf = System.getenv ("USERPROFILE");

        if (pf == null)
            pf = getSystemDrive () + "\\Documents and Settings\\" + getUserName ();

        return (pf);
    }

    public static File              getDotNetHome () {
        return getDotNetHome (-1);
    }

    public static File              getDotNetHome (int version) {
        return getDotNetHome (false, version);
    }

    public static File              getDotNetHome (boolean force32, int version) {
        File dotNet = new File (getSystemRoot (), "Microsoft.NET");

        if (!dotNet.isDirectory ())
            return (null);

        File framework = null;

        if (!force32) {
            framework = new File (dotNet, "framework64");
            if (!framework.isDirectory ())
                framework = null;
        }

        if (framework == null) {
            framework = new File (dotNet, "framework");

            if (!framework.isDirectory ())
                framework = null;
        }

        if (framework == null)
            return (null);

        final String start = version < 1 ? "v" : "v" + version + ".";

        File[] homes = framework.listFiles (new FileFilter () {
            public boolean accept (File f) {
                return (f.isDirectory () && f.getName ().startsWith (start));
            }
        });

        if (homes == null)
            return (null);

        Arrays.sort (homes);

        return (homes[homes.length - 1]);
    }

    public static void              createShortcut (
            File target,
            File location,
            File icon) throws IOException
    {

        createShortcut (target, location.getAbsolutePath (), icon.getAbsolutePath ());
    }

    @SuppressFBWarnings("COMMAND_INJECTION")
    public static void              createShortcut (
            File target,
            String location,
            String icon) throws IOException
    {
        // create script that will make shortcut
        File script = File.createTempFile ("shcut", ".vbs");
        script.deleteOnExit ();
        FileWriter writer = null;
        try {
            writer = new FileWriter (script);
            writer.write (String.format (mkshortcut,
                                         location,
                                         StringUtils.quote (target.getAbsolutePath ()),
                                         StringUtils.quote (target.getParentFile ().getAbsolutePath ()),
                                         StringUtils.quote (icon)));
            writer.close ();
            writer = null;
        } finally {
            Util.close (writer);
        }

        (new ProcessBuilder ("cmd.exe",
                             "/K",
                             StringUtils.quote (script.getAbsolutePath ()))).start ();
    }

    static String GET_HARD_DISK_SERIAL_NUMBERS = "strComputer = \".\"\n"
                                                 + "Set objWMIService = GetObject(\"winmgmts:\\\\\" & strComputer & \"\\root\\CIMV2\")\n"
                                                 + "Set colItems = objWMIService.ExecQuery( _\n"
                                                 + "    \"SELECT * FROM Win32_DiskDrive\",,48)\n"
                                                 + "For Each objItem in colItems\n"
                                                 + "    Wscript.Echo objItem.Caption & \":\" & objItem.InterfaceType & \":\" & objItem.SerialNumber\n"
                                                 + "Next\n";
    @SuppressFBWarnings("COMMAND_INJECTION")
    public static String            getHardDiskSerial () {
        StringBuilder result = new StringBuilder();
        try {
            File file = File.createTempFile ("getHardDiskSerial",
                                             ".vbs");
            file.deleteOnExit ();
            FileWriter fw = new java.io.FileWriter (file);

            String vbs = GET_HARD_DISK_SERIAL_NUMBERS;
            fw.write (vbs);
            fw.close ();
            Process p = Runtime.getRuntime ().exec ("cscript //NoLogo " + file.getPath ());
            BufferedReader input = new BufferedReader (new InputStreamReader (p.getInputStream ()));
            String line;
            while ((line = input.readLine ()) != null) {
                result.append("%").append(line);
            }
            input.close ();
        } catch (Exception e) {
            e.printStackTrace ();
        }
        return result.toString().trim ();
    }

    public static String            getSystemSerial() {
        String number = getDiskSerialNumber(System.getenv("SYSTEMDRIVE"));
        number = number.length() > 0 ? number : getMBSerialNumber();

        if (number.length() == 0)
            throw new IllegalStateException("Cannot resolve HD serial and MB serial numbers.");

        return number;
    }

    @SuppressFBWarnings("COMMAND_INJECTION")
    public static String            getDiskSerialNumber (String drive) {
        String result = "";
        try {
            File file = File.createTempFile ("getSerialNumber", ".vbs");
            file.deleteOnExit ();
            FileWriter fw = new java.io.FileWriter (file);

            String vbs = "Set objFSO = CreateObject(\"Scripting.FileSystemObject\")\n" +
                         "Set colDrives = objFSO.Drives\n" + "Set objDrive = colDrives.item(\"" + drive + "\")\n" +
                         "Wscript.Echo objDrive.SerialNumber";
            fw.write (vbs);
            fw.close ();
            Process p = Runtime.getRuntime ().exec ("cscript //NoLogo " + StringUtils.quote (file.getAbsolutePath ()));
            BufferedReader input = new BufferedReader (new InputStreamReader (p.getInputStream ()));
            String line;
            while ((line = input.readLine ()) != null) {
                result += line;
            }
            input.close ();
        } catch (Exception e) {
            e.printStackTrace ();
        }

        result = result.trim ();
        
        return result.length() > 0 ? String.format ("%08X",  Integer.valueOf (result)) : result;
    }

    @SuppressFBWarnings("COMMAND_INJECTION")
    public static String            getMBSerialNumber() {
        String result = "";
        try {
            File file = File.createTempFile("getMBSerialNumber", ".vbs");
            file.deleteOnExit();
            FileWriter fw = new java.io.FileWriter(file);

            String vbs =
             "Set objWMIService = GetObject(\"winmgmts:\\\\.\\root\\cimv2\")\n"
            + "Set colItems = objWMIService.ExecQuery _ \n"
            + "   (\"Select * from Win32_BaseBoard\") \n"
            + "For Each objItem in colItems \n"
            + "    Wscript.Echo objItem.SerialNumber \n"
            + "    exit for  ' do the first cpu only! \n"
            + "Next \n";

            fw.write(vbs);
            fw.close();
            Process p = Runtime.getRuntime().exec("cscript //NoLogo " + StringUtils.quote (file.getAbsolutePath ()));
            BufferedReader input = new BufferedReader( new InputStreamReader(p.getInputStream()));
            String line;
            
            while ((line = input.readLine()) != null) {
                result += line;
            }
            input.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result.trim ();
    }

    @SuppressFBWarnings("COMMAND_INJECTION")
    public static String            getSystemUUID() {
        String result = "";
        try {
            File file = File.createTempFile("getSystemUUID", ".vbs");
            file.deleteOnExit();
            FileWriter fw = new java.io.FileWriter(file);

            String vbs =
                    "Set objWMIService = GetObject(\"winmgmts:\\\\.\\root\\cimv2\")\n"
                            + "Set colItems = objWMIService.ExecQuery _ \n"
                            + "   (\"Select * from Win32_ComputerSystemProduct\") \n"
                            + "For Each objItem in colItems \n"
                            + "    Wscript.Echo objItem.UUID \n"
                            + "Next \n";

            fw.write(vbs);
            fw.close();
            Process p = Runtime.getRuntime().exec("cscript //NoLogo " + StringUtils.quote (file.getAbsolutePath ()));
            BufferedReader input = new BufferedReader( new InputStreamReader(p.getInputStream()));
            String line;

            while ((line = input.readLine()) != null) {
                result += line;
            }
            input.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result.trim ();
    }
 
    public static int           start (String cmd) 
    	throws IOException, InterruptedException
    {
        return (ProcessHelper.execAndWait ("cmd", "/c", "start", cmd));
    }
    
    public static String cppRuntimeVersion(int runtimeNumber, boolean isx64)
            throws IllegalArgumentException
    {
        String value = WindowsUtils.regQuery (HKey.HKLM,
                                               "SOFTWARE\\Wow6432Node\\Microsoft\\VisualStudio\\" + runtimeNumber + ".0\\VC\\Runtimes\\" + (isx64 ? "x64" : "x86"),
                                               "Version");
        return value;
    }
    
    public static void main (String[] args) throws Exception {
        System.out.println ("C++ Runtime v11 (x64): " + cppRuntimeVersion(11, true));
        System.out.println ("C++ Runtime v11 (x86): " + cppRuntimeVersion(11, false));
        System.out.println ("getDotNetHome = " + getDotNetHome ());
        System.out.println ("getHardDiskSerial = " + getHardDiskSerial());
        System.out.println ("getMBSerialNumber = " + getMBSerialNumber());
        System.out.println ("getSystemDrive SerialNumber = " + getDiskSerialNumber(getSystemDrive()));
        System.out.println ("getSystemSerial = " + getSystemSerial ());
        System.out.println ("getSystemUUID = " + getSystemUUID ());

    }
}
