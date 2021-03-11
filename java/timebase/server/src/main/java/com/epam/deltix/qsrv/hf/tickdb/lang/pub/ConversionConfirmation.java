package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.qsrv.hf.tickdb.lang.errors.*;

/**
 *  Levels of conversion allowed when modifying schema.
 */
public enum ConversionConfirmation {
    NO_CONVERSION,
    CONVERT_DATA,
    DROP_ATTRIBUTES,
    DROP_TYPES,
    DROP_DATA;
    
    public static ConversionConfirmation    fromId (Identifier id) {
        if (id == null)
            return (NO_CONVERSION);
        
        String  key = id.id.replaceAll ("[_|-]", "");
        
        for (ConversionConfirmation v : ConversionConfirmation.values ())
            if (key.equalsIgnoreCase (v.name ().replaceAll ("[_|-]", "")))
                return (v);
        
        throw new UnknownIdentifierException (id);
    }        
}
