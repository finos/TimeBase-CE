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
