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