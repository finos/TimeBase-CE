package com.epam.deltix.qsrv.util.tomcat;

import com.epam.deltix.util.jni.WindowsService;

import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class BaseTomcatService extends WindowsService {
    
    protected static Logger         LOGGER = Logger.getLogger ("deltix.util.tomcat");

    protected final DXTomcat mCat;
    protected final String          mServiceName;
    private final Object            mWaitForStop = new Object ();
    private boolean                 mShouldStop = false;
    
    public BaseTomcatService (String dir, String serviceName) {
        super();
        mCat = new DXTomcat(dir);
        mServiceName = serviceName;
    }

    protected void run () {
        run(mServiceName);
    }

    @Override
    protected void runService() {
        try {
            setWaitHint(10000);
            setStatus(SERVICE_START_PENDING);
            reportStatus();

//            mCat.init();
//
//            setStatus(SERVICE_START_PENDING);
//            reportStatus();

            mCat.init();
            mCat.start();

            setAcceptCtrlMask(SERVICE_ACCEPT_PAUSE_CONTINUE | SERVICE_ACCEPT_STOP);
            setStatus(SERVICE_RUNNING);
            reportStatus();
            LOGGER.info("Now SERVICE_RUNNING");
        } catch (Throwable x) {
            LOGGER.log(Level.SEVERE, "Failed to start Tomcat: " + x, x);

            setErrorCode(1);
            setStatus(SERVICE_STOPPED);
            reportStatus();
        }

        synchronized (mWaitForStop) {
            while (!mShouldStop) {
                try {
                    mWaitForStop.wait();
                } catch (InterruptedException x) {
                    LOGGER.log(Level.SEVERE, "Service thread was interrupted", x);
                }
            }
        }

        LOGGER.info("Service thread received signal to stop ...");

        try {
            mCat.stop();
            setStatus(SERVICE_STOPPED);
            reportStatus();
            LOGGER.info("Now SERVICE_STOPPED");
        } catch (Throwable x) {
            LOGGER.log(Level.SEVERE, "Failed to stop Tomcat: " + x, x);
        }
    }
    
    private void            stopDispatchThread () {
        synchronized (mWaitForStop) {
            mShouldStop = true;
            mWaitForStop.notify ();
        }
    }

    public void             control (int command) {
        try {
            switch (command) {
                case SERVICE_CONTROL_CONTINUE:
                    LOGGER.info ("Received SERVICE_CONTROL_CONTINUE ...");
                    setStatus (SERVICE_CONTINUE_PENDING);
                    reportStatus ();
                    mCat.init();
                    mCat.start ();
                    setStatus (SERVICE_RUNNING);
                    reportStatus ();
                    LOGGER.info ("Now SERVICE_RUNNING");
                    break;

                case SERVICE_CONTROL_PAUSE:
                    LOGGER.info ("Received SERVICE_CONTROL_PAUSE ...");
                    setStatus (SERVICE_PAUSE_PENDING);
                    reportStatus ();
                    mCat.stop ();
                    setStatus (SERVICE_PAUSED);
                    reportStatus ();
                    LOGGER.info ("Now SERVICE_PAUSED");
                    break;

                case SERVICE_CONTROL_STOP:
                    LOGGER.info ("Received SERVICE_CONTROL_STOP ...");
                    setStatus (SERVICE_STOP_PENDING);
                    reportStatus ();
                    stopDispatchThread ();
                    LOGGER.info ("Control thread sent the signal to stop ...");
                    break;
            }
        } catch (Throwable x) {
            LOGGER.log (Level.SEVERE, "Failure in control(): " + x, x);
        }
    }

}
