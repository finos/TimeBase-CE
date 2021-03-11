package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.tickdb.comm.client.TickDBClient;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.testframework.TBLightweight;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.io.UncheckedIOException;
import com.epam.deltix.util.time.TimeKeeper;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

/**
 *
 */
@Category(TickDBFast.class)
public class Test_TBDeath {
    private final String              HOST = "localhost";
    private final int                 PORT = 7788;
    private static final String       PREFIX = "####### >> ";

    private TBLightweight.TBProcess                 process;

    @Before
    public void                 setUp() throws Throwable {
        String workFolder =
            System.getProperty(
                "test.tbdeath.workFolder",
                Home.getPath("temp/tbdeath"));
        IOUtil.mkDirIfNeeded(new File(workFolder));

        String tbFolder =
            System.getProperty(
                "test.tbdeath.tbFolder",
                workFolder + "/tickdb");

        process = new TBLightweight.TBProcess(workFolder, tbFolder, PORT);
    }

    @After
    public void                 tearDown() throws Throwable {

    }

    public abstract class Stoppable implements Runnable {
        protected boolean       stopped = false;

        public void             stop() {
            stopped = true;
        }

        public void             run() {
            while (!stopped) {
                try {
                    runInternal();
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public abstract void runInternal();
    }

    public class ClientConnector extends Stoppable {
        TickDBClient                client = null;
        private int                 rndMin = 2000;
        private int                 rndMax = 2001;
        Random                      random = new Random();

        private long                startCycleTime = TimeKeeper.currentTime;

        public boolean              isHung() {
            log(TimeKeeper.currentTime - startCycleTime);
            return (TimeKeeper.currentTime - startCycleTime) > 10000;
        }

        public void                 runInternal() {
            try {
                startCycleTime = TimeKeeper.currentTime;
                openClient();
                Thread.sleep(random.nextInt(rndMax - rndMin) + rndMin);
                closeClient();
                Thread.sleep(random.nextInt(rndMax - rndMin) + rndMin);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Throwable t) {
                log(t.getMessage());
            } finally {
                closeClient();
            }
        }

        public void                 openClient() {
            try {
                log("Opening client...");
                client = (TickDBClient) TickDBFactory.connect(HOST, PORT, false);
                client.open(false);
                log("OK");
            } catch (UncheckedIOException ioe) {
                log(ioe.getMessage());
            }
        }

        public void                 closeClient() {
            try {
                if (client != null) {
                    log("Closing client...");
                    client.close();
                    log("OK");
                }
            } catch (UncheckedIOException ioe) {
                log(ioe.getMessage());
            }
        }
    }

    public class ProcessKiller extends Stoppable {
        private int                 rndMin = 2000;
        private int                 rndMax = 5000;
        Random                      random = new Random();

        public ProcessKiller() {
        }

        public void                 runInternal() {
            try {
                process.start();
                Thread.sleep(random.nextInt(rndMax - rndMin) + rndMin);
                process.stop();
                Thread.sleep(random.nextInt(rndMax - rndMin) + rndMin);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                process.stop();
            }
        }
    }

    @Test
    public void                 testPingDeadlock() {
        try {
            //client
            ClientConnector clientConnector = new ClientConnector();
            Thread thClient = new Thread(clientConnector);
            thClient.start();

            //emulate multiple disconnections
            ProcessKiller processKiller = new ProcessKiller();
            Thread thKiller = new Thread(processKiller);
            thKiller.start();

            Thread.sleep(30000);

            clientConnector.stop();
            processKiller.stop();

            assert !clientConnector.isHung();

            thClient.join();
            thKiller.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            assert false;
        }
    }

    public static void          log(Object ... args) {
        synchronized (System.out) {
            System.out.print(PREFIX);

            for (Object arg : args)
                System.out.print (arg);

            System.out.println ();
        }
    }

}
