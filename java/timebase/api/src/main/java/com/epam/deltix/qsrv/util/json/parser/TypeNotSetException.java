package com.epam.deltix.qsrv.util.json.parser;

public class TypeNotSetException extends Exception {
    public TypeNotSetException() {
        super("Type is not defined in json record.");
    }
}