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
package com.epam.deltix.test.qsrv.hf.tickdb.topic;

import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.test.messages.MarketMessage;

/**
 * @author Alexei Osipov
 */
class StubData {


    static RecordClassDescriptor makeTradeMessageDescriptor() {
        RecordClassDescriptor marketMsgDescriptor = mkMarketMessageDescriptor(840);

        return (StreamConfigurationHelper.mkTradeMessageDescriptor(marketMsgDescriptor, "", 840, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO));
    }

    private static RecordClassDescriptor mkMarketMessageDescriptor(
            Integer staticCurrencyCode
    ) {
        final String name = MarketMessage.class.getName();
        final DataField[] fields = {
                new NonStaticDataField(
                        "originalTimestamp", "Original Time",
                        new IntegerDataType(IntegerDataType.ENCODING_INT64, false)),
                StreamConfigurationHelper.mkField(
                        "currencyCode", "Currency Code",
                        new IntegerDataType(IntegerDataType.ENCODING_INT16, true), null,
                        staticCurrencyCode
                )
        };

        return (new RecordClassDescriptor(name, name, true, null, fields));
    }
}
