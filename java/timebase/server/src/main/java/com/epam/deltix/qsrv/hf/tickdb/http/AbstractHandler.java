/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb.http;

import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.security.SecurityController;
import org.owasp.encoder.Encode;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.AccessControlException;

public abstract class AbstractHandler  {

    public static DXTickDB                      TDB;
    public static SecurityController            SC;

    public static void          sendError(HttpServletResponse response, Throwable ex) throws IOException {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Encode.forHtml(ex.getClass().getName() + ":" + Util.printStackTrace(ex)));
    }

    public static void          sendForbiddenError(HttpServletResponse response, AccessControlException ex) throws IOException {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, Encode.forHtml(ex.getClass().getName() + ":" + Util.printStackTrace(ex)));
    }

    static void                 validatePermissionUnimplemented() {
//        if (GlobalQuantServer.SC != null)
//            throw new AccessControlException("Current version does not support this functionality in secure mode");
    }

}