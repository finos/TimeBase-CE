package com.epam.deltix.qsrv.hf.pub.md;

import java.util.Comparator;

/**
 *
 */
public class NamedDescriptorComparator implements Comparator <NamedDescriptor> {
    private final int       dir;

    private NamedDescriptorComparator (int dir) {
        this.dir = dir;
    }       
    
    public int  compare (NamedDescriptor o1, NamedDescriptor o2) {
        return (dir * o1.getName ().compareTo (o2.getName ()));
    }
    
    public static final Comparator <NamedDescriptor>    ASCENDING =
        new NamedDescriptorComparator (1);
    
    public static final Comparator <NamedDescriptor>    DESCENDING =
        new NamedDescriptorComparator (-1);
}
