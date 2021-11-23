package com.epam.deltix.qsrv.hf.tickdb.web;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.gflog.api.LogLevel;
import com.epam.deltix.qsrv.hf.tickdb.web.model.monitor.LoggerDto;
import com.epam.deltix.qsrv.hf.tickdb.web.model.monitor.SystemJvmStatusDto;
import com.epam.deltix.qsrv.hf.tickdb.web.model.monitor.ThreadDto;
import com.epam.deltix.util.lang.Util;
import io.jooby.annotations.FormParam;
import io.jooby.annotations.GET;
import io.jooby.annotations.POST;
import io.jooby.annotations.Path;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.stream.Collectors;

@Path("/system")
public class SystemController {

    @GET(path = "/jvm/status")
    public SystemJvmStatusDto jvmStatus() {
        Runtime rt = Runtime.getRuntime();
        return new SystemJvmStatusDto(
            rt.availableProcessors(), rt.maxMemory(), rt.freeMemory(), rt.totalMemory(),
            ManagementFactory.getRuntimeMXBean().getInputArguments()
        );
    }

    @GET(path = "/jvm/properties")
    public Map<String, String> jvmProperties() {
        Properties properties = System.getProperties();
        TreeMap<String, String> result = new TreeMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.toLowerCase().contains("pass"))
                continue;

            result.put(key, properties.getProperty(key));
        }

        return result;
    }

    @GET(path = "/threads")
    public List<ThreadDto> threads() {
        List<ThreadDto> threads = new ArrayList<>();
        Util.getAllStackTraces().forEach((t, i) -> {
            threads.add(
                new ThreadDto(
                    t.getId(),
                    t.getName(),
                    t.getState(),
                    Util.getThreadStackTrace(t, i)
                )
            );
        });

        return threads;
    }

    @POST(path = "/threads/interrupt")
    public void interruptThread(@FormParam long id) {
        Util.getAllStackTraces().forEach((t, i) -> {
            if (t.getId() == id) {
                t.interrupt();
            }
        });
    }

    @GET(path = "/loggers")
    public List<LoggerDto> loggers() {
        return LogFactory.getLogs().stream()
            .map(l -> new LoggerDto(l.getName(), l.getLevel()))
            .sorted()
            .collect(Collectors.toList());
    }

    @POST(path = "/loggers/setLevels")
    public void setLogLevels(List<LoggerDto> loggers) {
        Map<String, Log> existingLoggers = LogFactory.getLogs().stream().collect(Collectors.toMap(Log::getName, l -> l));
        loggers.forEach(l -> {
            Log existingLog = existingLoggers.get(l.getName());
            if (existingLog != null && existingLog.getLevel() != l.getLevel()) {
                existingLog.setLevel(l.getLevel());
            }
        });
    }

    @GET(path = "/logLevels")
    public List<String> logLevels() {
        return Arrays.stream(LogLevel.values())
            .map(Enum::toString)
            .collect(Collectors.toList());
    }

}
