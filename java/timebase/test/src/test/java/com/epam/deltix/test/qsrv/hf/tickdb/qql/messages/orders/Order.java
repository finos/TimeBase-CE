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

package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.orders;

import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.Attribute;
import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.CustomAttribute;
import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.FixAttribute;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.SchemaArrayType;
import com.epam.deltix.timebase.messages.SchemaElement;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

@SchemaElement(name = "deltix.orders.Order")
public class Order extends InstrumentMessage {

    private float sequence;
    protected Id id;
    private ObjectArrayList<Attribute> attributes = new ObjectArrayList<>();

    @SchemaElement(title = "sequence")
    public float getSequence() {
        return sequence;
    }

    public void setSequence(float sequence) {
        this.sequence = sequence;
    }

    @SchemaElement(title = "id")
    public Id getId() {
        return id;
    }

    public void setId(Id id) {
        this.id = id;
    }

    @SchemaElement(title = "attributes")
    @SchemaArrayType(
        isNullable = false,
        isElementNullable = false,
        elementTypes =  {
            CustomAttribute.class, FixAttribute.class
        }
    )
    public ObjectArrayList<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(ObjectArrayList<Attribute> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return "Order{" +
            "sequence=" + sequence +
            ", id=" + id +
            '}';
    }

}
