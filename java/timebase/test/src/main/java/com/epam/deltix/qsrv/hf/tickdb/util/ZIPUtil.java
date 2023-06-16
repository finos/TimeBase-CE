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
package com.epam.deltix.qsrv.hf.tickdb.util;

import com.epam.deltix.util.io.BasicIOUtil;
import com.epam.deltix.util.lang.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZIPUtil {
    public static ZipEntry[] listZipEntries (File f, String regex)
            throws IOException, InterruptedException
    {
        FileInputStream fis = new FileInputStream(f);
        ArrayList<ZipEntry> ret = new ArrayList<ZipEntry>();
        Matcher m = null;

        if (regex != null) {
            Pattern pat = Pattern.compile (regex);
            m = pat.matcher ("");
        }

        try {
            ZipInputStream zis = new ZipInputStream(fis);

            for (;;) {
                ZipEntry zentry = zis.getNextEntry ();

                if (zentry == null)
                    break;

                if (m != null) {
                    m.reset (zentry.getName ());

                    if (!m.matches ())
                        continue;
                }

                ret.add (zentry);
            }
        } finally {
            Util.close (fis);
        }

        return (ret.toArray (new ZipEntry[ret.size ()]));
    }

    public static void          extractZipFile (File zip, File destDir)
            throws IOException, InterruptedException
    {
        InputStream is = new FileInputStream(zip);

        try {
            extractZipStream (is, destDir);
        } finally {
            Util.close (is);
        }
    }

    public static void          extractZipStream (InputStream is, File destDir)
            throws IOException, InterruptedException
    {
        ZipInputStream zis = new ZipInputStream(is);
        byte []             buffer = new byte [4096];

        for (;;) {
            ZipEntry zentry = zis.getNextEntry ();

            if (zentry == null)
                break;

            String name = zentry.getName ();
            File destFile = new File(destDir, name);

            if (name.endsWith ("/"))
                BasicIOUtil.mkDirIfNeeded (destFile);
            else {
                BasicIOUtil.mkParentDirIfNeeded (destFile);

                BasicIOUtil.copyToFile (zis, destFile, zentry.getSize (), buffer);
            }
        }
    }

}