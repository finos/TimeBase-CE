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
package com.epam.deltix.util.service;

import com.epam.deltix.util.jni.WindowsService;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public abstract class BaseService extends WindowsService {

    public static final int ERROR_CODE = 1067;
    private static java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("deltix.util.service");

    private List<AdditionalTimeRequestListener> listeners = new ArrayList<AdditionalTimeRequestListener>();
    private final Object mWaitForStop = new Object();
    private boolean stopped = true;

    protected final String serviceName;

    protected BaseService() {
        this.serviceName = null;
    }

    protected BaseService(String serviceName) {
        this.serviceName = serviceName;
    }

    public void run() {
        run(serviceName);
    }

    protected int getMask() {
        return SERVICE_ACCEPT_STOP | SERVICE_ACCEPT_SHUTDOWN;
    }

    protected void runService() {

        // thread created outside of JVM, so set ContextClassLoader
        if (Thread.currentThread().getContextClassLoader() == null)
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        try {
            stopped = false;
            requestAdditionalTime(10000);
            setStatus(SERVICE_START_PENDING);
            reportStatus();

            onStart();

            setAcceptCtrlMask(getMask());
            setStatus(SERVICE_RUNNING);
            reportStatus();

        } catch (Throwable x) {
            onError(x, "onStart()");
            stop(ERROR_CODE); // The process terminated unexpectedly
        }

        synchronized (mWaitForStop) {
            while (!stopped) {
                try {
                    mWaitForStop.wait();
                } catch (InterruptedException x) {
                    getLogger().log(Level.SEVERE, "Service thread was interrupted", x);
                }
            }
        }

        if (!stopped) {
            getLogger().info("Service thread received signal to stop ...");
            stop(0);
        }
        getLogger().info("Service thread stopped ...");
    }

    public void         onError (Throwable error, String function) {
        getLogger().log(Level.SEVERE, "Failed to execute " + function + ": " + error, error);
    }

    /**
     * Stops the service with given error code
     * @param errorCode
     */
    private void        stop(int errorCode) {
        try {
            setStatus(SERVICE_STOP_PENDING);
            reportStatus();

//            if ((errorCode == 0 || errorCode == ERROR_CODE) && WatchdogService.INSTANCE.running())
//                WatchdogService.INSTANCE.unregister(serviceName);

            try {
                onStop();
            } catch (Throwable ex) {
                onError(ex, "onStop()");
            }

            setErrorCode(errorCode);

            stopDispatchThread();

            setStatus(SERVICE_STOPPED);
            reportStatus();
            getLogger().info("SERVICE_STOPPED");

        } catch (Throwable x) {
            getLogger().log(Level.SEVERE, "Failed to stop: " + x, x);
        }
    }

    public java.util.logging.Logger getLogger() {
        return LOGGER;
    }

    public void control(int command) {
        control(command, 0);
    }

    public void control(int command, int controlCode) {
        try {
            switch (command) {
                case SERVICE_CONTROL_CONTINUE:
                    getLogger().info("Received SERVICE_CONTROL_CONTINUE ...");
                    setStatus(SERVICE_CONTINUE_PENDING);
                    reportStatus();

                    onStart();

                    setStatus(SERVICE_RUNNING);
                    reportStatus();
                    getLogger().info("Now SERVICE_RUNNING");
                    break;

                case SERVICE_CONTROL_PAUSE:
                    getLogger().info("Received SERVICE_CONTROL_PAUSE ...");
                    setStatus(SERVICE_PAUSE_PENDING);
                    reportStatus();

                    onPause();

                    setStatus(SERVICE_PAUSED);
                    reportStatus();
                    getLogger().info("Now SERVICE_PAUSED");
                    break;

                case SERVICE_CONTROL_STOP:
                    getLogger().info("Received SERVICE_CONTROL_STOP ...");

                    stop(controlCode);

                    getLogger().info("Control thread sent the signal to stop ...");
                    break;

                case SERVICE_CONTROL_SHUTDOWN:
                    getLogger().info("Received SERVICE_CONTROL_SHUTDOWN ...");

                    stop(controlCode);

                    getLogger().info("Control thread sent the signal to shutdown ...");
                    break;
                default:
                    onCustomCommand(command);

            }
        } catch (Throwable x) {
            getLogger().log(Level.SEVERE, "Failure in control(): " + x, x);
        }
    }

    private void stopDispatchThread() {
        synchronized (mWaitForStop) {
            stopped = true;
            mWaitForStop.notify();
        }
    }

    public void addListener(AdditionalTimeRequestListener listener) {
        listeners.add(listener);
    }

    public void removeListener(AdditionalTimeRequestListener listener) {
        listeners.remove(listener);
    }

    public void requestAdditionalTime(int ms) {
        for (AdditionalTimeRequestListener listener : listeners) {
            listener.onAdditionalTimeRequested(ms);
        }

        // call setWaitHint if we are running service thread only
        if (!stopped)
            setWaitHint(ms);
    }

    /**
     * When implemented in a derived class, executes when a Start command is sent to
     * the service by the Service Control Manager (SCM) or when the operating system starts
     * (for a service that starts automatically). Specifies actions to take when the service starts.
     * @throws Throwable
     */
    public abstract void onStart() throws Throwable;

    /**
     * When implemented in a derived class, executes when a Stop command is sent to the service by the Service Control Manager (SCM). 
     * Specifies actions to take when a service stops running.
     * @throws Throwable
     */
    public abstract void onStop() throws Throwable;

    /**
     * When implemented in a derived class, executes when a Pause command is sent to the service by the Service Control Manager (SCM).
     * Specifies actions to take when a service pauses.
     * @throws Throwable
     */
    public abstract void onPause() throws Throwable;

    /**
     * When implemented in a derived class, OnCustomCommand(int) executes when the Service Control Manager (SCM)
     * passes a custom command to the service.
     * Specifies actions to take when a command with the specified parameter value occurs.
     * @param command The command message sent to the service (see WindowsService.SERVICE_CONTROL_*)
     * @throws Exception
     */
    public void onCustomCommand(int command) throws Exception{
    }
}
