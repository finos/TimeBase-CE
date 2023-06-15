/*
 * Copyright 2021 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.virtualplayer.VirtualPlayer;
import com.epam.deltix.util.cmdline.ShellCommandException;
import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;

/**
 * Processes shell commands for {@link RealtimePlayer} and {@link VirtualPlayer}.
 *
 * @author Alexei Osipov
 */
public class PlayerCommandProcessor {

    private static final String VIRTUAL_PLAYER = "virtual";
    private static final String REALTIME_PLAYER = "realtime";


    public enum LogMode {
        off,
        time,
        data
    }

    @SuppressWarnings("WeakerAccess")
    public static class Config {
        public final Multimap<String, String> streamMapping = HashMultimap.create();
        public String player = REALTIME_PLAYER;
        public double speed = 1;
        public int virtualTimeSliceDurationMs = 1000;
        public Integer timeQueueSize = null;
        public String timerStreamName = "timer";
        public Long startFromVirtualTimestamp = null;
        public Long destinationVirtualTimestamp = null;
        public Long stopAtTimestamp = null;
        public long time = Long.MIN_VALUE;
        public LogMode logMode = LogMode.time;
        public boolean cyclic = false;
    }

    private final Config config = new Config();

    private final TickDBShell shell;

    private PlayerInterface activePlayer = null;

    public PlayerCommandProcessor(TickDBShell shell) {
        this.shell = shell;
    }

    public boolean doCommand(String key, String args) {
        // TODO: Adapt exception handling to shell API

        switch (key.toLowerCase()) {
            case "playback":
                return processPlaybackCommand(args);

            // Control commands
            case "play":
                play();
                return true;
            case "stop":
                stop();
                return true;
            case "pause":
                pause();
                return true;
            case "resume":
                resume();
                return true;
            case "next":
                next();
                return true;

            default:
                return false;
        }
    }

    private boolean processPlaybackCommand(String args) {
        if (StringUtils.isBlank(args)) {
            return false;
        }
        String[] parts = args.split(" ", 2);
        String subCommand = parts[0];
        String[] streams = parts.length == 2 ? shell.splitSymbols(parts[1]) : new String[0];
        switch (subCommand) {
            case "add":
                assertArgumentCount(streams, 2);
                config.streamMapping.put(streams[0], streams[1]);
                return true;
            case "remove":
                assertArgumentCount(streams, 2);
                config.streamMapping.remove(streams[0], streams[1]);
                return true;
            case "clear":
                assertArgumentCount(streams, 0);
                config.streamMapping.clear();
                return true;
            default:
                return false;
        }

    }

    private void assertArgumentCount(String[] parts, int expectedArgumentCount) {
        int argumentCount = parts.length;
        if (argumentCount != expectedArgumentCount) {
            throw new ShellCommandException(String.format(
                    "Unexpected argument count: got %d while %d was expected", argumentCount, expectedArgumentCount
            ));
        }
    }

    public boolean doSet(String option, String value) {
        switch (option.toLowerCase()) {
            case "player":
                setPlayer(value);
                return true;

            case "frequency":
                setPeriod(value);
                return true;

            case "speed":
                setSpeed(value);
                return true;

            case "timer":
                setTimerStreamName(value);
                return true;

            case "timebuffer":
                setTimerQueueSize(value);
                return true;

            case "vtime":
                setStartFromTime(value);
                return true;

            case "destvtime":
                setDestinationVirtualTimestamp(value);
                return true;

            case "playlog":
                setLogMode(value);
                return true;

            case "cyclic":
                setCyclic(value);
                return true;

            default:
                return false;
        }
    }

    public void doSet() {
        System.out.println(pad("player:") + config.player);
        System.out.println(pad("frequency:") + config.virtualTimeSliceDurationMs);
        System.out.println(pad("speed:") + config.speed);
        System.out.println(pad("timer:") + config.timerStreamName);
        System.out.println(pad("timebuffer:") + getPrintable(config.timeQueueSize));
        System.out.println(pad("vtime:") + getPrintableNullableTime(config.startFromVirtualTimestamp));
        System.out.println(pad("destvtime:") + getPrintableNullableTime(config.destinationVirtualTimestamp));
        System.out.println(pad("playlog:") + config.logMode);
        System.out.println(pad("cyclic:") + config.cyclic);
    }

    private String getPrintableNullableTime(Long nullabeTimestamp) {
        return getPrintable(nullabeTimestamp != null ? shell.formatTime(nullabeTimestamp) : null);
    }

    private String pad(String str) {
        return StringUtils.rightPad(str, 15);
    }

    private void setPlayer(String value) {
        switch (value) {
            case VIRTUAL_PLAYER:
            case REALTIME_PLAYER:
                break;
            default:
                throw new ShellCommandException("Unknown player type");
        }
        if (hasActivePlayer()) {
            throw new ShellCommandException("Playback in progress. Can't change player while running.");
        }
        config.player = value;
        shell.confirm("Player type: " + config.player);
    }

    private void setStartFromTime(String args) {
        try {
            config.startFromVirtualTimestamp = parseNullableTime(args);
        } catch (ParseException e) {
            throw new ShellCommandException("Failed to parse time.");
        }
        shell.confirm("Virtual time initial timestamp: " + shell.formatTime(config.startFromVirtualTimestamp));
    }

    private void setDestinationVirtualTimestamp(String args) {
        try {
            config.destinationVirtualTimestamp = parseNullableTime(args);
        } catch (ParseException e) {
            throw new ShellCommandException("Failed to parse time.");
        }
        shell.confirm("Initial destination virtual time: " + shell.formatTime(config.destinationVirtualTimestamp));
    }

    private Long parseNullableTime(String args) throws ParseException {
        if (args.equalsIgnoreCase("null")) {
            return null;
        }
        return shell.parseTime(args);
    }

    private void setSpeed(String value) {
        config.speed = Double.parseDouble(value);
        shell.confirm("Playback speed: " + config.speed);
        if (hasActivePlayer()) {
            activePlayer.setSpeed(config.speed);
        }
    }

    private void setPeriod(String value) {
        config.virtualTimeSliceDurationMs = Integer.parseInt(value);
        shell.confirm("Playback virtual clock update interval: " + config.virtualTimeSliceDurationMs + " ms");
        if (hasActivePlayer()) {
            activePlayer.setTimeSliceDuration(config.virtualTimeSliceDurationMs);
        }
    }

    private void setTimerStreamName(String streamName) {
        config.timerStreamName = streamName;
        shell.confirm("Time message stream name: " + config.timerStreamName);
    }

    private void setTimerQueueSize(String value) {
        config.timeQueueSize = Integer.parseInt(value);
        if (config.timeQueueSize == -1) {
            config.timeQueueSize = null;
        }
        shell.confirm("Time message stream size: " + getPrintable(config.timeQueueSize));
    }

    private String getPrintable(Object value) {
        return value != null ? value.toString() : "not set";
    }

    private void setLogMode(String value) {
        config.logMode = LogMode.valueOf (value);
        shell.confirm ("Log all playback: " + config.logMode);
        if (hasActivePlayer()) {
            activePlayer.setLogMode(config.logMode);
        }
    }

    private void setCyclic(String value) {
        config.cyclic = Boolean.parseBoolean(value);
        shell.confirm("Cycle source streams: " + config.cyclic);
    }


    private void play() {
        if (hasActivePlayer()) {
            throw new ShellCommandException("Playback is already in progress.");
        } else {
            activePlayer = createPlayerFromConfig();
            activePlayer.play();
        }
    }

    private PlayerInterface createPlayerFromConfig() {
        config.time = shell.selector.getTime();
        config.stopAtTimestamp = getStopTimeFromShell();

        DXTickDB db = shell.dbmgr.getDB();
        if (db == null) {
            throw new ShellCommandException("DB is not set");
        }

        switch (config.player) {
            case VIRTUAL_PLAYER:
                return VirtualPlayer.create(config, shell);
            case REALTIME_PLAYER:
                return RealtimePlayer.create(config, shell);
            default:
                throw new ShellCommandException("Unknown player: " + config.player, 2);
        }
    }

    private Long getStopTimeFromShell() {
        long endtime = shell.selector.getEndtime();
        return endtime == Long.MAX_VALUE ? null : endtime;
    }

    private void stop() {
        assertHasActivePlayer();
        activePlayer.stop();
        activePlayer = null;
    }

    private void pause() {
        assertHasActivePlayer();
        activePlayer.pause();
    }

    private void resume() {
        assertHasActivePlayer();
        activePlayer.setStopAtTimestamp(getStopTimeFromShell()); // Update end time from current settings
        activePlayer.resume();
    }

    private void next() {
        assertHasActivePlayer();
        activePlayer.next();
    }

    private boolean hasActivePlayer() {
        return activePlayer != null;
    }

    private void assertHasActivePlayer() {
        if (!hasActivePlayer()) {
            throw new ShellCommandException("No playback running");
        }
    }
}