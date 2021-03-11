package com.epam.deltix.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.util.lang.Util;

/**
 *  A simple implementation of the {@link TypeLoader} interface, which maps
 *  class names to pre-existing classes.
 */
public class SimpleTypeLoader implements TypeLoader {
    private final String []       names;
    private final Class <?> []    classes;
    
    public SimpleTypeLoader (Object ... params) {
        if (params.length == 0)
            throw new IllegalArgumentException ("No arguments");

        int                 n = params.length / 2;

        if (params.length != n * 2)
            throw new IllegalArgumentException ("Odd # of arguments");

        names = new String [n];
        classes = new Class <?> [n];

        for (int ii = 0; ii < n; ii++) {
            int             base = ii * 2;
            names [ii] = (String) params [base];

            Object          p = params [base + 1];
            Class <?>       pc = p.getClass ();
            Class <?>       c;

            if (pc == Class.class)
                c = (Class <?>) p;
            else
                throw new IllegalArgumentException ("Illegal argument: " + p);

            classes [ii] = c;
        }       
    }

    public Class <?>                load (ClassDescriptor cd)
        throws ClassNotFoundException
    {
        for (int ii = 0; ii < names.length; ii++)
            if (Util.equals (names [ii], cd.getName ()))
                return (classes [ii]);

        return (TypeLoaderImpl.DEFAULT_INSTANCE.load (cd));
    }

}
