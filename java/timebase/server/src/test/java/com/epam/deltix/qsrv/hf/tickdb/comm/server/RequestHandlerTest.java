package com.epam.deltix.qsrv.hf.tickdb.comm.server;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Alexei Osipov
 */
public class RequestHandlerTest {
    @Test
    public void isLocal() throws Exception {
        assertTrue(RequestHandler.isLocal("/127.0.0.1:8011"));
        assertTrue(RequestHandler.isLocal("localhost/127.0.0.1:8011"));
        assertTrue(RequestHandler.isLocal("something/127.0.0.1:8011"));

        assertFalse(RequestHandler.isLocal("/128.0.0.1:8011"));
        assertFalse(RequestHandler.isLocal("localhost/128.0.0.1:8011"));
        assertFalse(RequestHandler.isLocal("something/128.0.0.1:8011"));
    }

}