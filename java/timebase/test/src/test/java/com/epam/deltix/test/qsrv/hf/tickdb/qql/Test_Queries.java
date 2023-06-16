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
package com.epam.deltix.test.qsrv.hf.tickdb.qql;

import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.TickDBShell;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.lang.Util;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.*;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBQQL;

/**
 *
 */
@Category(TickDBQQL.class)
public class Test_Queries {

    public void test(String path) throws IOException, InterruptedException {
        TickDBShell shell = new TickDBShell();
        try {
            shell.doExec(path);
            assertEquals(0, shell.getErrorCode());
        } finally {
            Util.close(shell.dbmgr.getDB());
        }
    }

    @BeforeClass
    public static void prepareHome() {
        Home.getFile("testdata/tickdb/qqltest").mkdirs();
    }

    @Test
    public void testSelect() throws Exception {
        test("${home}/java/timebase/test/src/test/resources/qql/select/*.q.txt");
    }

    @Test
    public void testDDL() throws Exception {
        test("${home}/java/timebase/test/src/test/resources/qql/ddl/*.q.txt");
    }

    @Test
    public void testComputations() throws Exception {
        test("${home}/java/timebase/test/src/test/resources/qql/computations/*.q.txt");
    }

//    @Test()
//    public void             testSingle () throws Exception {
//        TickDBShell             shell = new TickDBShell ();
//
//        try {
//            shell.doExec ("${home}/java/timebase/client/src/test/java/qql/select/000-prep.q.txt");
//            shell.doExec ("${home}/java/timebase/client/src/test/java/qql/select/102-names.q.txt");
//            assertEquals (0, shell.getErrorCode ());
//        } finally {
//            Util.close (shell.dbmgr.getDB ());
//        }
//    }
}