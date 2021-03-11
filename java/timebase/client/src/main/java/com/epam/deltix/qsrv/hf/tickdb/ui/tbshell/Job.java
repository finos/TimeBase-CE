package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell;

import com.epam.deltix.util.progress.ExecutionMonitorImpl;

/**
 *
 */
public class Job extends Thread {

    public final ExecutionMonitorImpl monitor = new ExecutionMonitorImpl();
    public String name;

    public Job(String name) {
        this.name = name;
    }
}
