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
import com.epam.deltix.util.vfs.ZipFileSystem;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import static junit.framework.Assert.assertEquals;

/**
 *
 */
public class Test_WindowsNativeServiceControl {

    private static ZipFileSystem    testZip;
    private static String           TEST_ZIP_FILE = Home.getPath("testdata/qsrv/WindowsServicesTest.zip");
    private static String           TEST_DIR = Home.getPath("testdata/qsrv/WindowsServicesTest");

    private static ServiceControl   INSTANCE = ServiceControlFactory.getInstance();

    private final String            SERV_TB_1 = "TimebaseTestService1";
    private final String            SERV_TB_1_DESCR = "Timebase test service.";
    private final String            SERV_TB_1_BINPATH = "\"" + TEST_DIR + "/services/TimeBase/TimeBaseTestSvc.exe" + "\" -tb";

    private final String            SERV_AG_1 = "AggregatorTestService1";
    private final String            SERV_AG_1_DESCR = "Aggregator test service.";
    private final String            SERV_AG_1_BINPATH = "\"" + TEST_DIR + "/services/Aggregator/AggregatorTestSvc.exe" + "\" -agg";

    private final String            SERV_UHF_1 = "UHFTestService1";
    private final String            SERV_UHF_1_DESCR = "UHF test service.";
    private final String            SERV_UHF_1_BINPATH = "\"" + TEST_DIR + "/services/UHF/UHFTestSvc.exe" + "\" -uhf";

    @BeforeClass
    public static void              start() throws Throwable {
        //unzip test data
        InputStream is = new FileInputStream(TEST_ZIP_FILE);
        File testDir = new File(TEST_DIR);
        if (testDir.exists())
            FileUtils.deleteDirectory(testDir);
        else
            testDir.mkdir();

        testZip.unzip(is, testDir);
    }

    @AfterClass
    public static void              stop() throws Throwable {
        //clear test data
        File testDir = new File(TEST_DIR);
        if (testDir.exists())
           FileUtils.deleteDirectory(testDir);
    }

    @Test
    public void                     testWindowsServiceControl() {
        testCreateService();
        testGetExecutablePath();
        testStartService();
        testStopService();
        testDeleteService();
    }

    public void                     testCreateService() {
        String id = SERV_TB_1;
        String descr = SERV_TB_1_DESCR;
        String binPath = SERV_TB_1_BINPATH;
        ServiceControl.CreationParameters params = new ServiceControl.CreationParameters();
        params.displayName = SERV_TB_1;
        params.startMode = ServiceControl.StartMode.auto;

        try {
            if (INSTANCE.exists(SERV_TB_1)) {
                INSTANCE.stop(SERV_TB_1);
                INSTANCE.delete(SERV_TB_1);
            }
            assertEquals(INSTANCE.exists(SERV_TB_1), false);
            INSTANCE.create(id, descr, binPath, params);
            assertEquals(INSTANCE.exists(SERV_TB_1), true);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    public void                     testGetExecutablePath() {
        try {
            assert(INSTANCE.getExecutablePath(SERV_TB_1).compareToIgnoreCase(SERV_TB_1_BINPATH) == 0);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void                     testStartService() {
        try {
            assertEquals(INSTANCE.queryStatusName(SERV_TB_1), "STOPPED");
            INSTANCE.start(SERV_TB_1);
            int sleepCount = 0;
            while (!INSTANCE.queryStatusName(SERV_TB_1).equals("RUNNING")) {
                Thread.sleep(500);
                if (++sleepCount > 20)
                    break;
            }
            assertEquals(INSTANCE.queryStatusName(SERV_TB_1), "RUNNING");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    public void                     testStopService() {
        try {
            assertEquals(INSTANCE.queryStatusName(SERV_TB_1), "RUNNING");
            INSTANCE.stop(SERV_TB_1);
            int sleepCount = 0;
            while (!INSTANCE.queryStatusName(SERV_TB_1).equals("STOPPED")) {
                Thread.sleep(500);
                if (++sleepCount > 20)
                    break;
            }
            assertEquals(INSTANCE.queryStatusName(SERV_TB_1), "STOPPED");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    public void                     testDeleteService() {
        try {
            assertEquals(INSTANCE.exists(SERV_TB_1), true);
            INSTANCE.delete(SERV_TB_1);
            assertEquals(INSTANCE.exists(SERV_TB_1), false);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    @Test
    public void                     testDependentWindowsServiceControl() {
        testCreateDependServices();
        testGetExecutablePathOfDependServices();
        testAddDependency();
        testStartAndWaitServices();
        testStopAndWaitServices();
        testDeleteDependServices();
    }

    public void                     testCreateDependServices() {
        //in this test creates 3 services:
        //  timebase
        //  aggregator
        //  uhf depends on timebase and aggregator
        //create timebase service
        String id_tb = SERV_TB_1;
        String descr_tb = SERV_TB_1_DESCR;
        String binPath_tb = SERV_TB_1_BINPATH;
        ServiceControl.CreationParameters params_tb = new ServiceControl.CreationParameters();
        params_tb.displayName = SERV_TB_1;
        params_tb.startMode = ServiceControl.StartMode.auto;

        try {
            if (INSTANCE.exists(SERV_TB_1)) {
                INSTANCE.stop(SERV_TB_1);
                INSTANCE.delete(SERV_TB_1);
            }
            assertEquals(INSTANCE.exists(SERV_TB_1), false);
            INSTANCE.create(id_tb, descr_tb, binPath_tb, params_tb);
            assertEquals(INSTANCE.exists(SERV_TB_1), true);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }

        //create aggregator service
        String id_ag = SERV_AG_1;
        String descr_ag = SERV_AG_1_DESCR;
        String binPath_ag = SERV_AG_1_BINPATH;
        ServiceControl.CreationParameters params_ag = new ServiceControl.CreationParameters();
        params_ag.displayName = SERV_AG_1;
        params_ag.startMode = ServiceControl.StartMode.auto;
        params_ag.dependencies = new String[]{ SERV_TB_1 };

        try {
            if (INSTANCE.exists(SERV_AG_1)) {
                INSTANCE.stop(SERV_AG_1);
                INSTANCE.delete(SERV_AG_1);
            }
            assertEquals(INSTANCE.exists(SERV_AG_1), false);
            INSTANCE.create(id_ag, descr_ag, binPath_ag, params_ag);
            assertEquals(INSTANCE.exists(SERV_AG_1), true);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }

        //create uhf service
        String id_uhf = SERV_UHF_1;
        String descr_uhf = SERV_UHF_1_DESCR;
        String binPath_uhf = SERV_UHF_1_BINPATH;
        ServiceControl.CreationParameters params_uhf = new ServiceControl.CreationParameters();
        params_uhf.displayName = SERV_UHF_1;
        params_uhf.startMode = ServiceControl.StartMode.auto;

        try {
            if (INSTANCE.exists(SERV_UHF_1)) {
                INSTANCE.stop(SERV_UHF_1);
                INSTANCE.delete(SERV_UHF_1);
            }
            assertEquals(INSTANCE.exists(SERV_UHF_1), false);
            INSTANCE.create(id_uhf, descr_uhf, binPath_uhf, params_uhf);
            assertEquals(INSTANCE.exists(SERV_UHF_1), true);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    public void                     testAddDependency() {
        boolean isOk = false;
        try {
            INSTANCE.addDependency(SERV_UHF_1, SERV_AG_1);
            INSTANCE.addDependency(SERV_UHF_1, SERV_TB_1);
            isOk = true;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        assertEquals(isOk, true);
    }

    public void                     testGetExecutablePathOfDependServices() {
        try {
            assert(INSTANCE.getExecutablePath(SERV_TB_1).compareToIgnoreCase(SERV_TB_1_BINPATH) == 0);
            assert(INSTANCE.getExecutablePath(SERV_AG_1).compareToIgnoreCase(SERV_AG_1_BINPATH) == 0);
            assert(INSTANCE.getExecutablePath(SERV_UHF_1).compareToIgnoreCase(SERV_UHF_1_BINPATH) == 0);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void                     testStartAndWaitServices() {
        try {
            assertEquals(INSTANCE.queryStatusName(SERV_TB_1), "STOPPED");
            assertEquals(INSTANCE.queryStatusName(SERV_AG_1), "STOPPED");
            assertEquals(INSTANCE.queryStatusName(SERV_UHF_1), "STOPPED");
            INSTANCE.startAndWait(SERV_UHF_1, false, 15000);
            assertEquals(INSTANCE.queryStatusName(SERV_TB_1), "RUNNING");
            assertEquals(INSTANCE.queryStatusName(SERV_AG_1), "RUNNING");
            assertEquals(INSTANCE.queryStatusName(SERV_UHF_1), "RUNNING");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    public void                     testStopAndWaitServices() {
        try {
            assertEquals(INSTANCE.queryStatusName(SERV_TB_1), "RUNNING");
            assertEquals(INSTANCE.queryStatusName(SERV_AG_1), "RUNNING");
            assertEquals(INSTANCE.queryStatusName(SERV_UHF_1), "RUNNING");
            INSTANCE.stopAndWait(SERV_TB_1, false, 15000);
            assertEquals(INSTANCE.queryStatusName(SERV_TB_1), "STOPPED");
            assertEquals(INSTANCE.queryStatusName(SERV_AG_1), "STOPPED");
            assertEquals(INSTANCE.queryStatusName(SERV_UHF_1), "STOPPED");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    public void                     testDeleteDependServices() {
        try {
            assertEquals(INSTANCE.exists(SERV_TB_1), true);
            assertEquals(INSTANCE.exists(SERV_AG_1), true);
            assertEquals(INSTANCE.exists(SERV_UHF_1), true);
            INSTANCE.delete(SERV_AG_1);
            INSTANCE.delete(SERV_UHF_1);
            INSTANCE.delete(SERV_TB_1);
            assertEquals(INSTANCE.exists(SERV_TB_1), false);
            assertEquals(INSTANCE.exists(SERV_AG_1), false);
            assertEquals(INSTANCE.exists(SERV_UHF_1), false);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    public void                     testErrorHandling() {

    }

}