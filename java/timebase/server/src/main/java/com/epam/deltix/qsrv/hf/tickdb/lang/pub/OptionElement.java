package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.parsers.Element;
import com.epam.deltix.util.parsers.Location;
import java.util.*;
/**
 *
 */
public final class OptionElement extends Element {
    public static OptionElement []  toArray (List <OptionElement> arrayList) {
        if (arrayList == null) 
            return (null);
        
        OptionElement []   optArray = new OptionElement [arrayList.size ()];
        
        arrayList.toArray (optArray);
        
        return (optArray);
    }
    
    public final Identifier     id;
    public final Expression     value;

    public OptionElement (long location, Identifier id, Expression value) {
        super (location);
        this.id = id;
        this.value = value;
    }
    
    public OptionElement (Identifier id, Expression value) {
        super (Location.NONE);
        
        this.id = id;
        this.value = value;
    }
    
    public OptionElement (Identifier id) {
        this (id, null);
    }

    @Override
    public void             print (StringBuilder s) {
        id.print (s);
        
        if (value != null) {
            s.append (" = ");
            value.print (s);
        }
    }  
    
    public static void      print (OptionElement [] options, StringBuilder s) {
        if (options != null) {
            s.append ("\nOPTIONS (");
            
            boolean     first = true;
            
            for (OptionElement oe : options) {
                if (first)
                    first = false;
                else
                    s.append ("; ");
                
                oe.print (s);
            }
            
            s.append (")");
        }
    }
}
