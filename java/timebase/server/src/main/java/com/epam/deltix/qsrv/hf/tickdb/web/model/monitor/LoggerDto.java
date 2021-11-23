package com.epam.deltix.qsrv.hf.tickdb.web.model.monitor;

import com.epam.deltix.gflog.api.LogLevel;

public class LoggerDto {

    private String name;
    private LogLevel level;

    public LoggerDto() {
    }

    public LoggerDto(String name, LogLevel level) {
        this.name = name;
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LogLevel getLevel() {
        return level;
    }

    public void setLevel(LogLevel level) {
        this.level = level;
    }
}
