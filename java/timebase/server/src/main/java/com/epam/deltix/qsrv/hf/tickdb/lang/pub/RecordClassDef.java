package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.parsers.Location;

/**
 *
 */
public final class RecordClassDef extends ClassDef {
    public final AttributeDef []        attributes;
    public final boolean                auxiliary;
    public final boolean                instantiable;
    public final TypeIdentifier         parent;

    public RecordClassDef (
            long                            location,
            TypeIdentifier                  id,
            String                          title,
            String                          comment,
            boolean                         auxiliary,
            boolean                         instantiable,
            TypeIdentifier                  parent,
            AttributeDef ...                attributes
    )
    {
        super (location, id, title, comment);
        this.attributes = attributes;
        this.auxiliary = auxiliary;
        this.instantiable = instantiable;
        this.parent = parent;
    }

    public RecordClassDef (
            TypeIdentifier                  id,
            String                          title,
            String                          comment,
            boolean                         auxiliary,
            boolean                         instantiable,
            TypeIdentifier                  parent,
            AttributeDef ...                attributes
    )
    {
        this (
                Location.NONE, id, title, comment,
                auxiliary, instantiable, parent, attributes
        );
    }

    @Override
    public void                 print (StringBuilder s) {
        s.append ("CLASS ");
        printHeader (s);

        if (parent != null) {
            s.append (" UNDER ");
            parent.print (s);
        }

        s.append (" (");

        boolean     first = true;

        for (AttributeDef ad : attributes) {
            if (first) {
                first = false;
                s.append ('\n');
            }
            else
                s.append (",\n");

            ad.print (s);
        }

        s.append ("\n)");

        s.append (auxiliary ? "\nAUXILIARY" : "\nNOT AUXILIARY");
        s.append (instantiable ? "\nINSTANTIABLE" : "\nNOT INSTANTIABLE");

        printComment (s);
    }
}
