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

import com.epam.deltix.qsrv.hf.pub.ChannelQualityOfService;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.test.qsrv.hf.tickdb.testframework.TestAllTypesStreamCreator;
import com.epam.deltix.util.io.Home;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBQQL;

/**
 * 
 */
@Category(TickDBQQL.class)
public class Test_AllTypes {
    private static final String     PATH = "temp/test/qdb";
    
    private static DXTickDB    db;

    @BeforeClass
    public static void      start () throws Throwable {
        File        f = Home.getFile (PATH);
        
        f.mkdirs ();
        
        db = TickDBFactory.create (f);
                
        db.format ();
    }

    @AfterClass
    public static void      stop () throws Throwable {
        db.close ();
    }

    public void             createAndCheckAllTypesStream (ChannelQualityOfService qos) {
        TestAllTypesStreamCreator c = new TestAllTypesStreamCreator (db);

        c.createStream ();        
        c.loadTestData (qos);
        c.verifyTestData (qos); 
    } 

    @Test
    public void             createAndCheckCompiled () {
        createAndCheckAllTypesStream (ChannelQualityOfService.MAX_THROUGHPUT);
    }

    @Test
    public void             createAndCheckIntp () {
        createAndCheckAllTypesStream (ChannelQualityOfService.MIN_INIT_TIME);
    }    
}