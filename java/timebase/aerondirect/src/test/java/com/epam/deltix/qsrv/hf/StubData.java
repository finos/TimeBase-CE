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
package com.epam.deltix.qsrv.hf;

import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.qsrv.hf.topic.consumer.MappingProvider;
import com.epam.deltix.util.collections.generated.IntegerToObjectHashMap;

/**
 * @author Alexei Osipov
 */
public class StubData {


//    public static RecordClassDescriptor makeTradeMessageDescriptor ()
//    {
//        RecordClassDescriptor marketMsgDescriptor = mkMarketMessageDescriptor (840);
//
//
//        return (StreamConfigurationHelper.mkTradeMessageDescriptor (marketMsgDescriptor, "", 840, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO));
//    }
//
//    private static RecordClassDescriptor     mkMarketMessageDescriptor (
//            Integer                 staticCurrencyCode
//    )
//    {
//        final String            name = MarketMessage.class.getName ();
//        final DataField[]      fields = {
//                new NonStaticDataField(
//                        "originalTimestamp", "Original Time",
//                        new IntegerDataType(IntegerDataType.ENCODING_INT64, false)),
//                        StreamConfigurationHelper.mkField (
//                        "currencyCode", "Currency Code",
//                        new IntegerDataType (IntegerDataType.ENCODING_INT16, true), null,
//                        staticCurrencyCode
//                )
//        };
//
//        return (new RecordClassDescriptor (name, name, true, null, fields));
//    }

    public static MappingProvider getStubMappingProvider() {
        return new MappingProvider() {
            @Override
            public ConstantIdentityKey[] getMappingSnapshot() {
                return new ConstantIdentityKey[0];
            }

            @Override
            public IntegerToObjectHashMap<ConstantIdentityKey> getTempMappingSnapshot(int neededTempEntityIndex) {
                return new IntegerToObjectHashMap<>();
            }
        };
    }
}