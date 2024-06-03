package com.epam.deltix.qsrv.hf.tickdb.web.controller;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.hf.tickdb.http.AbstractHandler;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBMonitor;
import com.epam.deltix.qsrv.util.metrics.MetricsService;
import com.epam.deltix.util.time.TimeKeeper;
import com.epam.deltix.util.vsocket.VSServerFramework;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.PostConstruct;

/**
 *
 */
@Controller
@RequestMapping(value = "/")
public class MetricsController {

    private final static Log LOGGER = LogFactory.getLog(MetricsController.class);

    private final static long MAX_RESOLUTION_MS = 1000;
    private final static String NUM_LOADERS_METRIC = "timebase.num_loaders";
    private final static String NUM_CURSORS_METRIC = "timebase.num_cursors";
    private final static String NUM_CONNECTIONS_METRIC = "timebase.connections.num_connections";
    private final static String TRAFFIC_RATE_METRIC = "timebase.connections.traffic_rate";
    private final static String AVAILABLE_MEMORY_METRIC = "timebase.memory.available";

    private final static String LICENCE_DAYS_VALID_METRIC = "timebase.license.days_valid";
    private final static Runtime RUNTIME = Runtime.getRuntime();

    private long lastRequestTime = TimeKeeper.currentTime;
    private ResponseEntity<String> cachedResponse = ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body("Metrics service is not available");

    public MetricsController() {
    }

    @PostConstruct
    public void init() {
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
        }
    }

    @RequestMapping(value = "/metrics", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> metrics() {
        long currentTime = TimeKeeper.currentTime;
        if (currentTime - lastRequestTime > MAX_RESOLUTION_MS) {
            lastRequestTime = currentTime;
            return cachedResponse = scrape();
        }

        return cachedResponse;
    }

    private ResponseEntity<String> scrape() {
        try {
            if (MetricsService.getInstance().initialized()) {
                return ResponseEntity.ok().body(MetricsService.getInstance().scrape());
            }
        } catch (Throwable t) {
            LOGGER.error().append("Failed to scrape metrics").append(t).commit();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to scrape metrics");
        }

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Metrics service is not available");
    }

}
