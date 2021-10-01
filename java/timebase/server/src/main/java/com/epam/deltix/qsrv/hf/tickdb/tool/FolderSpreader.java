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
package com.epam.deltix.qsrv.hf.tickdb.tool;

import com.epam.deltix.util.progress.ConsoleProgressIndicator;
import com.epam.deltix.util.progress.ProgressIndicator;
import com.epam.deltix.qsrv.hf.tickdb.impl.TickDBImpl;

import java.io.*;
import java.util.*;

/**
 *
 */
public class FolderSpreader {

    public static interface EventListener extends ProgressIndicator {
        public void info(String text);
        public void warning(String text);
    }

    private static class EmptyListener implements EventListener {
        public void info(String text) {
        }

        public void warning(String text) {            
        }

        public void setTotalWork(double v) {
        }

        public void setWorkDone(double v) {
        }

        public void incrementWorkDone(double inc) {
        }
    }

    public static class DefaultListener extends ConsoleProgressIndicator implements EventListener {
        public void info(String text) {
            System.out.println(text);
        }
        public void warning(String text) {
            System.out.println(text);
        }
    }

    private EventListener listener;

    public FolderSpreader() {
        this(null);
    }

    public FolderSpreader(EventListener listener) {
        this.listener = listener == null ? new EmptyListener() : listener;        
    }

    public  void          distribute (File ... dirs) throws IOException {
        for (File dir : dirs) {
            distribute(dir);
        }
    }

    public void         distribute (File dir) throws IOException {
        listener.info("Opening " + dir + " ...");

        File []         files = dir.listFiles ();

        if (files == null)
            throw new FileNotFoundException (dir.getPath ());

        int             n = files.length;

        listener.info ("There are " + n + " files.");

        Map <String, File>  streamDirs = new HashMap <String, File> ();
        Map <File, String>  fileMap = new HashMap <File, String> ();
        List <File>         deleteList = new ArrayList <File> ();

        for (File ff : files) {
            String      name = ff.getName ();
            int         nlen = name.length ();

            if (name.equals (TickDBImpl.MD_FILE_NAME) || name.equals (TickDBImpl.CATALOG_NAME))
                ;
            else if (name.endsWith (".bak"))
                deleteList.add (ff);
            else if (name.endsWith (".uhfq.xml")) {
                String      sk = name.substring (0, nlen - 9);

                streamDirs.put (sk, new File (dir, sk));
            }
            else if ((name.startsWith ("x.") || name.startsWith ("m.")
                    || name.startsWith ("x1.") || name.startsWith ("m1.")) && name.endsWith (".dat")) {
                int     dot = name.lastIndexOf ('.', nlen - 5);

                if (dot < 6)
                    throw new RuntimeException ("Unrecognized file: " + name + " (" + dot + ")");

                String  sk = name.substring (dot + 1, nlen - 4);

                fileMap.put (ff, sk);
            }
            else
                listener.warning ("WARNING: Unrecognized file: " + ff);
        }

        listener.info ("Removing .bak files ...");
        for (File f : deleteList)
            f.delete ();

        listener.info ("Creating stream folders ...");

        //FileWriter  cat = new FileWriter (new File (dir, TickDBFactory.CATALOG_NAME), true);

        for (Map.Entry <String, File> e : streamDirs.entrySet ()) {
            File    sd = e.getValue ();
            String  name = e.getKey () + ".uhfq.xml";

            sd.mkdir ();
            new File (dir, name).renameTo (new File (sd, name));

            //cat.write (sd.getCanonicalPath ());
            //cat.write ("\n");
        }

        //cat.close ();

        listener.info ("Moving files ...");
        listener.setTotalWork (fileMap.size ());
        listener.setWorkDone(0);

        for (Map.Entry <File, String> e : fileMap.entrySet ()) {
            File    f = e.getKey ();
            String  sk = e.getValue ();
            String  name = f.getName ();

            f.renameTo (new File (streamDirs.get (sk), name));
            listener.incrementWorkDone (1);
        }
        listener.info("\nAll done.");
    }

//    public static void main (String [] args) throws Exception {
//        FolderSpreader fs = new FolderSpreader(new DefaultListener());
//        File dir = new File(args[0]);
//        fs.distribute (dir);
//
//        new ProcessBuilder ("explorer", dir.getPath ()).start ();
//    }
}