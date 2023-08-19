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

package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell;

import com.epam.deltix.qsrv.hf.pub.ExchangeCodec;
import com.epam.deltix.timebase.messages.*;

/**
 * Basic information about a bar - contains open, high, low, close and volume attributes
 */
@SchemaElement(name = "com.epam.deltix.timebase.messages.BarMessage")
public class SimpleBarMessage extends InstrumentMessage {

    @SchemaElement(title = "Exchange Code")
    @SchemaType(encoding = "ALPHANUMERIC(10)", dataType = SchemaDataType.VARCHAR)
    public long exchangeCode = ExchangeCodec.NULL;

    @SchemaElement(title = "Close")
    public double close;

    @RelativeTo("close")
    @SchemaElement(title = "Open")
    public double open;

    @RelativeTo("close")
    @SchemaElement(title = "High")
    public double high;

    @RelativeTo("close")
    @SchemaElement(title = "Low")
    public double low;

    @SchemaElement(title = "Volume")
    public double volume;

    @Override
    public String toString() {
        return "SimpleBarMessage {" +
                ", exchangeCode=" + exchangeCode +
                ", close=" + close +
                ", open=" + open +
                ", high=" + high +
                ", low=" + low +
                ", volume=" + volume +
                '}';
    }
}
