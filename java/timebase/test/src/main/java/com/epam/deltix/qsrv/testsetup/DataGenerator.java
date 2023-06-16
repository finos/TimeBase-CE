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
package com.epam.deltix.qsrv.testsetup;

import com.epam.deltix.qsrv.test.messages.BestBidOfferMessage;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.timebase.messages.InstrumentMessage;


import java.util.GregorianCalendar;

public class DataGenerator extends BaseGenerator<InstrumentMessage> {
    protected int count;
    protected TradeMessage trade = new TradeMessage();
    protected BestBidOfferMessage bbo1 = new BestBidOfferMessage();
    protected BestBidOfferMessage bbo2 = new BestBidOfferMessage();

    public DataGenerator(GregorianCalendar calendar, int interval, String ... symbols) {
        super(calendar, interval, symbols);

        trade.setPrice(nextDouble());
        trade.setSize(rnd.nextInt(1000));
        trade.setCurrencyCode((short)840);

        bbo1.setBidPrice(nextDouble());
        bbo1.setBidSize(rnd.nextInt(1000));

        bbo2.setOfferPrice(nextDouble());
        bbo2.setOfferSize(rnd.nextInt(1000));
    }

    public boolean next() {

        InstrumentMessage message;

        if (count % 10 == 0) {
            message = trade;
        } else {
            if (count % 2 == 0)
                message = bbo1;
            else
                message = bbo2;
        }

        message.setSymbol(symbols.next());
        message.setTimeStampMs(symbols.isReseted() ? getNextTime() : getActualTime());

        current = message;
        count++;
        return true;
    }

    public boolean isAtEnd() {
        return false;
    }
}