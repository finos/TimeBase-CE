package com.epam.deltix.qsrv.hf.tickdb.web.model.monitor;

public class TrackDto {

    private boolean on;

    public TrackDto() {
    }

    public TrackDto(boolean on) {
        this.on = on;
    }

    public boolean isOn() {
        return on;
    }

    public void setOn(boolean on) {
        this.on = on;
    }
}
