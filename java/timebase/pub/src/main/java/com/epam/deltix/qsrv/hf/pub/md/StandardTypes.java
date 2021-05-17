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
package com.epam.deltix.qsrv.hf.pub.md;

/**
 *
 */
public class StandardTypes {
    public static final DataType CLEAN_BOOLEAN = new BooleanDataType (false);
    public static final DataType NULLABLE_BOOLEAN = new BooleanDataType (true);
    
    public static final DataType CLEAN_INTEGER = new IntegerDataType (IntegerDataType.ENCODING_INT64, false);
    public static final DataType NULLABLE_INTEGER = new IntegerDataType (IntegerDataType.ENCODING_INT64, true);
    
    public static final DataType CLEAN_FLOAT = new FloatDataType (FloatDataType.ENCODING_FIXED_DOUBLE, false);
    public static final DataType NULLABLE_FLOAT = new FloatDataType (FloatDataType.ENCODING_FIXED_DOUBLE, true);
    
    public static final DataType CLEAN_VARCHAR = new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, false, true);
    public static final DataType NULLABLE_VARCHAR = new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, true);
    
    public static final DataType CLEAN_CHAR = new CharDataType (false);
    public static final DataType NULLABLE_CHAR = new CharDataType (true);
    
    public static final DataType CLEAN_TIMESTAMP = new DateTimeDataType (false);
    public static final DataType NULLABLE_TIMESTAMP = new DateTimeDataType (true);
    
    public static final DataType CLEAN_TIMEOFDAY = new TimeOfDayDataType (false);
    public static final DataType NULLABLE_TIMEOFDAY = new TimeOfDayDataType (true);
    
    public static final DataType CLEAN_BINARY = new BinaryDataType (false, 0);
    public static final DataType NULLABLE_BINARY = new BinaryDataType (true, 0);
    
    public static final DataType CLEAN_QUERY = new QueryDataType (false, null);
    public static final DataType NULLABLE_QUERY = new QueryDataType (true, null);
    
    public static final String []  PRIMITIVE_FIELD_TYPE_NAMES = {
        CLEAN_BOOLEAN.getBaseName (),
        CLEAN_INTEGER.getBaseName (),
        CLEAN_FLOAT.getBaseName (),
        CLEAN_VARCHAR.getBaseName (),
        CLEAN_CHAR.getBaseName (),
        CLEAN_TIMESTAMP.getBaseName (),
        CLEAN_TIMEOFDAY.getBaseName (),
        CLEAN_BINARY.getBaseName (),
    };
    
    public static String        toSimpleName (DataType type) {
        String  s = type.getBaseName ();
        
        if (type.isNullable ())
            s += "?";
        
        return (s);
    }
    
    public static DataType      forName (String name) {
        name = name.trim ();
        
        if (name.equals ("BOOLEAN"))
            return (CLEAN_BOOLEAN);
        
        if (name.equals ("BOOLEAN?"))
            return (NULLABLE_BOOLEAN);
        
        if (name.equals ("INTEGER"))
            return (CLEAN_INTEGER);
        
        if (name.equals ("INTEGER?"))
            return (NULLABLE_INTEGER);
        
        if (name.equals ("FLOAT"))
            return (CLEAN_FLOAT);
        
        if (name.equals ("FLOAT?"))
            return (NULLABLE_FLOAT);
        
        if (name.equals ("VARCHAR"))
            return (CLEAN_VARCHAR);
        
        if (name.equals ("VARCHAR?"))
            return (NULLABLE_VARCHAR);
        
        if (name.equals ("CHAR"))
            return (CLEAN_CHAR);
        
        if (name.equals ("CHAR?"))
            return (NULLABLE_CHAR);
        
        if (name.equals ("TIMESTAMP"))
            return (CLEAN_TIMESTAMP);
        
        if (name.equals ("TIMESTAMP?"))
            return (NULLABLE_TIMESTAMP);
        
        if (name.equals ("TIMEOFDAY"))
            return (CLEAN_TIMEOFDAY);
        
        if (name.equals ("TIMEOFDAY?"))
            return (NULLABLE_TIMEOFDAY);
        
        if (name.equals ("BINARY"))
            return (CLEAN_BINARY);
        
        if (name.equals ("BINARY?"))
            return (NULLABLE_BINARY);
        
        if (name.equals ("QUERY"))
            return (CLEAN_QUERY);
        
        if (name.equals ("QUERY?"))
            return (NULLABLE_QUERY);
        
        return (null);
    }
}
