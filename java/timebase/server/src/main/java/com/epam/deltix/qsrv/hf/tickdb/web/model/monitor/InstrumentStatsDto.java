package com.epam.deltix.qsrv.hf.tickdb.web.model.monitor;

public class InstrumentStatsDto extends ChannelStatsDto {

    private String symbol;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

}
