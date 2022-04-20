package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class SymbolLimits {
    private List<String> symbols = new ArrayList<>();
    private boolean subscribeAll;

    public SymbolLimits() {

    }

    public SymbolLimits(boolean subscribeAll) {
        this.subscribeAll = subscribeAll;
    }

    public SymbolLimits(boolean subscribeAll, String symbol) {
        this.subscribeAll = subscribeAll;
        this.symbols.add(symbol);
    }

    public SymbolLimits(boolean subscribeAll, List<String> symbols) {
        this.subscribeAll = subscribeAll;
        this.symbols.addAll(symbols);
    }

    public void addSymbol(String symbol) {
        symbols.add(symbol);
    }

    public List<String> symbols() {
        return symbols;
    }

    public boolean isSubscribeAll() {
        return subscribeAll;
    }

    public void setSubscribeAll(boolean subscribeAll) {
        this.subscribeAll = subscribeAll;
    }
}
