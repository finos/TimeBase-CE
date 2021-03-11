package com.epam.deltix.snmp.parser;

/**
 *
 */
public final class TextualConvention extends Type {
    public final String                 hint;
    public final String                 status;
    public final String                 description;
    public final String                 reference;
    public final Type                   syntax;

    public TextualConvention (
        long                            location, 
        String                          hint,
        String                          status,
        String                          description,
        String                          reference,
        Type                            syntax
    )
    {
        super (location);
        
        this.hint = hint;
        this.status = status;
        this.description = description;
        this.reference = reference;
        this.syntax = syntax;
    }    
}
