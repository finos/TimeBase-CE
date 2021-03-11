package com.epam.deltix.qsrv;

import com.epam.deltix.gflog.Log;
import com.epam.deltix.gflog.LogFactory;
import com.epam.deltix.util.io.IOUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipOutputStream;

/**
 * @author Andy
 *         Date: 4/4/11 10:19 AM
 */
public class LogsDownloadHelper {
    private static final Log LOG = LogFactory.getLog(LogsDownloadHelper.class);
    private static final File                       logDir = QSHome.getFile ("logs");
    private static final String              FS = "yyyy-MM-dd-HH-mm-ss";
    private static final SimpleDateFormat DF = new SimpleDateFormat (FS);

    /** Auto-generates suggested file name to store log files */
    public static String getLogFilename() {
        return "logs-at-" + DF.format (new Date()) + ".zip";
    }

    /* Archive QuantServer log files into given output stream */
    public static void store(OutputStream os) throws IOException, InterruptedException {
        ZipOutputStream zos = new ZipOutputStream(os);

        for (File f : logDir.listFiles()) {
            try {
                IOUtil.addFileToZip(f, zos, f.getName());
            } catch (IOException iox) {
                LOG.trace("Failed to add log: %s").with(iox);
            }
        }

        zos.finish();
    }

    public static void store(File output)  throws IOException, InterruptedException {
        FileOutputStream fos = new FileOutputStream(output);
        store (fos);
        fos.close();
    }
}
