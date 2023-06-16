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
package com.epam.deltix.qsrv.hf.tickdb.schema;

import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.md.StaticDataField;

import javax.xml.bind.annotation.XmlElement;

public class CreateFieldChange extends AbstractFieldChange {

    @XmlElement
    private boolean hasImpact = false;

    protected CreateFieldChange() {} // for jaxb

    public CreateFieldChange(StaticDataField field) {
        this(field, false);
        setInitialValue(field.getStaticValue());
    }

    public CreateFieldChange(DataField field, boolean hasImpact) {
        super(null, field);
        this.hasImpact = hasImpact;
        if (field instanceof StaticDataField) {
            setInitialValue(((StaticDataField) field).getStaticValue());
        }
    }

    public Impact getChangeImpact() {
        if (getTarget() instanceof StaticDataField)
            return Impact.None;

        if (getInitialValue() == null)
            return getTarget().getType().isNullable() && !hasImpact ? Impact.None : Impact.DataConvert;
        
        return Impact.DataConvert;
    }

    public boolean hasErrors() {
        if (!getTarget().getType().isNullable())
            return getInitialValue() == null;

        return resolution == null;
    }

    public void setInitialValue(String value) {
        this.resolution = ErrorResolution.resolve(value);
    }

    public String getInitialValue() {
        return resolution != null ? resolution.defaultValue : null;
    }
}