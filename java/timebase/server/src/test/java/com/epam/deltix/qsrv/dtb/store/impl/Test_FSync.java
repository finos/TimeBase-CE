package com.epam.deltix.qsrv.dtb.store.impl;

import com.epam.deltix.qsrv.dtb.fs.pub.AbstractFileSystem;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.qsrv.dtb.fs.pub.Constants;
import com.epam.deltix.qsrv.dtb.fs.pub.FSFactory;
import com.epam.deltix.util.io.IOUtil;


import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.Random;

/**
 * Created by Alex Karpovich on 15/07/2020.
 */
public class Test_FSync {

    final static int FILES = 1000;

    public static void main(String[] args) throws IOException {

        String location = args.length == 1 ? args[0] : File.createTempFile("xtemp", Long.toString(System.currentTimeMillis())).getAbsolutePath();

        File file = new File(location);
        if (file.exists())
            IOUtil.deleteFileOrDir(file);

        if (!file.mkdirs())
            throw new IllegalStateException("Cannot create folder: " + location);

        AbstractFileSystem fs = FSFactory.getLocalFS();
        AbstractPath parent = fs.createPath(location);

        Random rnd = new Random(2020);

        byte[] buffer = new byte[1024 * 1024];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte) rnd.nextInt(200);
        }

        long t0 = System.currentTimeMillis();

        for (int i = 0; i < FILES; i++) {
            AbstractPath path = fs.createPath(parent, "z" + i);

            try (OutputStream os = new BufferedOutputStream(path.openOutput (0))) {
                DataOutputStream dos = new DataOutputStream(os);

                dos.write(new byte[] {8, 16, 32, 64});
                dos.write(buffer);
            }

            fsync(Paths.get(location), true);
        }

        long                            t1 = System.currentTimeMillis ();
        double                          s = (t1 - t0) * 0.001;
        double count = FILES;

        System.out.printf (
                "Written %s files in %,.3fs; speed: %,.0f files/s\n",
                count,
                s,
                count / s
        );

    }

    public static void fsync(Path fileToSync, boolean isDir) throws IOException {
        // If the file is a directory we have to open read-only, for regular files we must open r/w for the fsync to have an effect.
        // See http://blog.httrack.com/blog/2013/11/15/everything-you-always-wanted-to-know-about-fsync/

        if (isDir && Constants.WINDOWS) {
            // opening a directory on Windows fails, directories can not be fsynced there
            if (!Files.exists(fileToSync)) {
                // yet do not suppress trying to fsync directories that do not exist
                throw new NoSuchFileException(fileToSync.toString());
            }
            return;
        }

        try (final FileChannel file = FileChannel.open(fileToSync, isDir ? StandardOpenOption.READ : StandardOpenOption.WRITE)) {
            try {
                file.force(true);
            } catch (final IOException e) {
                if (isDir) {
                    assert !(Constants.LINUX || Constants.MAC_OS_X) :
                            "On Linux and MacOSX fsyncing a directory should not throw IOException, " +
                                    "we just don't want to rely on that in production (undocumented). Got: " + e;
                    // Ignore exception if it is a directory
                    return;
                }
                // Throw original exception
                throw e;
            }
        }
    }
}
