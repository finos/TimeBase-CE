package com.epam.deltix.qsrv.hf.tickdb.web.model.monitor;

public class ThreadDto {

    private long id;
    private String name;
    private Thread.State state;
    private String stackTrace;

    public ThreadDto(long id, String name, Thread.State state, String stackTrace) {
        this.id = id;
        this.name = name;
        this.state = state;
        this.stackTrace = stackTrace;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Thread.State getState() {
        return state;
    }

    public void setState(Thread.State state) {
        this.state = state;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }
}
