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
