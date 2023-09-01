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

import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundEncoder;

public class FieldPositionChange extends AbstractFieldChange {

    protected FieldPositionChange() { } // for JAXB

    public FieldPositionChange(DataField source, DataField target) {
        super(source, target);
    }

    public boolean hasErrors() {
        return false;
    }

    public Impact getChangeImpact() {
        return Impact.DataConvert;
    }

    @Override
    public String toString() {
        return "Field " + source.getName() + " position changed.";
    }
}