package com.epam.deltix.qsrv.hf.tickdb.http;

import com.epam.deltix.timebase.messages.IdentityKey;

import java.util.Objects;

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

    public InstrumentIdentityKey(String symbol) {
        this.instrumentType = "CUSTOM";
        this.symbol = symbol;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InstrumentIdentityKey that = (InstrumentIdentityKey) o;
        return Objects.equals(instrumentType, that.instrumentType) && Objects.equals(symbol, that.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instrumentType, symbol);
    }
}
