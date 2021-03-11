package com.epam.deltix.qsrv.snmp.modimpl;

import com.epam.deltix.qsrv.snmp.model.timebase.Failure;

/**
 *
 */
public class FailureImpl implements Failure {

    private String message;
    private int index;
    
    public FailureImpl(int index, Exception e) {
        this.index = index;
        this.message = e.getMessage();
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
