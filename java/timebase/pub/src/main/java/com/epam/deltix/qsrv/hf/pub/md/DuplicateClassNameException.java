package com.epam.deltix.qsrv.hf.pub.md;

/**
 *
 */
public class DuplicateClassNameException extends RuntimeException {
    public final String         name;

    public DuplicateClassNameException (String name) {
        super ("Duplicate class name " + name);
        this.name = name;
    }        
}
