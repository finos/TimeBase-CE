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
package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.dtb.store.pub.EntityFilter;
import com.epam.deltix.qsrv.dtb.store.pub.TSRoot;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

/**
 * @author Alexei Osipov
 */
public class TickCursorImplTest {

    /**
     * This test ensures that exception from message source is properly propagated to {@link TickCursorImpl} client.
     */
    @Test(expected=TestRuntimeException.class)
    public void testExceptionInMessageSource() throws Exception {
        TickCursorImpl cursor = new TickCursorImpl(null, new SelectionOptions());
        cursor.subscribeToAllEntities();
        PDStream pdStream = new PDStream() {
            @Override
            protected void lockStream(boolean readOnly) {
            }

            @Override
            protected void onOpen(boolean verify) throws IOException {
            }

            @Override
            protected void invalidateUniqueContainer() {
            }

            @Override
            public PDStreamReader createReader(TSRoot root, long time, SelectionOptions options, EntityFilter filter) {
                PDStreamReader mock = Mockito.mock(PDStreamReader.class);
                Mockito.doThrow(new TestRuntimeException()).when(mock).next();
                return mock;
            }
        };
        pdStream.open(true);
        cursor.addStream(pdStream);
        cursor.reset(0);
        cursor.next();
        //Assert.fail();
    }

    private static class TestRuntimeException extends RuntimeException {
        public TestRuntimeException() {
            super("Test exception");
        }
    }
}