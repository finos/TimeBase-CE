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
package com.epam.deltix.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.pub.CurrencyCodec;
import com.epam.deltix.qsrv.hf.pub.ExchangeCodec;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.qsrv.test.messages.BestBidOfferMessage;
import com.epam.deltix.qsrv.test.messages.MarketMessage;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamScope;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.util.currency.CurrencyCodeList;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * User: BazylevD
 * Date: Apr 16, 2009
 * Time: 4:22:32 PM
 */
public class TickDBUtil {
    public static String USER_HOME = System.getProperty("user.home") + "\\";

    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    Callback callback;
    private final StringBuilder sb = new StringBuilder();

    interface Callback {
        void callback(InstrumentMessage msg, TickCursor cursor);
    }

    public TickDBUtil() {
        callback = null;
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public String           toString(TickCursor cursor) {
        final StringBuilder sb = new StringBuilder();
        printCursor(cursor, sb);
        return sb.toString ();
    }

    private void printCursor(TickCursor cur, StringBuilder out) {
        if (cur == null) {
            System.out.println("NO DATA");
        } else {
            while (cur.next()) {
                InstrumentMessage msg = cur.getMessage();
                if (callback != null)
                    callback.callback(msg, cur);

                print(msg, out);
            }
        }
    }

    public void print(InstrumentMessage msg, StringBuilder out) {

        if (out.length() > 0)
            out.append(System.lineSeparator());

        if (msg instanceof RawMessage) {
            out.append(msg.toString());
            return;
        }

        out.append(String.format(
                "%s,%s,%s,%d",
                msg.getClass().getSimpleName(),
                msg.getSymbol(),
                df.format(msg.getTimeStampMs()),
                msg.getTimeStampMs()
        ));

        if (msg instanceof MarketMessage) {
            MarketMessage mm = (MarketMessage) msg;
            appendCurrency(mm, out); //TODO: rebuild

            if (msg instanceof BestBidOfferMessage) {
                BestBidOfferMessage bbo = (BestBidOfferMessage) msg;

                out.append(String.format(
                    ",%.2f,%.2f,%s,%.2f,%.2f,%s",
                        bbo.getBidPrice(),
                        bbo.getBidSize(),
                    ExchangeCodec.longToCode(bbo.getBidExchangeId()),
                        bbo.getOfferPrice(),
                        bbo.getOfferSize(),
                    ExchangeCodec.longToCode(bbo.getOfferExchangeId())
                ));
            } else if (msg instanceof TradeMessage) {
                TradeMessage trade = (TradeMessage) msg;

                out.append(String.format(
                        ",%s,%.2f,%.2f",
                        ExchangeCodec.longToCode(trade.getExchangeId()),
                        trade.getPrice(),
                        trade.getSize()
                ));
            } else if (msg instanceof BarMessage) {
                BarMessage bar = (BarMessage) msg;

                out.append(String.format(
                        ",%s,%.2f,%.2f,%.2f,%.2f,%.2f",
                        ExchangeCodec.longToCode(bar.getExchangeId()),
                        bar.getOpen(),
                        bar.getHigh(),
                        bar.getLow(),
                        bar.getClose(),
                        bar.getVolume()
                ));
            }
        }

    }



    // the method is not thread safe, because it uses sb instance 
    public String toString(InstrumentMessage msg) {
        if (msg instanceof RawMessage) {
            return msg.toString();
        }

        sb.setLength(0);
        sb.append(
        String.format(
                "%s,%s,%s,%d",
                msg.getClass().getSimpleName(),
                msg.getSymbol(),
                df.format(msg.getTimeStampMs()),
                msg.getTimeStampMs()
        ));

        if (msg instanceof MarketMessage) {
            MarketMessage mm = (MarketMessage) msg;

            appendCurrency(mm, sb);

            if (msg instanceof BestBidOfferMessage) {
                BestBidOfferMessage bbo = (BestBidOfferMessage) msg;

                sb.append(
                String.format(
                    ",%.2f,%.2f,%s,%.2f,%.2f,%s",
                        bbo.getBidPrice(),
                        bbo.getBidSize(),
                        ExchangeCodec.longToCode(bbo.getBidExchangeId()),
                        bbo.getOfferPrice(),
                        bbo.getOfferSize(),
                        ExchangeCodec.longToCode(bbo.getOfferExchangeId())
                ));
            } else if (msg instanceof TradeMessage) {
                TradeMessage trade = (TradeMessage) msg;

                sb.append(
                String.format(
                        ",%s,%.2f,%.2f",
                        ExchangeCodec.longToCode(trade.getExchangeId()),
                        trade.getPrice(),
                        trade.getSize()
                ));
            } else if (msg instanceof BarMessage) {
                BarMessage bar = (BarMessage) msg;

                sb.append(
                String.format(
                        ",%s,%.2f,%.2f,%.2f,%.2f,%.2f",
                        ExchangeCodec.longToCode(bar.getExchangeId()),
                        bar.getOpen(),
                        bar.getHigh(),
                        bar.getLow(),
                        bar.getClose(),
                        bar.getVolume()
                ));
            }
        }

        return sb.toString();
    }

    public void appendCurrency(MarketMessage msg, StringBuilder sb) {
        short code = msg.getCurrencyCode();
        CurrencyCodeList.CurrencyInfo info = CurrencyCodeList.getInfoByNumeric(code);
        // but it may contain CurrencyCodec encoded value
        if (info == null)
            info = CurrencyCodec.getCurrencyCode(code);

        sb.append(
                String.format(
                        ",%s",
                        info != null ? info.numericCode : code
                ));
    }

    public static StreamOptions transientTradeStreamOptions () {
        StreamOptions   options = new StreamOptions (StreamScope.TRANSIENT, null, null, StreamOptions.MAX_DISTRIBUTION);

        options.setFixedType (StreamConfigurationHelper.mkUniversalTradeMessageDescriptor ());

        return (options);
    }
}