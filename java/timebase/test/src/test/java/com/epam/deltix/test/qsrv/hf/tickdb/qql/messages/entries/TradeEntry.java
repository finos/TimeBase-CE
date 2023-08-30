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

package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.entries;

import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.AggressorSide;
import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.Attribute;
import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.CustomAttribute;
import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.FixAttribute;
import com.epam.deltix.timebase.messages.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

@SchemaElement(name = "deltix.entries.TradeEntry")
public class TradeEntry extends PackageEntry {

    private float price;
    private float size;
    private AggressorSide side;
    private ObjectArrayList<Attribute> attributes = new ObjectArrayList<>();

    @SchemaElement(title = "price")
    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    @SchemaElement(title = "size")
    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    @SchemaElement(title = "side")
    public AggressorSide getSide() {
        return side;
    }

    public void setSide(AggressorSide side) {
        this.side = side;
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

    public PackageEntry copyFrom(PackageEntry source) {
        super.copyFrom(source);

        if (source instanceof TradeEntry) {
            final TradeEntry obj = (TradeEntry) source;
            price = obj.price;
            size = obj.size;
            side = obj.side;
            attributes.addAll(obj.attributes);
        }
        return this;
    }

    @Override
    public String toString() {
        return "TradeEntry{" +
            "price=" + price +
            ", size=" + size +
            ", side=" + side +
            '}';
    }
}
