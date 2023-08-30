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

import com.epam.deltix.qsrv.hf.pub.MappingTypeLoader;
import com.epam.deltix.qsrv.test.messages.BestBidOfferMessage;
import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.entries.*;
import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.entries.Package;
import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.orders.*;

public class TypeLoader {
    public static MappingTypeLoader TYPE_LOADER = new MappingTypeLoader();

    static {
        TYPE_LOADER.bind("BarMessageExtended", BarMessageExtended.class);
        TYPE_LOADER.bind("deltix.entries.Package", Package.class);
        TYPE_LOADER.bind("deltix.entries.PackageEntry", PackageEntry.class);
        TYPE_LOADER.bind("deltix.entries.TradeEntry", TradeEntry.class);
        TYPE_LOADER.bind("deltix.entries.PriceEntry", PriceEntry.class);
        TYPE_LOADER.bind("deltix.entries.L1Entry", L1Entry.class);
        TYPE_LOADER.bind("deltix.entries.L2Entry", L2Entry.class);

        TYPE_LOADER.bind("deltix.Attribute", Attribute.class);
        TYPE_LOADER.bind("deltix.AttributeId", AttributeId.class);
        TYPE_LOADER.bind("deltix.ExtendedAttribute", ExtendedAttribute.class);
        TYPE_LOADER.bind("deltix.CustomAttribute", CustomAttribute.class);
        TYPE_LOADER.bind("deltix.FixAttribute", FixAttribute.class);

        TYPE_LOADER.bind("deltix.orders.OrderEvent", OrderEvent.class);
        TYPE_LOADER.bind("deltix.orders.Order", Order.class);
        TYPE_LOADER.bind("deltix.orders.Id", Id.class);
        TYPE_LOADER.bind("deltix.orders.ExternalId", ExternalId.class);
        TYPE_LOADER.bind("deltix.orders.Execution", Execution.class);
        TYPE_LOADER.bind("deltix.orders.ExecutionInfo", ExecutionInfo.class);
        TYPE_LOADER.bind("deltix.orders.MarketOrder", MarketOrder.class);
        TYPE_LOADER.bind("deltix.orders.OrderInfo", OrderInfo.class);
        TYPE_LOADER.bind("deltix.orders.LimitOrderInfo", LimitOrderInfo.class);
        TYPE_LOADER.bind("deltix.orders.MarketOrderInfo", MarketOrderInfo.class);
        TYPE_LOADER.bind("deltix.orders.CommissionInfo", CommissionInfo.class);
        TYPE_LOADER.bind("deltix.orders.LimitOrder", LimitOrder.class);
        TYPE_LOADER.bind("deltix.orders.ExecutedInfo", ExecutedInfo.class);
        TYPE_LOADER.bind("deltix.orders.ExecutedLimitOrderInfoA", ExecutedLimitOrderInfoA.class);
        TYPE_LOADER.bind("deltix.orders.ExecutedLimitOrderInfoB", ExecutedLimitOrderInfoB.class);
        TYPE_LOADER.bind("deltix.orders.ExecutedMarketOrderInfo", ExecutedMarketOrderInfo.class);

        TYPE_LOADER.bind("deltix.timebase.api.messages.BestBidOfferMessage", BestBidOfferMessage.class);
    }
}
