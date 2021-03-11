package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell;

/**
 * @author Alexei Osipov
 */
// TODO: Rename?
public interface PlayerInterface {
    void play();

    void pause();

    void resume();

    void stop();

    void next();

    void setSpeed(double speed);

    void setTimeSliceDuration(int virtualTimeSliceDurationMs);

    void setStopAtTimestamp(Long stopAtTimestamp);

    void setLogMode(PlayerCommandProcessor.LogMode logMode);
}
