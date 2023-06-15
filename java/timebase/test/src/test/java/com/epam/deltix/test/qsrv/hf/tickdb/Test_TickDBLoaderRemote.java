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
package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import org.junit.Before;
import org.junit.After;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TickDBServer;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.TickDBClient;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

/**
 * User: BazylevD
 * Date: Apr 21, 2009
 * Time: 2:47:00 PM
 */
@Category(TickDBFast.class)
public class Test_TickDBLoaderRemote extends Test_TickDBLoader {
    private DXTickDB localDB;
    private TickDBServer server;

    @Before
    @Override
    public void         setUp () {
        localDB = TickDBFactory.create(TDBRunner.getTemporaryLocation());
        localDB.format ();

        // start server
        server = new TickDBServer(0, localDB);
        server.start();

        db = new TickDBClient("localhost", server.getPort ());
        db.open(false);
    }

    @After
    @Override
    public void         tearDown () {
        db.close();

        try {
            server.shutdown (true);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        localDB.close();
        //localDB.delete ();
    }
}