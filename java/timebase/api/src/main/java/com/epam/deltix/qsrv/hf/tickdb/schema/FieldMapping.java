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
package com.epam.deltix.qsrv.hf.tickdb.schema;

import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundEncoder;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.schema.encoders.IgnoreWriter;
import com.epam.deltix.qsrv.hf.tickdb.schema.encoders.WritableValueDelegate;
import com.epam.deltix.qsrv.hf.tickdb.schema.encoders.DefaultValueEncoder;
import com.epam.deltix.qsrv.hf.tickdb.schema.encoders.MixedWritableValue;

public class FieldMapping {

    public DataType                         sourceType;
    public NonStaticFieldLayout             source;
    public int                              sourceIndex;
    public SchemaConverter.DataTypeIndex    sourceTypeIndex;

    public DataType                         targetType;
    public NonStaticFieldLayout             target;
    public int                              targetIndex;
    public SchemaConverter.DataTypeIndex    targetTypeIndex;

    public ErrorResolution                  resolution;

    FieldMapping(NonStaticFieldLayout target, NonStaticFieldLayout source) {
        this.target = target;
        this.targetType = target.getField().getType();
        this.targetTypeIndex = getTypeIndex(targetType);

        if (source != null) {
            this.source = source;
            this.sourceType = source.getField().getType();
            this.sourceTypeIndex = getTypeIndex(sourceType);
        }
    }

    public MixedWritableValue       getWritable(UnboundEncoder encoder) {

        if (resolution != null)
            return resolution.result == ErrorResolution.Result.Resolved ?
                            new DefaultValueEncoder(encoder, resolution.defaultValue, targetType) :
                            new IgnoreWriter(encoder, targetType);
        else
            return new WritableValueDelegate(encoder);
            //throw new IllegalStateException("Resolution is null for " + this);
    }

    public static SchemaConverter.DataTypeIndex getTypeIndex(DataType type) {
        
        if (type instanceof BooleanDataType)
            return SchemaConverter.DataTypeIndex.Boolean;

        else if (type instanceof VarcharDataType)
            return SchemaConverter.DataTypeIndex.String;

        else if (type instanceof CharDataType)
            return SchemaConverter.DataTypeIndex.String;

        else if (type instanceof DateTimeDataType)
            return SchemaConverter.DataTypeIndex.Long;

        else if (type instanceof TimeOfDayDataType)
            return SchemaConverter.DataTypeIndex.Int;

        else if (type instanceof IntegerDataType) {
            if (((IntegerDataType)type).getNativeTypeSize() < 8)
                return SchemaConverter.DataTypeIndex.Int;
            return SchemaConverter.DataTypeIndex.Long;

        } else if (type instanceof FloatDataType) {
            if (((FloatDataType) type).isDecimal64())
                return SchemaConverter.DataTypeIndex.Long;

            if (((FloatDataType) type).isFloat())
                return SchemaConverter.DataTypeIndex.Float;

            return SchemaConverter.DataTypeIndex.Double;

        } else if (type instanceof EnumDataType)
           return SchemaConverter.DataTypeIndex.Enum;

        else if (type instanceof ArrayDataType)
            return SchemaConverter.DataTypeIndex.Array;

        else if (type instanceof ClassDataType)
            return SchemaConverter.DataTypeIndex.Object;

        if (type instanceof BinaryDataType)
            return SchemaConverter.DataTypeIndex.Binary;

        throw new IllegalStateException("DataType " + type + "is not supported.");
    }
}