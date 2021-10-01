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
package com.epam.deltix.test.qsrv.hf.tickdb.testframework;

import com.epam.deltix.qsrv.test.messages.TestEnum;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.timebase.messages.*;

import com.epam.deltix.util.collections.generated.ByteArrayList;
import junit.framework.Assert;

/**
 *
 */
public class TestAllTypesMessage extends InstrumentMessage {
    
    private static final Introspector       ix = 
        Introspector.createEmptyMessageIntrospector ();
    private static RecordClassDescriptor    myDescriptor = null;
    
    @SchemaElement(
            title = "Sequence"
    )
    public int                  sequence;
    //
    //  BOOLEAN
    //
    @SchemaElement(
            title = "Non-nullable BOOLEAN"
    )
    public boolean              bool_c;
    
    @SchemaElement(
            title = "Nullable BOOLEAN"
    )
    @SchemaType(
            dataType = SchemaDataType.BOOLEAN
    )
    public byte                 bool_n;
    //
    //  FLOAT
    //
    @SchemaElement(
            title = "Non-nullable FLOAT:IEEE32"
    )
    @SchemaType(
            encoding = "IEEE32",
            dataType = SchemaDataType.FLOAT,
            isNullable = false
    )
    public float                float_c_32;

    @SchemaElement(
            title = "Nullable FLOAT:IEEE32"
    )
    @SchemaType(
            encoding = "IEEE32",
            dataType = SchemaDataType.FLOAT
    )
    public float                float_n_32;

    @SchemaElement(
            title = "Non-nullable FLOAT:IEEE64"
    )
    @SchemaType(
            encoding = "IEEE64",
            dataType = SchemaDataType.FLOAT,
            isNullable = false
    )
    public double               float_c_64;

    @SchemaElement(
            title = "Nullable FLOAT:IEEE64"
    )
    @SchemaType(
            encoding = "IEEE64",
            dataType = SchemaDataType.FLOAT
    )
    public double               float_n_64;

    @SchemaElement(
            title = "Non-nullable FLOAT:DECIMAL"
    )
    @SchemaType(
            encoding = "DECIMAL",
            dataType = SchemaDataType.FLOAT,
            isNullable = false
    )
    public double               float_c_dec;

    @SchemaElement(
            title = "Nullable FLOAT:DECIMAL"
    )
    @SchemaType(
            encoding = "DECIMAL",
            dataType = SchemaDataType.FLOAT
    )
    public double               float_n_dec;

    @SchemaElement(
            title = "Non-nullable FLOAT:DECIMAL(2)"
    )
    @SchemaType(
            encoding = "DECIMAL(2)",
            dataType = SchemaDataType.FLOAT,
            isNullable = false
    )
    public double               float_c_dec2;

    @SchemaElement(
            title = "Nullable FLOAT:DECIMAL(2)"
    )
    @SchemaType(
            encoding = "DECIMAL(2)",
            dataType = SchemaDataType.FLOAT
    )
    public double               float_n_dec2;
    //
    //  INTEGER
    //
    @SchemaElement(
            title = "Non-nullable INTEGER:INT8"
    )
    @SchemaType(
            encoding = "INT8",
            dataType = SchemaDataType.INTEGER,
            isNullable = false
    )
    public byte                 int_c_8;

    @SchemaElement(
            title = "Nullable INTEGER:INT8"
    )
    @SchemaType(
            encoding = "INT8",
            dataType = SchemaDataType.INTEGER
    )
    public byte                 int_n_8;

    @SchemaElement(
            title = "Non-nullable INTEGER:INT16"
    )
    @SchemaType(
            encoding = "INT16",
            dataType = SchemaDataType.INTEGER,
            isNullable = false
    )
    public short                int_c_16;

    @SchemaElement(
            title = "Nullable INTEGER:INT16"
    )
    @SchemaType(
            encoding = "INT16",
            dataType = SchemaDataType.INTEGER
    )
    public short                int_n_16;

    @SchemaElement(
            title = "Non-nullable INTEGER:INT32"
    )
    @SchemaType(
            encoding = "INT32",
            dataType = SchemaDataType.INTEGER,
            isNullable = false
    )
    public int                  int_c_32;

    @SchemaElement(
            title = "Nullable INTEGER:INT32"
    )
    @SchemaType(
            encoding = "INT32",
            dataType = SchemaDataType.INTEGER
    )
    public int                  int_n_32;

    @SchemaElement(
            title = "Non-nullable INTEGER:INT64"
    )
    @SchemaType(
            encoding = "INT64",
            dataType = SchemaDataType.INTEGER,
            isNullable = false
    )
    public long                 int_c_64;

    @SchemaElement(
            title = "Nullable INTEGER:INT64"
    )
    @SchemaType(
            encoding = "INT64",
            dataType = SchemaDataType.INTEGER
    )
    public long                 int_n_64;

    @SchemaElement(
            title = "Non-nullable INTEGER:PUINT30"
    )
    @SchemaType(
            encoding = "PUINT30",
            dataType = SchemaDataType.INTEGER,
            isNullable = false
    )
    public int                  puint_c_30;

    @SchemaElement(
            title = "Nullable INTEGER:PUINT30"
    )
    @SchemaType(
            encoding = "PUINT30",
            dataType = SchemaDataType.INTEGER
    )
    public int                  puint_n_30;

    @SchemaElement(
            title = "Non-nullable INTEGER:PUINT61"
    )
    @SchemaType(
            encoding = "PUINT61",
            dataType = SchemaDataType.INTEGER,
            isNullable = false
    )
    public long                 puint_c_61;

    @SchemaElement(
            title = "Nullable INTEGER:PUINT61"
    )
    @SchemaType(
            encoding = "PUINT61",
            dataType = SchemaDataType.INTEGER
    )
    public long                 puint_n_61;
    //
    //  CHAR
    //
    @SchemaElement(
            title = "Non-nullable CHAR"
    )
    @SchemaType(
            isNullable = false
    )
    public char                 char_c;
    
    @SchemaElement(
            title = "Nullable CHAR"
    )
    public char                 char_n;
    //
    //  VARCHAR
    //
    @SchemaElement(
            title = "Non-nullable VARCHAR:UTF8"
    )
    @SchemaType(
            isNullable = false
    )
    public String               varchar_c_utf8;
    
    @SchemaElement(
            title = "Nullable VARCHAR:UTF8"
    )
    public String               varchar_n_utf8;

    @SchemaElement(
            title = "Non-nullable VARCHAR:ALPHANUMERIC(10):long"
    )
    @SchemaType(
            encoding = "ALPHANUMERIC(10)",
            dataType = SchemaDataType.VARCHAR,
            isNullable = false
    )
    public long                 varchar_c_alpha10;

    @SchemaElement(
            title = "Nullable VARCHAR:ALPHANUMERIC(10):long"
    )
    @SchemaType(
            encoding = "ALPHANUMERIC(10)",
            dataType = SchemaDataType.VARCHAR
    )
    public long                 varchar_n_alpha10;

    @SchemaElement(
            title = "Non-nullable VARCHAR:ALPHANUMERIC(5)"
    )
    @SchemaType(
            encoding = "ALPHANUMERIC(5)",
            dataType = SchemaDataType.VARCHAR,
            isNullable = false
    )
    public CharSequence         varchar_c_alpha5_s;

    @SchemaElement(
            title = "Nullable VARCHAR:ALPHANUMERIC(5)"
    )
    @SchemaType(
            encoding = "ALPHANUMERIC(5)",
            dataType = SchemaDataType.VARCHAR
    )
    public CharSequence         varchar_n_alpha5_s;
    //
    //  TIMEOFDAY
    //
    @SchemaElement(
            title = "Non-nullable TIMEOFDAY"
    )
    @SchemaType(
            dataType = SchemaDataType.TIME_OF_DAY,
            isNullable = false
    )
    public int                  tod_c;

    @SchemaElement(
            title = "Nullable TIMEOFDAY"
    )
    @SchemaType(
            dataType = SchemaDataType.TIME_OF_DAY
    )
    public int                  tod_n;
    //
    //  DATE
    //
    @SchemaElement(
            title = "Non-nullable DATE"
    )
    @SchemaType(
            dataType = SchemaDataType.TIMESTAMP,
            isNullable = false
    )
    public long                 date_c;
    
    @SchemaElement(
            title = "Nullable DATE"
    )
    @SchemaType(
            dataType = SchemaDataType.TIMESTAMP
    )
    public long                 date_n;
    //
    //  ENUM
    //
    @SchemaElement(
            title = "Non-nullable ENUM"
    )
    @SchemaType(
            isNullable = false
    )
    public TestEnum enum_c;
    
    @SchemaElement(
            title = "Nullable ENUM"
    )
    public TestEnum             enum_n;
    
//    @Enumerated(deltix.qsrv.hf.tickdb.testframework.TestBitmask.class)
//    @SchemaElement(
//            title = "Non-nullable BITMASK"
//    )
//    @SchemaType(
//            isNullable = false
//    )
//    public long                 bitmask_c;
    
    // BUG 10318
    //@Enumerated ("deltix.qsrv.hf.tickdb.testframework.TestBitmask")
    //@Title ("Nullable BITMASK")
    //public long                 bitmask_n;
    //
    //  BINARY
    //
    // BUG 10303
    //@NotNull @Title ("Non-nullable BINARY")
    //public ByteArrayList        binary_c;
    
    @SchemaElement(
            title = "Nullable BINARY"
    )
    public ByteArrayList        binary_n;
        
    public static synchronized RecordClassDescriptor getClassDescriptor () {
        if (myDescriptor == null) {
            try {
                myDescriptor = ix.introspectRecordClass (TestAllTypesMessage.class);
            } catch (Introspector.IntrospectionException x) {
                throw new RuntimeException (x);   // Unexpected, this should be reliable.
            }
        }
        
        return (myDescriptor);
    }

    public void         assertEquals (String diag, TestAllTypesMessage other) {        
        Assert.assertEquals (diag + "sequence", this.sequence, other.sequence);
        Assert.assertEquals (diag + "bool_c", this.bool_c, other.bool_c);
        Assert.assertEquals (diag + "bool_n", this.bool_n, other.bool_n);
        Assert.assertEquals (diag + "float_c_32", this.float_c_32, other.float_c_32);        
        Assert.assertEquals (diag + "float_n_32", this.float_n_32, other.float_n_32);
        Assert.assertEquals (diag + "float_c_64", this.float_c_64, other.float_c_64);
        Assert.assertEquals (diag + "float_n_64", this.float_n_64, other.float_n_64);
        Assert.assertEquals (diag + "float_c_dec", this.float_c_dec, other.float_c_dec);
        Assert.assertEquals (diag + "float_n_dec", this.float_n_dec, other.float_n_dec);
        Assert.assertEquals (diag + "float_c_dec2", this.float_c_dec2, other.float_c_dec2, 0.01);
        Assert.assertEquals (diag + "float_n_dec2", this.float_n_dec2, other.float_n_dec2, 0.01);
        Assert.assertEquals (diag + "int_c_8", this.int_c_8, other.int_c_8);
        Assert.assertEquals (diag + "int_n_8", this.int_n_8, other.int_n_8);
        Assert.assertEquals (diag + "int_c_16", this.int_c_16, other.int_c_16);
        Assert.assertEquals (diag + "int_n_16", this.int_n_16, other.int_n_16);
        Assert.assertEquals (diag + "int_c_32", this.int_c_32, other.int_c_32);
        Assert.assertEquals (diag + "int_n_32", this.int_n_32, other.int_n_32);
        Assert.assertEquals (diag + "int_c_64", this.int_c_64, other.int_c_64);
        Assert.assertEquals (diag + "int_n_64", this.int_n_64, other.int_n_64);
        Assert.assertEquals (diag + "puint_c_30", this.puint_c_30, other.puint_c_30);
        Assert.assertEquals (diag + "puint_n_30", this.puint_n_30, other.puint_n_30);
        Assert.assertEquals (diag + "puint_c_61", this.puint_c_61, other.puint_c_61);
        Assert.assertEquals (diag + "puint_n_61", this.puint_n_61, other.puint_n_61);
        Assert.assertEquals (diag + "char_c", this.char_c, other.char_c);
        Assert.assertEquals (diag + "char_n", this.char_n, other.char_n);
        Assert.assertEquals (diag + "varchar_c_utf8", this.varchar_c_utf8, other.varchar_c_utf8);
        Assert.assertEquals (diag + "varchar_n_utf8", this.varchar_n_utf8, other.varchar_n_utf8);
        Assert.assertEquals (diag + "varchar_c_alpha10", this.varchar_c_alpha10, other.varchar_c_alpha10);
        Assert.assertEquals (diag + "varchar_n_alpha10", this.varchar_n_alpha10, other.varchar_n_alpha10);
        Assert.assertEquals (diag + "varchar_c_alpha5_s", this.varchar_c_alpha5_s != null ? this.varchar_c_alpha5_s.toString() : this.varchar_c_alpha5_s, other.varchar_c_alpha5_s != null ? other.varchar_c_alpha5_s.toString() : other.varchar_c_alpha5_s);
        Assert.assertEquals (diag + "varchar_n_alpha5_s", this.varchar_n_alpha5_s != null ? this.varchar_n_alpha5_s.toString() : this.varchar_n_alpha5_s, other.varchar_n_alpha5_s!= null ? other.varchar_n_alpha5_s.toString() : other.varchar_n_alpha5_s);
        Assert.assertEquals (diag + "tod_c", this.tod_c, other.tod_c);
        Assert.assertEquals (diag + "tod_n", this.tod_n, other.tod_n);
        Assert.assertEquals (diag + "date_c", this.date_c, other.date_c);
        Assert.assertEquals (diag + "date_n", this.date_n, other.date_n);
        Assert.assertEquals (diag + "enum_c", this.enum_c, other.enum_c);
        Assert.assertEquals (diag + "enum_n", this.enum_n, other.enum_n);
        
        // BUG 10317
        //Assert.assertEquals (diag + "bitmask_c", this.bitmask_c, other.bitmask_c);
        
        // BUG 10303
        //Assert.assertEquals (diag + "binary_c", this.binary_c, other.binary_c);
        
        Assert.assertEquals (diag + "binary_n", this.binary_n, other.binary_n);                
    }
    
    public static void main (String [] args) throws Exception {
        RecordClassDescriptor   rcd = getClassDescriptor ();
                
        System.out.println (rcd);
    }
}
