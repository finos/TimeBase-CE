package com.epam.deltix.util;

import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.lang.Util;
import java.io.*;
import com.epam.deltix.gflog.Log;
import com.epam.deltix.gflog.LogFactory;
import com.epam.deltix.gflog.LogLevel;


/**
 * This file is code-generated. Do not edit.
 */
public abstract class Version {

    private static final Log            LOG = LogFactory.getLog(Version.class);
    public static final int             MAJOR = 6;
    public static final int             MINOR = 0;
    public static final String          NAME = "12-SNAPSHOT";
    public static final String          BUILD = "35e64ee76d";
    public static final Integer         COMMITS_AFTER_TAG = null;
    public static final String          BUILD_DATE = "2021-02-15 11:04:03 +0300";

    public static final String          VERSION_STRING;
    public static final String          MAJOR_VERSION_STRING;
       
    // public static final int VERSION_CODE = (MAJOR << 16) | (MINOR << 8) | (int) Long.parseLong(BUILD, 16);

    static {
        StringBuilder   sb = new StringBuilder ();
        
        sb.append (MAJOR);
        sb.append (".");
        sb.append (MINOR);
        sb.append (".");
        sb.append (NAME);

        MAJOR_VERSION_STRING = sb.toString();

        sb.append ("-");
        if (COMMITS_AFTER_TAG != null) {
            sb.append (COMMITS_AFTER_TAG);
        } else {
            sb.append ("?");
        }

        sb.append ("-");
        sb.append (BUILD);
        
        FileReader  fr = null;
        
        if (Home.isSet()) {
            try {
                File    pf = Home.getFile ("build/classes/patches");
                File [] pfhs = pf.listFiles ();

                if (pfhs != null) {
                    for (File pfh : pfhs) {
                        if (!pfh.getName ().toLowerCase ().endsWith (".txt"))
                            continue;

                        fr = new FileReader (pfh);

                        BufferedReader  bfr = new BufferedReader (fr);
                        String          h = bfr.readLine ();
                        fr.close ();

                        sb.append ('+');
                        sb.append (h);
                    }
                }

            } catch (Throwable x) {
                LOG.error("Error collecting patch data: %s").with(x);
            } finally {
                Util.close (fr);
            }
        }
        VERSION_STRING = sb.toString ();
    }

    public static void                  main (String [] args) {
        System.out.println ("Deltix Software - Version " + VERSION_STRING);
    }
}
