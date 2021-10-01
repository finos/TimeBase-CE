/*
 * Copyright 2021 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.parsers.Location;

/**
 *
 */
public final class SimpleDataTypeSpec extends DataTypeSpec {
    public static final int         NO_DIMENSION = -1;
    
    public final TypeIdentifier     typeId;
    public final String             encoding;
    public final int                dimension;
    public final Expression         min;
    public final Expression         max;

    public SimpleDataTypeSpec (
        long                        location,
        TypeIdentifier              typeId,
        boolean                     nullable,
        String                      encoding,
        int                         dimension,
        Expression                  min,
        Expression                  max
    )
    {
        super (location, nullable);
        
        this.typeId = typeId;
        this.encoding = encoding == null ? null : encoding.toUpperCase ();
        this.dimension = dimension;
        this.min = min;
        this.max = max;
    }

    public SimpleDataTypeSpec (
        TypeIdentifier              typeId,
        boolean                     nullable,
        String                      encoding,
        int                         dimension,
        Expression                  min,
        Expression                  max
    )
    {
        this (Location.NONE, typeId, nullable, encoding, dimension, min, max);
    }
    
    @Override
    public void             print (StringBuilder s) {
        typeId.print (s);
        
        if (!nullable)
            s.append (" NOT NULL");
        
        if (encoding != null) {
            s.append (" ");
            s.append (encoding);
            
            if (dimension != NO_DIMENSION) {
                s.append (" (");
                s.append (dimension);
                s.append (")");
            }                        
        }
        
        if (min != null || max != null) {
            s.append (" BETWEEN ");
            if (min == null)
                s.append ("*");
            else
                min.print (s);
            s.append (" AND ");
            if (min == null)
                s.append ("*");
            else
                min.print (s);
        }
    }        
}