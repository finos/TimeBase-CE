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