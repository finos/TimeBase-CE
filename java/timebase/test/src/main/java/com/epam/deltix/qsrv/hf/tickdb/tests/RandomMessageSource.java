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
package com.epam.deltix.qsrv.hf.tickdb.tests;

import com.epam.deltix.qsrv.test.messages.*;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import java.util.Random;

public class RandomMessageSource implements MessageSource<InstrumentMessage> {

    private final Random random = new Random(System.currentTimeMillis());
    private final TradeMessage tradeMessage = new TradeMessage();
    private final BarMessage barMessage = new BarMessage();
    private final BestBidOfferMessage bboMessage = new BestBidOfferMessage();
    private final String[] symbols;

    private long count = 0;

    public RandomMessageSource() {
        this(1000);
    }

    public RandomMessageSource(int symbols) {
        this.symbols = TestUtils.getRandomStrings(symbols);
    }

    @Override
    public InstrumentMessage getMessage() {
        int n = random.nextInt(3);
        switch (n) {
            case 0:
                return getBarMessage();
            case 1:
                return getBBOMessage();
            case 2:
                return getTradeMessage();
        }
        return null;
    }

    private void setInstrumentMessage(InstrumentMessage instrumentMessage) {
        instrumentMessage.setSymbol(getSymbol());
    }

    private void setMarketMessage(MarketMessage marketMessage) {
        setInstrumentMessage(marketMessage);
        marketMessage.setCurrencyCode((short) random.nextInt(2000));
        marketMessage.setOriginalTimestamp(System.currentTimeMillis());
        marketMessage.setSequenceNumber(count++);
    }

    private TradeMessage getTradeMessage() {
        setMarketMessage(tradeMessage);
        tradeMessage.setNetPriceChange(random.nextDouble() * 1000);
        tradeMessage.setCondition("hej");
        tradeMessage.setAggressorSide(AggressorSide.values()[random.nextInt(AggressorSide.values().length)]);
        tradeMessage.setSize(random.nextDouble() * 1000);
        tradeMessage.setPrice(random.nextDouble() * 1000);
        tradeMessage.setExchangeId(random.nextInt(4242));
        tradeMessage.setEventType(MarketEventType.values()[random.nextInt(MarketEventType.values().length)]);
        return tradeMessage;
    }

    private BarMessage getBarMessage() {
        setMarketMessage(barMessage);
        barMessage.setOpen(random.nextDouble() * 1000);
        barMessage.setClose(random.nextDouble() * 1000);
        barMessage.setHigh(random.nextDouble() * 1000);
        barMessage.setLow(random.nextDouble() * 1000);
        barMessage.setVolume(random.nextDouble() * 1_000_000);
        barMessage.setExchangeId(random.nextInt(4242));
        return barMessage;
    }

    private BestBidOfferMessage getBBOMessage() {
        setMarketMessage(bboMessage);
        bboMessage.setBidExchangeId(random.nextInt(4242));
        bboMessage.setBidNumOfOrders(random.nextInt(1000));
        bboMessage.setBidPrice(random.nextDouble() * 1000);
        bboMessage.setBidQuoteId("test");
        bboMessage.setBidSize(random.nextDouble() * 1000);
        bboMessage.setIsNational(random.nextBoolean());
        bboMessage.setOfferExchangeId(random.nextInt(4242));
        bboMessage.setOfferNumOfOrders(random.nextInt(1000));
        bboMessage.setOfferSize(random.nextDouble() * 1000);
        bboMessage.setOfferPrice(random.nextDouble() * 1000);
        bboMessage.setOfferQuoteId("test");
        return bboMessage;
    }

    private String getSymbol() {
        return symbols[random.nextInt(symbols.length)];
    }

    @Override
    public boolean next() {
        return true;
    }

    @Override
    public boolean isAtEnd() {
        return false;
    }

    @Override
    public void close() { }
}