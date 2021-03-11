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
