package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.parsers.Location;

/**
 *
 */
public class PolymorphicDataTypeSpec extends DataTypeSpec {
    public final DataTypeSpec[]     elementsTypeSpec;

    public PolymorphicDataTypeSpec (
        long                        location,
        DataTypeSpec[]              elementsTypeSpec,
        boolean                     nullable
    )
    {
        super (location, nullable);

        this.elementsTypeSpec = elementsTypeSpec;
    }

    public PolymorphicDataTypeSpec (
        DataTypeSpec[]              elementTypeSpec,
        boolean                     nullable
    )
    {
        this (Location.NONE, elementTypeSpec, nullable);
    }

}
