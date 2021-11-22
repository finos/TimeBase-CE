package com.epam.deltix.qsrv.jetty;

import io.jooby.Router;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class JettyHandler extends AbstractHandler {
    private final Router router;
    private final boolean defaultHeaders;
    private final int bufferSize;
    private final long maxRequestSize;

    public JettyHandler(Router router, int bufferSize, long maxRequestSize, boolean defaultHeaders) {
        this.router = router;
        this.bufferSize = bufferSize;
        this.maxRequestSize = maxRequestSize;
        this.defaultHeaders = defaultHeaders;
    }

    @Override public void handle(String target, Request request, HttpServletRequest servletRequest,
                                 HttpServletResponse response) {
        request.setHandled(true);
        response.setContentType("text/plain");
        if (defaultHeaders) {
            response.setHeader(HttpHeader.SERVER.asString(), "J");
        }
        JettyContext context = new JettyContext(request, router, bufferSize, maxRequestSize);
        router.match(context).execute(context);
    }
}