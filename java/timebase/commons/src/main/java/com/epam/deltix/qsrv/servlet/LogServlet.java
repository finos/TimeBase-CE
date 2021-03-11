package com.epam.deltix.qsrv.servlet;

import com.epam.deltix.qsrv.LogsDownloadHelper;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 *  Returns logs in a zipped format
 */
public class LogServlet extends HttpServlet {

    @Override
    public void         doGet (HttpServletRequest req, final HttpServletResponse resp) 
        throws ServletException, IOException 
    {
        resp.setContentType ("application/zip");
        resp.setHeader ("Content-Disposition", "attachment; filename=\"" + LogsDownloadHelper.getLogFilename() + "\"");

        ServletOutputStream     os = resp.getOutputStream ();
        try {
            LogsDownloadHelper.store(os);
        } catch (InterruptedException ix) {
            throw new ServletException (ix);
        }
    }
}
