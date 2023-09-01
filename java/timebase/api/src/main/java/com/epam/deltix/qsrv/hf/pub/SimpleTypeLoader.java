/*
 * Copyright 2023 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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