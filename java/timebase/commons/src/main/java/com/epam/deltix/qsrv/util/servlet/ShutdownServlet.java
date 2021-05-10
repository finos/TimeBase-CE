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
