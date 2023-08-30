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
import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.entries.*;
import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.entries.Package;
import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.orders.*;

public class QQLTypeLoader {

    public static class QueryTypeLoader extends MappingTypeLoader {

        private final Class<?> target;

        public QueryTypeLoader(Class<?> target) {
            this.target = target;
        }

        @Override
        public Class load(ClassDescriptor cd) throws ClassNotFoundException {
            if (target != null && cd.getName().startsWith("QUERY"))
                return target;

            return super.load(cd);
        }
    }


    final QueryTypeLoader loader;

    public QQLTypeLoader() {
        this(null);
    }

    public QQLTypeLoader(Class<?> clazz) {

        loader = new QueryTypeLoader(clazz);

        loader.bind("deltix.MyBarMessage", BarMessageExtended.class);
        loader.bind("deltix.entries.Package", Package.class);
        loader.bind("deltix.entries.PackageEntry", PackageEntry.class);
        loader.bind("deltix.entries.TradeEntry", TradeEntry.class);
        loader.bind("deltix.entries.PriceEntry", PriceEntry.class);
        loader.bind("deltix.entries.L1Entry", L1Entry.class);
        loader.bind("deltix.entries.L2Entry", L2Entry.class);

        loader.bind("deltix.Attribute", Attribute.class);
        loader.bind("deltix.AttributeId", AttributeId.class);
        loader.bind("deltix.ExtendedAttribute", ExtendedAttribute.class);
        loader.bind("deltix.CustomAttribute", CustomAttribute.class);
        loader.bind("deltix.FixAttribute", FixAttribute.class);
//
        loader.bind("deltix.orders.OrderEvent", OrderEvent.class);
        loader.bind("deltix.orders.Order", Order.class);
        loader.bind("deltix.orders.Id", Id.class);
        loader.bind("deltix.orders.ExternalId", ExternalId.class);
        loader.bind("deltix.orders.Execution", Execution.class);
        loader.bind("deltix.orders.ExecutionInfo", ExecutionInfo.class);
        loader.bind("deltix.orders.MarketOrder", MarketOrder.class);
        loader.bind("deltix.orders.OrderInfo", OrderInfo.class);
        loader.bind("deltix.orders.LimitOrderInfo", LimitOrderInfo.class);
        loader.bind("deltix.orders.MarketOrderInfo", MarketOrderInfo.class);
        loader.bind("deltix.orders.CommissionInfo", CommissionInfo.class);
        loader.bind("deltix.orders.LimitOrder", LimitOrder.class);
        loader.bind("deltix.orders.ExecutedInfo", ExecutedInfo.class);
        loader.bind("deltix.orders.ExecutedLimitOrderInfoA", ExecutedLimitOrderInfoA.class);
        loader.bind("deltix.orders.ExecutedLimitOrderInfoB", ExecutedLimitOrderInfoB.class);
        loader.bind("deltix.orders.ExecutedMarketOrderInfo", ExecutedMarketOrderInfo.class);


        loader.bind("Test1", Test1Msg.class);
        loader.bind("Test2", Test2Msg.class);
        loader.bind("Subclass1", Subclass1Msg.class);
        loader.bind("Subclass2", Subclass2Msg.class);
        loader.bind("Subclass3", Subclass3Msg.class);
    }

    public static QueryTypeLoader createTypeLoader() {
        return new QQLTypeLoader().loader;
    }

    public static QueryTypeLoader createTypeLoader(Class<?> cls) {
        return new QQLTypeLoader(cls).loader;
    }
}
