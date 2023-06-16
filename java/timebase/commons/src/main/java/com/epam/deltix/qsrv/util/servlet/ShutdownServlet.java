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
package com.epam.deltix.qsrv.util.servlet;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.config.QuantServerExecutor;
import com.epam.deltix.qsrv.hf.security.TimeBasePermissions;
import com.epam.deltix.util.runtime.Shutdown;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;

public class ShutdownServlet extends HttpServlet {
    protected static final Log LOG = LogFactory.getLog(ShutdownServlet.class);
    public static int SHUTDOWN_PARAM = 0;
    public static boolean localhostOnly = false;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        LOG.warn("System shutdown requested by %s from %s").with(req.getRemoteUser()).with(req.getRemoteAddr());

        if (localhostOnly && !InetAddress.getByName(req.getRemoteHost()).isLoopbackAddress()) {
            decline(req, resp);
        } else {
            if (QuantServerExecutor.SC == null || QuantServerExecutor.SC.hasPermission(req.getUserPrincipal(), TimeBasePermissions.SHUTDOWN_PERMISSION)) {
                ServletOutputStream out = resp.getOutputStream();
                out.write(SHUTDOWN_PARAM);
                out.flush();

                Shutdown.asyncExit(0); // should be started async to prevent blocking tomcat thread executor
            } else {
                decline(req, resp);
            }
        }
    }

    private void decline(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LOG.error("System shutdown request from %s (remote address: %s) is declined.").with(req.getUserPrincipal()).with(req.getRemoteAddr());
        resp.sendError(HttpServletResponse.SC_FORBIDDEN, "No permission");
    }
}