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
package com.epam.deltix.qsrv.servlet;

import com.epam.deltix.qsrv.HeapDumpDownloadHelper;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 *
 */
public class HeapDumpServlet extends HttpServlet {

    @Override
    public void         doGet (HttpServletRequest req, final HttpServletResponse resp)
        throws ServletException, IOException
    {
        resp.setContentType ("application/zip");
        resp.setHeader ("Content-Disposition", "attachment; filename=\"" + HeapDumpDownloadHelper.getZippedHeapDumpFilename() + "\"");

        ServletOutputStream os = resp.getOutputStream ();
        try {
            HeapDumpDownloadHelper.dumpAndStore(os);
        } catch (InterruptedException e) {
            throw new ServletException (e);
        }
    }
}