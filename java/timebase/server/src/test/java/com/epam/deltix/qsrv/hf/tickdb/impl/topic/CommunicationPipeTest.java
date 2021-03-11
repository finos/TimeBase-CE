package com.epam.deltix.qsrv.hf.tickdb.impl.topic;

import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * @author Alexei Osipov
 */
public class CommunicationPipeTest {

    @Test(timeout = 3_000)
    public void test() throws InterruptedException {
        int dataBlockSize = 677;
        int dataBlocks = 100;
        int bufferSize = 2 * 1024;
        //noinspection ConstantConditions
        assert bufferSize > dataBlockSize;

        CommunicationPipe p = new CommunicationPipe(bufferSize);
        InputStream in = p.getInputStream();
        OutputStream out = p.getOutputStream();

        byte[] sourceBytes = new byte[dataBlockSize];
        new Random().nextBytes(sourceBytes);

        CountDownLatch completed = new CountDownLatch(2);
        new Thread(() -> {
            for (int i = 0; i < dataBlocks; i++) {
                try {
                    out.write(sourceBytes);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                completed.countDown();
            }
        }).start();

        new Thread(() -> {
            byte[] readBytes = new byte[dataBlockSize];
            try {
                while (in.available() > dataBlockSize) {
                    int read = in.read(readBytes);
                    Assert.assertEquals(dataBlockSize, read);
                    Assert.assertArrayEquals(sourceBytes, readBytes);
                }
            }catch (Exception e) {
                e.printStackTrace();
                return;
            }
            completed.countDown();

        }).start();

        completed.await();
    }
}