package com.epam.deltix.qsrv.hf.tickdb.web.controller;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.hf.tickdb.http.AbstractHandler;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBMonitor;
import com.epam.deltix.qsrv.util.metrics.MetricsService;
import com.epam.deltix.util.time.TimeKeeper;
import com.epam.deltix.util.vsocket.VSServerFramework;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class MetricsServlet extends HttpServlet {
    protected static final Log LOG = LogFactory.getLog(MetricsServlet.class);

    private final static long MAX_RESOLUTION_MS = 1000;
    private final static String NUM_LOADERS_METRIC = "timebase.num_loaders";
    private final static String NUM_CURSORS_METRIC = "timebase.num_cursors";
    private final static String NUM_CONNECTIONS_METRIC = "timebase.connections.num_connections";
    private final static String TRAFFIC_RATE_METRIC = "timebase.connections.traffic_rate";
    private final static String AVAILABLE_MEMORY_METRIC = "timebase.memory.available";

//    private final static String LICENCE_DAYS_VALID_METRIC = "timebase.license.days_valid";
    private final static Runtime RUNTIME = Runtime.getRuntime();

    private volatile long lastRequestTime = TimeKeeper.currentTime;

    private volatile String cachedResponse = "Metrics service is not available";

    public MetricsServlet() {
        if (MetricsService.getInstance().initialized()) {
            if (AbstractHandler.TDB instanceof TBMonitor) {
                MetricsService.getInstance().registerGauge(NUM_LOADERS_METRIC,
                    (TBMonitor) AbstractHandler.TDB, TBMonitor::loadersCount
                );
                MetricsService.getInstance().registerGauge(NUM_CURSORS_METRIC,
                    (TBMonitor) AbstractHandler.TDB, TBMonitor::cursorsCount
                );
            }

            MetricsService.getInstance().registerGauge(NUM_CONNECTIONS_METRIC,
                VSServerFramework.INSTANCE, VSServerFramework::getDispatchersCount
            );
            MetricsService.getInstance().registerGauge(TRAFFIC_RATE_METRIC,
                VSServerFramework.INSTANCE, VSServerFramework::getThroughput
            );
            MetricsService.getInstance().registerGauge(AVAILABLE_MEMORY_METRIC,
                RUNTIME, (r) -> r.maxMemory() - r.totalMemory() + r.freeMemory()
            );

//            MetricsService.getInstance().registerGauge(LICENCE_DAYS_VALID_METRIC,
//                LicenseController.qs(), QSLicenseController::getDaysValid
//            );
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        if (MetricsService.getInstance().initialized()) {
            String response = cachedResponse;
            long currentTime = TimeKeeper.currentTime;
            if (currentTime - lastRequestTime > MAX_RESOLUTION_MS) {
                lastRequestTime = currentTime;
                try {
                    cachedResponse = response = MetricsService.getInstance().scrape();
                } catch (Throwable t) {
                    LOG.error().append("Failed to scrape metrics").append(t).commit();
                    resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Failed to scrape metrics");
                    return;
                }
            }

            try {
                resp.setContentType("text/plain");
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().println(response);
                resp.getWriter().flush();
                return;
            } catch (Exception ignored) {
            }
        }

        resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Metrics service is not available");
    }

}
