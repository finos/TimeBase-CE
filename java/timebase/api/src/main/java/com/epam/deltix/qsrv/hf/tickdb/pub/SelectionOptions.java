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
package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.qsrv.hf.pub.ChannelQualityOfService;

/**
 *  Options for selecting data from a stream.
 */
public class SelectionOptions extends CommonOptions {

    /**
     * A constant for {@link #shiftOffset shiftOffset} field. Abort the cursor, when a truncation occurs.
     */
    public static final long ABORT_CURSOR = Long.MIN_VALUE;

    /**
     *  Instead of returning false from next () at the end of the stream,
     *  wait for live data to be added.
     */
    public boolean      live = false;

    /**
     *  Specify cursor direction  
     */
    public boolean      reversed = false;

    /**
     *  Output out-of-order late messages. Time base consumers receive historical messages they requested strictly ordered by their time.
     *  For scenarios when new messages arrive in the middle of consumer's session (So called "live" mode) it is possible that newly arrived message has a timestamp in already consumer time region.
     *  In this cases this flag allows consumer to receive these "late" messages even if they out of order with respect to current baseline time.
     *
     *  NOTE: Late Messages that are timestamped prior to consumer's <code>select time</code> or last <code>reset time</code> will not be delivered even with this flag enabled.
     *
     */
    public boolean      allowLateOutOfOrder = false;

    /**
     * Allow rebroadcast unique message on open/reset cursors
     */
    public boolean      rebroadcast = true;

    /**
     *  Enabled/Disables sending system messages when cursor switches from historical to realtime mode.
     */
    public boolean      realTimeNotification = false;

    /**
     *  When true, allows receiving notification messages
     *  {@link deltix.timebase.messages.service.StreamTruncatedMessage}, {@link deltix.timebase.messages.service.MetaDataChangeMessage}
     *  and {@link deltix.timebase.messages.schema.SchemaChangeMessage}
     *  when stream is truncated / stream metadata is changed.
     */
    public boolean      versionTracking = false;

    /**
     *  Enabled/Disables fixed message order from cursor independent of subscribed entities.
     */
    public boolean      ordered = false;

    /**
     * <p>Will restrict permitted stream type of cursor to type of first stream in the cursor.
     * Attempt to add new stream of different type to produced cursor <i>may</i> result in error.</p>
     *
     * <p>Do not set this flag if you going to add new streams of arbitrary type to the cursor.</p>
     *
     * <p>Cursors created with this flag <i>may</i> give better performance is some specific cases.</p>
     */
    public boolean      restrictStreamType = false;

    /**
     * Specify (in milliseconds) an offset to shift cursor relatively to the current timestamp,
     * when stream truncation occurs.
     * <p>
     * I.e. cursor be reset to current_timestamp + shiftOffset position; shiftOffset can be negative.
     * If the field is equals to {@link #ABORT_CURSOR ABORT_CURSOR}, then cursor throws an exception, 
     * when stream truncation occurs.
     * </p>
     */
    public long         shiftOffset = 1;

    /**
     * List of spaces to select data from.
     * If set to {@code null} then data from all spaces is loaded.
     *
     * Any non-null values is permitted only for streams that supports "spaces".
     * If set then data only from the specified spaces will be loaded.
     *
     * See also {@link LoadingOptions#space}
     */
    public String[] spaces = null;

    /**
     * Include/Exclude schema change messages.
     *
     * RecordClassDescriptor available at {@link StreamOptions#getSchemaChangeMessageDescriptor()} ()}
     */
    //TODO replace or handle with version tracking + subscriptions
    public boolean includeSchemaChangeMessages = false;

    /**
     * Creates new SelectionOptions instance with given arguments.
     * @param raw raw selection mode, (@see deltix.qsrv.hf.pub.RawMessage).
     * @param live consuming live data
     * @param reversed reverse mode
     * @param shiftOffset offset to shift cursor in case of truncations
     */

    public SelectionOptions (boolean raw, boolean live, boolean reversed, long shiftOffset) {
        this.raw = raw;
        this.live = live;
        this.reversed = reversed;
        this.shiftOffset = shiftOffset;
    }

    /**
     * Creates new SelectionOptions instance with given arguments.
     * @param raw raw selection mode, (@see deltix.qsrv.hf.pub.RawMessage).
     * @param live consuming live data
     * @param reversed reverse mode, if true then cursor read data using descending order of message time
     */
    public SelectionOptions (boolean raw, boolean live, boolean reversed) {
        this.raw = raw;
        this.live = live;
        this.reversed = reversed;
    }

    /**
     * Creates new SelectionOptions instance with given arguments.
     * @param raw raw selection mode, (@see deltix.qsrv.hf.pub.RawMessage).
     * @param live consuming live data
     */
    public SelectionOptions (boolean raw, boolean live) {
        this.raw = raw;
        this.live = live;
    }

    /**
     * Creates new SelectionOptions instance with given arguments.
     * @param raw raw selection mode, (@see deltix.qsrv.hf.pub.RawMessage).
     * @param live consuming live data
     * @param qos ChannelQualityOfService setting
     */
    public SelectionOptions (boolean raw, boolean live, ChannelQualityOfService qos) {
        this.raw = raw;
        this.live = live;
        this.channelQOS = qos;
    }

    public SelectionOptions () {
    }

    public boolean isAllowLateOutOfOrder () {
        return allowLateOutOfOrder;
    }

    public void setAllowLateOutOfOrder (boolean allowLateOutOfOrder) {
        this.allowLateOutOfOrder = allowLateOutOfOrder;
    }

    public boolean isRealTimeNotification() {
        return realTimeNotification;
    }

    public void     setRealTimeNotification(boolean realTimeNotification) {
        this.realTimeNotification = realTimeNotification;
    }

    public boolean isLive () {
        return live;
    }

    public void setLive (boolean live) {
        this.live = live;
    }

    public boolean isRaw () {
        return raw;
    }

    public void setRaw (boolean raw) {
        this.raw = raw;
    }

    public boolean isReversed () {
        return reversed;
    }

    public void setReversed (boolean reversed) {
        this.reversed = reversed;
    }

    public boolean isSchemaChangeMessagesIncluded() {
        return includeSchemaChangeMessages;
    }

    public void setIncludeSchemaChangeMessages(boolean includeSchemaChangeMessages) {
        this.includeSchemaChangeMessages = includeSchemaChangeMessages;
    }

    public void withSpaces(String ... spaces) {
        this.spaces = spaces;
    }
}