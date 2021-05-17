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

/**
 Indicates static to non-static change and visa-versa
 */
public class FieldModifierChange extends AbstractFieldChange {

    protected FieldModifierChange() { } // for jaxb

    @XmlElement
    private CreateFieldChange create;

    @XmlElement
    private DeleteFieldChange delete;
    
    public FieldModifierChange(DataField source,
                               DataField target, boolean tHasImpact) {
        super(source, target);
        this.create = new CreateFieldChange(target, tHasImpact);
        this.delete = new DeleteFieldChange(source);
    }

    public Impact getChangeImpact() {
        if (source instanceof StaticDataField)
            return create.getChangeImpact();
        else
            return delete.getChangeImpact();
    }

    public boolean hasErrors() {
       return create.hasErrors();
    }

    public void setInitialValue(String value) {
        create.setInitialValue(value);
        resolution = create.resolution;
    }

    public String getInitialValue() {
        return create.getInitialValue();
    }
}
