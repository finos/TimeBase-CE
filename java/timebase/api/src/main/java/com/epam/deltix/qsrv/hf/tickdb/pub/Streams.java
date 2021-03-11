package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;

import java.util.TreeSet;

public abstract class Streams {

    public static RecordClassDescriptor[]     catTypes (TickStream ... streams) {
        TreeSet<RecordClassDescriptor> set =
                new TreeSet <> ();

        for (TickStream s : streams) {
            if (s.isFixedType ())
                set.add (s.getFixedType ());
            else
                for (RecordClassDescriptor c : s.getPolymorphicDescriptors ())
                    set.add (c);
        }

        return (set.toArray (new RecordClassDescriptor [set.size ()]));
    }
}
