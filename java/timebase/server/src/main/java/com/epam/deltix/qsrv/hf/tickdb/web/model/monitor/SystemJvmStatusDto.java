package com.epam.deltix.qsrv.hf.tickdb.web.model.monitor;

import java.util.List;

public class SystemJvmStatusDto {

    private int availableProcessors;
    private long maxMem;
    private long freeMem;
    private long currentMem;
    private List<String> arguments;

    public SystemJvmStatusDto(int availableProcessors, long maxMem, long freeMem, long currentMem, List<String> arguments) {
        this.availableProcessors = availableProcessors;
        this.maxMem = maxMem;
        this.freeMem = freeMem;
        this.currentMem = currentMem;
        this.arguments = arguments;
    }

    public int getAvailableProcessors() {
        return availableProcessors;
    }

    public void setAvailableProcessors(int availableProcessors) {
        this.availableProcessors = availableProcessors;
    }

    public long getMaxMem() {
        return maxMem;
    }

    public void setMaxMem(long maxMem) {
        this.maxMem = maxMem;
    }

    public long getFreeMem() {
        return freeMem;
    }

    public void setFreeMem(long freeMem) {
        this.freeMem = freeMem;
    }

    public long getCurrentMem() {
        return currentMem;
    }

    public void setCurrentMem(long currentMem) {
        this.currentMem = currentMem;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public void setArguments(List<String> arguments) {
        this.arguments = arguments;
    }
}
