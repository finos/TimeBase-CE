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
package com.epam.deltix.util.os;

import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.time.GMT;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 *
 */
public class WindowsNativeServiceControl extends ServiceControl {

    public static final WindowsNativeServiceControl INSTANCE = new WindowsNativeServiceControl();

    private static int                              ERROR_CODE = 1067;

    static {
        System.load(Home.getFile("bin").getAbsolutePath() + "/windsc" + System.getProperty("os.arch") + ".dll");
    }

    @Override
    public native void                 start(String id)
        throws IOException, InterruptedException;

    @Override
    public native void                 stop(String id)
        throws IOException, InterruptedException;

    @Override
    public native void                 delete(String id)
        throws IOException, InterruptedException;

    @Override
    public native void                 addDependency(String id, String dependId)
        throws IOException, InterruptedException;

    @Override
    public native void                 create(
        String                              id,
        String                              description,
        String                              binPath,
        CreationParameters                  params
    )
        throws IOException, InterruptedException;

    @Override
    public native String               queryStatusName(String id)
        throws IOException, InterruptedException;

    public String                      queryStatusNameNoErrors (String id) {
        try {
            return (queryStatusName(id));
        } catch (Throwable x) {
            LOG.warn("Query failed - ignoring: %s").with(x);
            return ("ERROR: " + x.toString());
        }
    }

    @Override
    public native String               getExecutablePath(String id)
        throws InvocationTargetException, IllegalAccessException;

    @Override
    public native boolean              exists(String id)
        throws IOException, InterruptedException;

    public static native String       queryExitCode(String id)
        throws IOException, InterruptedException;

    public static native String       queryServiceExitCode(String id)
        throws IOException, InterruptedException;

    @Override
    public native void                setFailureAction(String id, FailureAction action, int delay, String command)
        throws IOException, InterruptedException;

    @Override
    public void                        startAndWait (String id, boolean ignoreQueryErrors, long timeout)
        throws IOException, InterruptedException
    {
        long        limit = System.currentTimeMillis() + timeout;

        for (;;) {
            String  status = ignoreQueryErrors ? queryStatusNameNoErrors (id) : queryStatusName (id);

            if (status.equals(STATUS_RUNNING))
                break;

            if (status.equals(STATUS_STOPPED)) {
                // on error we should if service fails with predefined error
                String exitCode = queryExitCode(id);

                if (StringUtils.equals(String.valueOf(ERROR_CODE), exitCode))
                    throw new RuntimeException ("Service failed to start (service exit code: " + exitCode + ")");
                else
                    start(id);
            }

            if (System.currentTimeMillis () >= limit)
                throw new InterruptedException ("Service failed to start after period of time (" + GMT.formatTime(limit) + ")."
                    + "Exit code = " + queryExitCode(id));

            Thread.sleep(500);
        }
    }

    @Override
    public void                        stopAndWait (String id, boolean ignoreQueryErrors, long timeout)
        throws IOException, InterruptedException
    {
        long        limit = System.currentTimeMillis() + timeout;

        for (;;) {
            String  status =
                ignoreQueryErrors ? queryStatusNameNoErrors(id) : queryStatusName(id);

            if (status.equals(STATUS_STOPPED))
                break;

            if (status.equals(STATUS_RUNNING))
                stop(id);

            if (System.currentTimeMillis() >= limit)
                throw new InterruptedException();

            Thread.sleep(500);
        }
    }
}
