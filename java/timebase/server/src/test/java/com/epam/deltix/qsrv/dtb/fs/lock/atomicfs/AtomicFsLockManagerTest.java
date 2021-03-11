package com.epam.deltix.qsrv.dtb.fs.lock.atomicfs;

import com.epam.deltix.qsrv.dtb.fs.azure2.Azure2FS;
import com.epam.deltix.qsrv.dtb.fs.azure2.Azure2PathImpl;
import com.epam.deltix.qsrv.dtb.fs.local.LocalFS;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractFileSystem;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author Alexei Osipov
 */
public class AtomicFsLockManagerTest {
    String clientId = "4f5db66f-0f42-4233-9477-df58b08b7e44";
    String secret = "G8KZhnhwfUWu/B8Zxwey18jvBgqpisihzoVq+gftl90=";
    String fullAccount = "deltixlake.azuredatalakestore.net";
    String authTokenEndpoint = "https://login.microsoftonline.com/f87ea646-b858-4dd3-aedc-bee803a4bdf7/oauth2/token";

    private static final Charset CHARSET = StandardCharsets.ISO_8859_1;

    @Test
    public void testLockOnAzureFs() throws Exception {

        Azure2FS fs = Azure2FS.create(clientId, secret, fullAccount, authTokenEndpoint, 1024L * 8, 10);
        runTestOnFs(fs);
    }

    @Test
    public void testLockOnLocalFs() throws Exception {
        LocalFS fs = new LocalFS();
        runTestOnFs(fs);
    }

    private void runTestOnFs(AbstractFileSystem fs) throws IOException, InterruptedException {


        AbstractPath parentPath = fs.createPath("tmp/alexei_oispov/tests/locks");
        parentPath.makeFolderRecursive();

        AbstractPath dataFile = parentPath.append("test.txt");
        dataFile.deleteIfExists();

        AbstractPath lockFile = parentPath.append("test.txt.lock");

        Thread t1 = new Thread(new Writer("A", 10, clonePath(dataFile), clonePath(lockFile)));
        Thread t2 = new Thread(new Writer("B", 10, clonePath(dataFile), clonePath(lockFile)));
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        String out = getFileContent(dataFile);
        System.out.println(out);
    }

    private static AbstractPath clonePath(AbstractPath path) {
        return path.getFileSystem().createPath(path.getPathString());
    }

    private static class Writer implements Runnable {

        private final String tag;
        private final int iterations;
        private final AbstractPath dataFile;
        private final AbstractPath lockFile;

        public Writer(String tag, int iterations, AbstractPath dataFile, AbstractPath lockFile) {
            this.tag = tag;
            this.iterations = iterations;
            this.dataFile = dataFile;
            this.lockFile = lockFile;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < iterations; i++) {
                    FsLock lock = AtomicFsLockManager.acquire(lockFile);
                    System.out.println(tag + " got lock");
                    if (!exists(dataFile)) {
                        try {
                            writeFileContent(dataFile, "CREATED\n");
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }

                    String fileContent = getFileContent(dataFile);
                    String updatedFileContent = fileContent + tag + " " + i + "\n";
                    try {
                        dataFile.deleteExisting();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    try {
                        writeFileContent(dataFile, updatedFileContent);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }


                    System.out.println(tag + " going to release lock");
                    AtomicFsLockManager.release(lock);
                    System.out.println(tag + " released lock");
                }
            } catch (AtomicFsLockManager.LockExpiredException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }

    private static boolean exists(AbstractPath path) {
        if (path instanceof Azure2PathImpl) {
            // Azure2PathImpl caches exists status. We have to workaround that.
            return ((Azure2PathImpl) path).existsIgnoreCached();
        } else {
            return path.exists();
        }
    }

    @Nonnull
    private static String getFileContent(AbstractPath path) {
        try (InputStream inputStream = path.openInput(0)) {
            return IOUtils.toString(inputStream, CHARSET);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeFileContent(AbstractPath path, String data) throws IOException {
        try (OutputStream os = path.openOutput(0)) {
            java.io.Writer w = new OutputStreamWriter(os, CHARSET);
            w.append(data);
            w.flush();
        }
    }
}