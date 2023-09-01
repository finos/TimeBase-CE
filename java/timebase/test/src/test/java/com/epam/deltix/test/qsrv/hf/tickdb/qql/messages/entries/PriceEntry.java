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

import com.epam.deltix.timebase.messages.SchemaElement;

@SchemaElement(name = "deltix.entries.PriceEntry")
public class PriceEntry extends PackageEntry {

    protected float price;
    protected float size;

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

    public PackageEntry copyFrom(PackageEntry source) {
        super.copyFrom(source);

        if (source instanceof PriceEntry) {
            final PriceEntry obj = (PriceEntry) source;
            price = obj.price;
            size = obj.size;
        }
        return this;
    }

    @Override
    public String toString() {
        return "PriceEntry{" +
            "price=" + price +
            ", size=" + size +
            '}';
    }
}
