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
package com.epam.deltix.qsrv.hf.tickdb.pub.query;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.pub.values.ValueBean;
import java.io.Serializable;

/**
 *  Input parameter definition for a prepared statement.
 */
public final class Parameter implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public final String             name;
    public final DataType           type;
    public final ValueBean          value;

    public static ValueBean []      valuesOf (Parameter [] params) {
        if (params == null)
            return (null);
        
        int         n = params.length;
        
        if (n == 0)
            return (null);
        
        ValueBean []        ret = new ValueBean [params.length];
        
        for (int ii = 0; ii < n; ii++)
            ret [ii] = params [ii].value;
        
        return (ret);
    }
    
    public Parameter (String name, DataType type) {
        this.name = name;
        this.type = type;
        value = ValueBean.forType (type);
    }

    public static Parameter         BOOLEAN (String name, boolean value) {
        Parameter   p = new Parameter (name, StandardTypes.CLEAN_BOOLEAN);

        p.value.writeBoolean (value);

        return (p);
    }

    public static Parameter         INTEGER (String name, long value) {
        Parameter   p = new Parameter (name, StandardTypes.CLEAN_INTEGER);

        p.value.writeLong (value);

        return (p);
    }

    public static Parameter         FLOAT (String name, float value) {
        Parameter   p = new Parameter (name, StandardTypes.CLEAN_FLOAT);

        p.value.writeFloat (value);

        return (p);
    }

    public static Parameter         FLOAT (String name, double value) {
        Parameter   p = new Parameter (name, StandardTypes.CLEAN_FLOAT);

        p.value.writeDouble (value);

        return (p);
    }

    public static Parameter         VARCHAR (String name, CharSequence value) {
        Parameter   p = new Parameter (name, StandardTypes.CLEAN_VARCHAR);

        p.value.writeString (value);

        return (p);
    }

    public static Parameter         TIMESTAMP (String name, long value) {
        Parameter   p = new Parameter (name, StandardTypes.CLEAN_VARCHAR);

        p.value.writeLong (value);

        return (p);
    }
}