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
package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.RecordInfo;
import com.epam.deltix.timebase.messages.SchemaElement;
import com.epam.deltix.timebase.messages.TypeConstants;

import java.util.Objects;

public class SimpleTradeMessage extends InstrumentMessage {

    protected double price = TypeConstants.IEEE64_NULL;

    protected double size = TypeConstants.IEEE64_NULL;

    protected long count = 0;

    @SchemaElement
    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    @SchemaElement
    public double getSize() {
        return size;
    }

    public void setSize(double size) {
        this.size = size;
    }

    @SchemaElement
    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    @Override
    public SimpleTradeMessage clone() {
        SimpleTradeMessage t = createInstance();
        t.copyFrom(this);
        return t;
    }

    @Override
    protected SimpleTradeMessage createInstance() {
        return new SimpleTradeMessage();
    }

    @Override
    public SimpleTradeMessage copyFrom(RecordInfo template) {
        super.copyFrom(template);
        if (template instanceof SimpleTradeMessage) {
            SimpleTradeMessage t = (SimpleTradeMessage)template;
            setPrice(t.getPrice());
            setSize(t.getSize());
            setCount(t.getCount());
        }
        return this;
    }

    @Override
    public String toString() {
        return "SimpleTradeMessage{" +
            "price=" + price +
            ", size=" + size +
            ", count=" + count +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SimpleTradeMessage that = (SimpleTradeMessage) o;
        return Double.compare(price, that.price) == 0 && Double.compare(size, that.size) == 0 && count == that.count;
    }

    @Override
    public int hashCode() {
        return Objects.hash(price, size, count);
    }
}

