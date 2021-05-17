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
import com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField;

import javax.xml.bind.annotation.XmlElement;

public class DeleteFieldChange extends AbstractFieldChange {

    @XmlElement
    private boolean hasImpact = false;

    protected DeleteFieldChange() { } // for jaxb

    public DeleteFieldChange(NonStaticDataField field) {
        this(field, true);        
    }

    public DeleteFieldChange(DataField field) {
        this(field, true);
    }

    private DeleteFieldChange(DataField field, boolean hasImpact) {
        super(field, null);
        this.hasImpact = hasImpact;
    }

    public Impact getChangeImpact() {
        if (source instanceof StaticDataField)
            return Impact.None;
        
        return hasImpact ? Impact.DataConvert : Impact.None;
    }

    public boolean hasErrors() {
        return false;
    }
}
