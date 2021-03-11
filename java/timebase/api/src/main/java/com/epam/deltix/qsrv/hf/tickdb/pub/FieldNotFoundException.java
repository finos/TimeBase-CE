package com.epam.deltix.qsrv.hf.tickdb.pub;

public class FieldNotFoundException extends Exception {
    private String field;

    public FieldNotFoundException(String field) {
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
