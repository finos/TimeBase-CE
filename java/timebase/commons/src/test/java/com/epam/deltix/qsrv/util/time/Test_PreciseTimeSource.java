package com.epam.deltix.qsrv.util.time;

import com.epam.deltix.qsrv.util.time.PreciseTimeSource;
import org.junit.Test;

import java.util.Date;

/**
 * @author Andy
 *         Date: Nov 19, 2010 1:28:11 PM
 */
public class Test_PreciseTimeSource {

    @Test
    public void test1 () throws InterruptedException {
        PreciseTimeSource ts = new PreciseTimeSource ();


        final int N = 10000000;
        long delta [] = new long [N];
        for (int i=0; i < N; i++) {
            long now1 = System.currentTimeMillis();
            long now2 = ts.currentTimeMillis();
            delta[i] = now1 - now2;
        }
        for (int i=0; i < N; i++) {
            if (delta[i] != 0)
                System.out.println("#A#:" + i + " " + delta[i]);
        }

        for (int i=0; i < N; i++) {
            long now1 = System.currentTimeMillis();
            long now2 = ts.currentTimeMillis();
            delta[i] = now1 - now2;
            Thread.sleep(0);
        }

        for (int i=0; i < N; i++) {
            if (delta[i] != 0)
                System.out.println("#B#:" + i + " " + delta[i]);
        }


        System.out.println("Done " + ts);
        while(true) {
            long now1 = System.currentTimeMillis();
            long now2 = ts.currentTimeMillis();
            if (Math.abs(now1 - now2) > 2) {
                System.out.println("#delta at #:" + new Date(now1) + " " + (now1 - now2));
                Thread.sleep(5000);
            }

        }
    }
}
