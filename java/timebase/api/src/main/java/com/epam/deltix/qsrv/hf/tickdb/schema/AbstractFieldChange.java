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

import javax.xml.bind.annotation.XmlElement;

public abstract class AbstractFieldChange implements SchemaChange {

    protected AbstractFieldChange() { } // for jaxb

    @XmlElement
    protected DataField source;
    @XmlElement
    protected DataField target;
    
    @XmlElement
    protected ErrorResolution   resolution;

    @XmlElement
    protected FieldAttribute    attribute;

    protected AbstractFieldChange(DataField source, DataField target) {
        this.source = source;
        this.target = target;
    }

    protected AbstractFieldChange(DataField source, DataField target, FieldAttribute attr) {
        this.source = source;
        this.target = target;
        this.attribute = attr;
    }

    public DataField        getSource() {
        return source;
    }

    public DataField        getTarget() {
        return target;
    }

    public FieldAttribute   getAttribute() {
        return attribute;
    }

    public void             setAttribute(FieldAttribute attribute) {
        this.attribute = attribute;
    }

    public abstract boolean hasErrors();

    public ErrorResolution  getResolution() {
        return resolution;
    }

    public static String valueOf(DataField field, FieldAttribute attr) {

        switch (attr) {
            case Title:
                return field.getTitle();
            case Name:
                return field.getName();
            case Description:
                return field.getDescription();
            case DataType:
                return field.getType().getEncoding();
            case PrimaryKey:
                return String.valueOf(((NonStaticDataField) field).isPk());
            case StaticValue:
                return ((StaticDataField)field).getStaticValue();
            case Relation:
                return ((NonStaticDataField)field).getRelativeTo();

            default:
                throw new IllegalArgumentException("Undefined Field Attribute: " + attr);

        }
    }

}