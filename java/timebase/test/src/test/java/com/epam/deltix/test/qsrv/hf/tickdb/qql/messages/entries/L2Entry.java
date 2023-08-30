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

@SchemaElement(name = "deltix.entries.L2Entry")
public class L2Entry extends PriceEntry {

    private int level;
    private QuoteSide side;

    @SchemaElement(title = "level")
    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    @SchemaElement(title = "side")
    public QuoteSide getSide() {
        return side;
    }

    public void setSide(QuoteSide side) {
        this.side = side;
    }

    public PackageEntry copyFrom(PackageEntry source) {
        super.copyFrom(source);

        if (source instanceof L2Entry) {
            final L2Entry obj = (L2Entry) source;
            level = obj.level;
            side = obj.side;
        }
        return this;
    }

    @Override
    public String toString() {
        return "L2Entry{" +
            "level=" + level +
            ", side=" + side +
            ", price=" + price +
            ", size=" + size +
            '}';
    }
}
