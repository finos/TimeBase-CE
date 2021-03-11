package com.epam.deltix.qsrv.hf.pub.md;

/**
 *
 */
public class UndefinedClassNameException extends RuntimeException {
    public final String         name;

    public UndefinedClassNameException (String name) {
        super ("Undefined class name " + name);
        this.name = name;
    }        
}
