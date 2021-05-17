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

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created after BitMEX API key exposure to disable remote access
 * to monitoring capabilities by default.
 *
 * See http://rm.orientsoft.by/issues/9804
 */
public class AccessFilter implements Filter {

    public static final String ENABLE_REMOTE_ACCESS_PROP = "QuantServer.enableRemoteMonitoring";
    private static final boolean ENABLE_REMOTE_ACCESS = Boolean.getBoolean(ENABLE_REMOTE_ACCESS_PROP);
    private static final Logger LOGGER = Logger.getLogger(AccessFilter.class.getName());

    public void init(FilterConfig config) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if (ENABLE_REMOTE_ACCESS) {
            filterChain.doFilter(request, response);
        } else {
            if (request instanceof HttpServletRequest) {
                if (InetAddress.getByName(request.getRemoteHost()).isLoopbackAddress()) {
                    filterChain.doFilter(request, response);
                } else {
                    if (LOGGER.isLoggable(Level.INFO))
                        LOGGER.info("Preventing remote host " + request.getRemoteHost() + " from accessing " + ((HttpServletRequest) request).getRequestURI());
                    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, "Remote access is forbidden");
                }
            } else {
                filterChain.doFilter(request, response);
            }
        }
    }

}
