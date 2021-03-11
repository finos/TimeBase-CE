package com.epam.deltix.qsrv.hf.tickdb.schema;

import javax.xml.bind.annotation.XmlElement;

public final class ErrorResolution {

    @XmlElement
    public String defaultValue;

    @XmlElement
    public Result result;

    public ErrorResolution() { // JAXB
    }

    protected ErrorResolution(String defaultValue, Result result) {
        this.defaultValue = defaultValue;
        this.result = result;
    }

    public static ErrorResolution       resolve(String defaultValue) {
        return new ErrorResolution(defaultValue, Result.Resolved);
    }

    public static ErrorResolution       ignore() {
        return new ErrorResolution(null, Result.Ignored);
    }

    public enum Result {
        Resolved, Ignored
    }
}