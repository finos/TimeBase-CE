/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.qsrv;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.config.QuantServiceConfig;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.memory.HeapDumper;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipOutputStream;

/**
 *
 */
public class HeapDumpDownloadHelper {
    private static final Log LOG = LogFactory.getLog(HeapDumpDownloadHelper.class);
    private static final File                   dumpsDir = QSHome.getFile("dumps");
    private static final String                 FS = "yyyyMMdd[HH]";
    private static final SimpleDateFormat       DF = new SimpleDateFormat(FS);

    public static String getZippedHeapDumpFilename() {
        return getHeapDumpName() + ".zip";
    }

    public static void dumpAndStore(OutputStream os) throws IOException, InterruptedException {
        ZipOutputStream zos = new ZipOutputStream(os);
        File heapDumpFile = getHeapDumpTempFile();
        try {
            HeapDumper.dumpHeap(heapDumpFile.getAbsolutePath(), false);
            IOUtil.addFileToZip(heapDumpFile, zos, heapDumpFile.getName());
        } finally {
            if (!heapDumpFile.delete())
                LOG.warn("Failed to remove heap dump file '%s'").with(heapDumpFile.getAbsolutePath());
            zos.finish();
        }
    }

    private static File getHeapDumpTempFile() {
        if (!dumpsDir.exists())
            if (!dumpsDir.mkdirs())
                LOG.warn("Failed to create '%s' directory!").with(dumpsDir.getAbsolutePath());

        File heapDumpFile = new File(dumpsDir, getHeapDumpName());
        int n = 0;
        while (heapDumpFile.exists())
            heapDumpFile = new File(dumpsDir,  getHeapDumpName(++n));

        heapDumpFile.deleteOnExit();

        return heapDumpFile;
    }

    private static String getHeapDumpName() {
        return getHeapDumpName(0);
    }

    private static synchronized String getHeapDumpName(int n) {
        return System.getProperty(QuantServiceConfig.QSRV_TYPE_SYS_PROP) + "-" +
               DF.format(new Date()) + (n != 0 ? "(" + n + ")" : "") + ".hprof";
    }
}