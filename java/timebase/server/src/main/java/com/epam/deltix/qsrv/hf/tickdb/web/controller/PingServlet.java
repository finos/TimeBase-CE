package com.epam.deltix.qsrv.hf.tickdb.web.controller;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.hf.tickdb.http.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class PingServlet extends HttpServlet {
    protected static final Log LOG = LogFactory.getLog(PingServlet.class);

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        try {
            if (AbstractHandler.TDB.isOpen()) {
                resp.setContentType("text/plain");
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().println("TimeBase is ready.");
                resp.getWriter().flush();
                return;
            }
        } catch (Exception ignored) {
        }

        resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "TimeBase is not ready.");
    }
}
