package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.parsers.Element;
import com.epam.deltix.util.parsers.Location;

/**
 *
 */
public class TypeIdentifier extends Element {
    public final String                 typeName;

    public TypeIdentifier (String typeName) {
        super (NO_LOCATION);
        this.typeName = typeName;
    }

    public TypeIdentifier (long location, String typeName) {
        super (location);
        this.typeName = typeName;
    }

    public TypeIdentifier (TypeIdentifier pack, String typeName, long end) {
        super (Location.fromTo (pack.location, end));
        
        this.typeName = pack.typeName + "." + typeName;
    }

    @Override
    public void                         print (StringBuilder s) {
         GrammarUtil.escapeIdentifier (NamedObjectType.TYPE, typeName, s);
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            typeName.equals (((TypeIdentifier) obj).typeName)
        );
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * 41 + typeName.hashCode ());
    }
}
