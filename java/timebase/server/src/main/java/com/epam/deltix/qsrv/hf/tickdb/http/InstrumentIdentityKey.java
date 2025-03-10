package com.epam.deltix.qsrv.hf.tickdb.http;

import com.epam.deltix.timebase.messages.IdentityKey;

/**
 * The class was added for compatibility with dxapi protocol.
 */
public class InstrumentIdentityKey implements IdentityKey {

    private String instrumentType;

    private String symbol;

    // For JAXB
    public InstrumentIdentityKey() {
        symbol = null;
    }

    public InstrumentIdentityKey(String instrumentType, String symbol) {
        this.instrumentType = instrumentType;
        this.symbol = symbol;
    }

    public String getInstrumentType() {
        return instrumentType;
    }

    @Override
    public CharSequence getSymbol() {
        return symbol;
    }
}
