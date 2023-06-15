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
package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import com.epam.deltix.qsrv.hf.tickdb.comm.StreamState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
class StreamsCache {

    enum State {
        INITIAL, PARTIALLY_LOADED, NOT_CHANGED, CHANGED
    }

    private final Map<String, TickStreamClient> streams = new HashMap<String, TickStreamClient>();
    private final Map<String, StreamState>      states = new HashMap <String, StreamState> ();
    private volatile State                      dbState = State.INITIAL;

    public synchronized boolean         addStream(TickStreamClient stream) {

        if (!streams.containsKey(stream.getKey())) {
            streams.put(stream.getKey(), stream);
            setState(stream.getKey(), new StreamState(true));

            if (dbState == State.INITIAL)
                dbState = State.PARTIALLY_LOADED;

            return true;
        }

        return false;
    }

    public synchronized StreamState     getState(String key) {
        return states.get(key);
    }

    private synchronized void           setState(String key, StreamState state) {
        StreamState s = states.put(key, state);
        assert s == null : key;
    }

    synchronized void            onPropertyChanged(String key, int property) {
        StreamState state = getState(key);
        if (state != null)
            state.reset(property);
    }

    synchronized void            onStreamRenamed(String key, String newKey) {

        TickStreamClient stream = streams.remove(key);
        if (stream != null)
            setChanged();

        StreamState state = states.remove(key);
        if (state != null)
            states.put(newKey, null);
    }

    public synchronized TickStreamClient[] getStreams() {
        return (streams.values().toArray(new TickStreamClient [streams.size ()]));
    }

    synchronized void           onStreamCreated(String key) {
        //System.out.println(this + ": Stream created: " + key);

        states.put(key, null);
        setChanged();
    }

    synchronized void           onStreamDeleted(String key) {
        //System.out.println(this + ": Stream deleted: " + key);

        streams.remove(key);

        StreamState state = states.remove(key);
        if (state != null)
            state.notifyLocks();

        setChanged();
    }

    private void                setChanged() {
        if (dbState == State.NOT_CHANGED)
            dbState = State.CHANGED;
    }

    public synchronized TickStreamClient            getStream(String key) {
        return streams.get(key);
    }

    synchronized String[]                    getUnmappedStreams() {
        ArrayList<String> list = new ArrayList<String>();

        for (Map.Entry<String, StreamState> entry : states.entrySet()) {
            if (entry.getValue() == null)
                list.add(entry.getKey());
        }

        return list.toArray(new String[list.size()]);
    }

//    public synchronized boolean         isNewStream(String key) {
//        return !streams.containsKey(key) && states.containsKey(key);
//    }

    synchronized State           getDbState() {
        return dbState;
    }

    synchronized void            setUnchanged() {

        // check for changed streams
        for (Map.Entry<String, StreamState> entry : states.entrySet()) {
            if (entry.getValue() == null) {
                dbState = State.CHANGED;
                return;
            }
        }

        dbState = State.NOT_CHANGED;
    }

    synchronized void                   clear() {

        for (String key : streams.keySet()) {
            StreamState state = states.remove(key);
            if (state != null)
                state.notifyLocks();
        }

        streams.clear();
        states.clear();

        dbState = State.INITIAL;
    }
}