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
package com.epam.deltix.qsrv.test.messages;

import com.epam.deltix.timebase.messages.OldElementName;
import com.epam.deltix.timebase.messages.SchemaElement;

/**
 */
@OldElementName("deltix.qsrv.hf.pub.MarketEventType")
@SchemaElement(
        name = "deltix.timebase.api.messages.MarketEventType",
        title = "Market Event Type"
)
public enum MarketEventType {
    /**
     */
    @OldElementName("Bid")
    @SchemaElement(
            name = "BID"
    )
    BID(0),

    /**
     */
    @OldElementName("Offer")
    @SchemaElement(
            name = "OFFER"
    )
    OFFER(1),

    /**
     */
    @OldElementName("Trade")
    @SchemaElement(
            name = "TRADE"
    )
    TRADE(2),

    /**
     */
    @OldElementName("IndexValue")
    @SchemaElement(
            name = "INDEX_VALUE"
    )
    INDEX_VALUE(3),

    /**
     */
    @OldElementName("OpeningPrice")
    @SchemaElement(
            name = "OPENING_PRICE"
    )
    OPENING_PRICE(4),

    /**
     */
    @OldElementName("ClosingPrice")
    @SchemaElement(
            name = "CLOSING_PRICE"
    )
    CLOSING_PRICE(5),

    /**
     */
    @OldElementName("SettlementPrice")
    @SchemaElement(
            name = "SETTLEMENT_PRICE"
    )
    SETTLEMENT_PRICE(6),

    /**
     */
    @OldElementName("TradingSessionHighPrice")
    @SchemaElement(
            name = "TRADING_SESSION_HIGH_PRICE"
    )
    TRADING_SESSION_HIGH_PRICE(7),

    /**
     */
    @OldElementName("TradingSessionLowPrice")
    @SchemaElement(
            name = "TRADING_SESSION_LOW_PRICE"
    )
    TRADING_SESSION_LOW_PRICE(8),

    /**
     */
    @OldElementName("TradingSessionVWAPPrice")
    @SchemaElement(
            name = "TRADING_SESSION_VWAP_PRICE"
    )
    TRADING_SESSION_VWAP_PRICE(9),

    /**
     */
    @OldElementName("Imbalance")
    @SchemaElement(
            name = "IMBALANCE"
    )
    IMBALANCE(10),

    /**
     */
    @OldElementName("TradeVolume")
    @SchemaElement(
            name = "TRADE_VOLUME"
    )
    TRADE_VOLUME(11),

    /**
     */
    @OldElementName("OpenInterest")
    @SchemaElement(
            name = "OPEN_INTEREST"
    )
    OPEN_INTEREST(12),

    /**
     */
    @OldElementName("CompositeUnderlyingPrice")
    @SchemaElement(
            name = "COMPOSITE_UNDERLYING_PRICE"
    )
    COMPOSITE_UNDERLYING_PRICE(13),

    /**
     */
    @OldElementName("SimulatedSellPrice")
    @SchemaElement(
            name = "SIMULATED_SELL_PRICE"
    )
    SIMULATED_SELL_PRICE(14),

    /**
     */
    @OldElementName("SimulatedBuyPrice")
    @SchemaElement(
            name = "SIMULATED_BUY_PRICE"
    )
    SIMULATED_BUY_PRICE(15),

    /**
     */
    @OldElementName("MarginRate")
    @SchemaElement(
            name = "MARGIN_RATE"
    )
    MARGIN_RATE(16),

    /**
     */
    @OldElementName("MidPrice")
    @SchemaElement(
            name = "MID_PRICE"
    )
    MID_PRICE(17),

    /**
     */
    @OldElementName("EmptyBook")
    @SchemaElement(
            name = "EMPTY_BOOK"
    )
    EMPTY_BOOK(18),

    /**
     */
    @OldElementName("SettleHighPrice")
    @SchemaElement(
            name = "SETTLE_HIGH_PRICE"
    )
    SETTLE_HIGH_PRICE(19),

    /**
     */
    @OldElementName("SettleLowPrice")
    @SchemaElement(
            name = "SETTLE_LOW_PRICE"
    )
    SETTLE_LOW_PRICE(20),

    /**
     */
    @OldElementName("PriorSettlePrice")
    @SchemaElement(
            name = "PRIOR_SETTLE_PRICE"
    )
    PRIOR_SETTLE_PRICE(21),

    /**
     */
    @OldElementName("SessionHighBid")
    @SchemaElement(
            name = "SESSION_HIGH_BID"
    )
    SESSION_HIGH_BID(22),

    /**
     */
    @OldElementName("SessionLowOffer")
    @SchemaElement(
            name = "SESSION_LOW_OFFER"
    )
    SESSION_LOW_OFFER(23),

    /**
     */
    @OldElementName("EarlyPrices")
    @SchemaElement(
            name = "EARLY_PRICE"
    )
    EARLY_PRICE(24),

    /**
     */
    @OldElementName("AuctionClearingPrice")
    @SchemaElement(
            name = "AUCTION_CLEARING_PRICE"
    )
    AUCTION_CLEARING_PRICE(25),

    /**
     * Swap Value Factor (SVF) for swaps cleared through a central counterparty (CCP)
     */
    @OldElementName("SwapValueFactor")
    @SchemaElement(
            name = "SWAP_VALUE_FACTOR"
    )
    SWAP_VALUE_FACTOR(26),

    /**
     * value adjustment for long positions
     */
    @OldElementName("ValueAdjLong")
    @SchemaElement(
            name = "VALUE_ADJ_LONG"
    )
    VALUE_ADJ_LONG(27),

    /**
     * Cumulative Value Adjustment for long positions
     */
    @OldElementName("CumulativeValueAdjLong")
    @SchemaElement(
            name = "CUMMULATIVE_VALUE_ADJ_LONG"
    )
    CUMMULATIVE_VALUE_ADJ_LONG(28),

    /**
     * Daily Value Adjustment for Short Positions
     */
    @OldElementName("DailyValueAdjShort")
    @SchemaElement(
            name = "DAILY_VALUE_ADJ_SHORT"
    )
    DAILY_VALUE_ADJ_SHORT(29),

    /**
     * Cumulative Value Adjustment for Short Positions
     */
    @OldElementName("CumulativeValueAdjShort")
    @SchemaElement(
            name = "CUMMULATIVE_VALUE_ADJ_SHORT"
    )
    CUMMULATIVE_VALUE_ADJ_SHORT(30),

    /**
     */
    @OldElementName("FixingPrice")
    @SchemaElement(
            name = "FIXING_PRICE"
    )
    FIXING_PRICE(31),

    /**
     */
    @OldElementName("CashRate")
    @SchemaElement(
            name = "CASH_RATE"
    )
    CASH_RATE(32),

    /**
     */
    @OldElementName("RecoveryRate")
    @SchemaElement(
            name = "RECOVERY_RATE"
    )
    RECOVERY_RATE(33),

    /**
     * Recovery Rate for Long
     */
    @OldElementName("RecoveryRateLong")
    @SchemaElement(
            name = "RECOVERY_RATE_LONG"
    )
    RECOVERY_RATE_LONG(34),

    /**
     * Recovery Rate for Short
     */
    @OldElementName("RecoveryRateShort")
    @SchemaElement(
            name = "RECOVERY_RATE_SHORT"
    )
    RECOVERY_RATE_SHORT(35);

    private final int value;

    MarketEventType(int value) {
        this.value = value;
    }

    public int getNumber() {
        return this.value;
    }

    public static MarketEventType valueOf(int number) {
        switch (number) {
            case 0: return BID;
            case 1: return OFFER;
            case 2: return TRADE;
            case 3: return INDEX_VALUE;
            case 4: return OPENING_PRICE;
            case 5: return CLOSING_PRICE;
            case 6: return SETTLEMENT_PRICE;
            case 7: return TRADING_SESSION_HIGH_PRICE;
            case 8: return TRADING_SESSION_LOW_PRICE;
            case 9: return TRADING_SESSION_VWAP_PRICE;
            case 10: return IMBALANCE;
            case 11: return TRADE_VOLUME;
            case 12: return OPEN_INTEREST;
            case 13: return COMPOSITE_UNDERLYING_PRICE;
            case 14: return SIMULATED_SELL_PRICE;
            case 15: return SIMULATED_BUY_PRICE;
            case 16: return MARGIN_RATE;
            case 17: return MID_PRICE;
            case 18: return EMPTY_BOOK;
            case 19: return SETTLE_HIGH_PRICE;
            case 20: return SETTLE_LOW_PRICE;
            case 21: return PRIOR_SETTLE_PRICE;
            case 22: return SESSION_HIGH_BID;
            case 23: return SESSION_LOW_OFFER;
            case 24: return EARLY_PRICE;
            case 25: return AUCTION_CLEARING_PRICE;
            case 26: return SWAP_VALUE_FACTOR;
            case 27: return VALUE_ADJ_LONG;
            case 28: return CUMMULATIVE_VALUE_ADJ_LONG;
            case 29: return DAILY_VALUE_ADJ_SHORT;
            case 30: return CUMMULATIVE_VALUE_ADJ_SHORT;
            case 31: return FIXING_PRICE;
            case 32: return CASH_RATE;
            case 33: return RECOVERY_RATE;
            case 34: return RECOVERY_RATE_LONG;
            case 35: return RECOVERY_RATE_SHORT;
            default: return null;
        }
    }

    public static MarketEventType strictValueOf(int number) {
        final MarketEventType value = valueOf(number);
        if (value == null) {
            throw new IllegalArgumentException("Enumeration 'MarketEventType' does not have value corresponding to '" + number + "'.");
        }
        return value;
    }
}