package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.parsers.Location;

/**
 *
 */
public class ClassDataTypeSpec extends PolymorphicDataTypeSpec {
    public ClassDataTypeSpec (
        long                        location,
        DataTypeSpec[]              elementsTypeSpec,
        boolean                     nullable
    )
    {
        super (location, elementsTypeSpec, nullable);
    }

    public ClassDataTypeSpec (
        DataTypeSpec[]              elementTypeSpec,
        boolean                     nullable
    )
    {
        this (Location.NONE, elementTypeSpec, nullable);
    }

    @Override
    public void             print (StringBuilder s) {
        s.append("OBJECT");
        s.append("(");

        boolean first = true;
        for (DataTypeSpec element : elementsTypeSpec) {
            if (first)
                first = false;
            else
                s.append (",");

            element.print (s);
        }

        s.append(")");

        if (!nullable)
            s.append (" NOT NULL");
    }
}
