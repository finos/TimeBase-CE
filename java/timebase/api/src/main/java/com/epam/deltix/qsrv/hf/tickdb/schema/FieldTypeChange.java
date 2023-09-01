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

import com.epam.deltix.qsrv.hf.pub.md.*;

public class FieldTypeChange extends FieldChange {

    protected FieldTypeChange() { } // for jaxb

    public FieldTypeChange(DataField source, DataField target) {
        super(source, target, FieldAttribute.DataType);
    }

    @Override
    public Impact getChangeImpact() {
        
        DataType srcType = getSource().getType();
        DataType trgType = getTarget().getType();

        if (srcType.isNullable() && !trgType.isNullable())
            return Impact.DataLoss;

        return srcType.isConvertible(trgType) ==
                    DataType.ConversionType.Lossless ? Impact.DataConvert : Impact.DataLoss;
    }

    public boolean isDefaultValueRequired() {

        DataType srcType = getSource().getType();
        DataType trgType = getTarget().getType();

        if (srcType.isNullable() && !trgType.isNullable())
            return true;

        return srcType.isConvertible(trgType) != DataType.ConversionType.Lossless;
    }

    public boolean isBoundsChanged() {
        DataType srcType = getSource().getType();
        DataType trgType = getTarget().getType();

        return srcType.isConvertible(trgType) == DataType.ConversionType.Lossy;
    }

    public void setDefaultValue(String value) {
        this.resolution = ErrorResolution.resolve(value);
    }

    public String getDefaultValue() {
        return resolution != null ? resolution.defaultValue : null;
    }

    public boolean          hasErrors() {
        return getChangeImpact() == Impact.DataLoss && resolution == null;
    }
    
    /*
        Messages with conversion errors will be ignored
     */
    public void setIgnoreErrors() {
        this.resolution = ErrorResolution.ignore();
    }

//    public ResolverEncoder getResolverEncoder(UnboundEncoder base) {
//        if (errorResolver != null)
//            return errorResolver.getEncoder(base);
//        else if (defaultResolver != null)
//            return defaultResolver.getEncoder(base);
//
//        return null;
//    }
//
//    @Override
//    ErrorResolver           getErrorResolver() {
//        return errorResolver != null ? errorResolver : defaultResolver;
//    }

    @Override
    public boolean          equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FieldTypeChange that = (FieldTypeChange) o;

        if (source != null ? !source.equals(that.source) : that.source != null) return false;
        if (target != null ? !target.equals(that.target) : that.target != null) return false;

        return true;
    }

    @Override
    public String toString() {

        return "\"" + attribute.toString() + "\" changed from " +
                toShortString(source.getType()) + " to " + toShortString(target.getType());
    }

    public String toShortString(DataType type) {
        String typeName = type.getBaseName() + (type.getEncoding() != null ? ":" + type.getEncoding() : "");

        StringBuilder sb = new StringBuilder("[" + typeName + "; nullable=" + type.isNullable());
        if (type instanceof FloatDataType) {
            FloatDataType dataType = (FloatDataType) type;

            if (dataType.getMin() != null || dataType.getMax() != null) {
                sb.append(";range=").append(dataType.getMin() != null ? dataType.getMin() : "").append(":").append(dataType.getMax() != null ? dataType.getMax() : "");
            }
        } else if (type instanceof IntegerDataType) {
            IntegerDataType dataType = (IntegerDataType) type;

            if (dataType.getMin() != null || dataType.getMax() != null) {
                sb.append(";range=").append(dataType.getMin() != null ? dataType.getMin() : "").append(":").append(dataType.getMax() != null ? dataType.getMax() : "");
            }
        }

        return sb.append("]").toString();
    }

    @Override
    public int              hashCode() {
        int result = source != null ? source.hashCode() : 0;
        result = 31 * result + (target != null ? target.hashCode() : 0);
        result = 31 * result + (resolution != null ? resolution.hashCode() : 0);
        return result;
    }
}