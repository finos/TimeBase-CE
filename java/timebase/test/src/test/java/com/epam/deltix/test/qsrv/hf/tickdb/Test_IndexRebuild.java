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
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.test.qsrv.hf.tickdb.schema.Test_SchemaConverter;
import com.epam.deltix.qsrv.testsetup.TickDBCreator;
import com.epam.deltix.test.qsrv.hf.tickdb.server.ServerRunner;
import org.junit.Test;

import java.io.File;
import java.io.FilenameFilter;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

@Category(TickDBFast.class)
public class Test_IndexRebuild {

    @Test
    public void         test() throws Throwable {

        TDBRunner runner = new ServerRunner(false, true);
        runner.startup();

        TickDBCreator.createBarsStream(runner.getTickDb(), TickDBCreator.BARS_STREAM_KEY);
        runner.getTickDb().close();

        File bars = new File(runner.getLocation(), TickDBCreator.BARS_STREAM_KEY);
        String[] files = bars.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("x");
            }
        });

        for (String file : files)
            new File(bars, file).delete();

        runner.getTickDb().open(false);
        TickDBCreator.createBarsStream(runner.getTickDb(), "bars1");

        DXTickStream stream1 = runner.getTickDb().getStream(TickDBCreator.BARS_STREAM_KEY);
        DXTickStream stream2 = runner.getTickDb().getStream("bars1");

        TickCursor cursor1 = stream1.select(Long.MIN_VALUE, null);
        TickCursor cursor2 = stream2.select(Long.MIN_VALUE, null);

        while (cursor1.next()) {
            cursor2.next();
            Test_SchemaConverter.checkEquals(cursor1.getMessage(), cursor2.getMessage());
        }

        cursor1.close();
        cursor2.close();

        runner.shutdown();

    }
}