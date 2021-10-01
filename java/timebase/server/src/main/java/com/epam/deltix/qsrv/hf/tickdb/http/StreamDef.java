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
package com.epam.deltix.qsrv.hf.tickdb.http;

import com.epam.deltix.qsrv.hf.tickdb.pub.BufferOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamScope;
import com.epam.deltix.util.time.Periodicity;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/*
    Copy from StreamOptions to resolve namespace conflicts and have separate fixed implementation
 */

@XmlRootElement()
public class StreamDef {

    /**
     *  Optional user-readable name.
     */
    @XmlElement
    public String                       name = null;

    /**
     *  Optional multi-line description.
     */
    @XmlElement
    public String                       description = null;

    /**
     *  Location of the stream (by default null). When defined this attribute provides alternative stream location (rather than default location under QuantServerHome)
     */
    @XmlElement
    public String                       location = null;

    /**
     *
     *  The number of M-files into which to distribute the
     *  data. Supply {@link deltix.qsrv.hf.tickdb.pub.StreamOptions#MAX_DISTRIBUTION} to keep a separate file
     *  for each instrument (default).
     */
    @XmlElement
    public int                          distributionFactor = StreamOptions.MAX_DISTRIBUTION;

    /**
     * Options that control data buffering.
     */
    @XmlElement
    public BufferOptions                bufferOptions;

    /**
     * Unique streams maintain in-memory cache of resent messages.
     * This concept assumes that stream messages will have some field(s) marked as primary key {@link deltix.timebase.api.PrimaryKey}.
     * Primary key may be a simple field (e.g. symbol) or composite (e.g. symbol and portfolio ID).
     * For each key TimeBase runtime maintains a copy of the last message received for this key (cache).
     * Each new consumer will receive a snapshot of current cache at the beginning of live data subscription.
     */
    @XmlElement
    public boolean                      unique;

    /**
     * Indicates that loader will ignore binary similar messages(for 'unique' streams only).
     */
    @XmlElement
    public boolean                      duplicatesAllowed = true;

    /**
     *  Determines persistent properties of a stream.
     */
    @XmlElement
    public StreamScope                  scope = StreamScope.DURABLE;

    /**
     *  Stream periodicity, if known.
     */
    @XmlElement
    public Periodicity                  periodicity = Periodicity.mkIrregular();

    /**
     *  High availability durable streams are cached on startup.
     */
    public boolean                      highAvailability = false;

    /**
     *  Optional owner of stream.
     *  During stream creation it will be set
     *  equals to authenticated user name.
     */
    @XmlElement
    public String                       owner = null;

    @XmlElement
    public boolean                      polymorphic = true;

    public StreamDef() {
    }

    public StreamDef(StreamOptions options) {
        this.name = options.name;
        this.description = options.description;
        this.distributionFactor = options.distributionFactor;
        this.periodicity = options.periodicity;
        this.unique = options.unique;
        this.owner = options.owner;
        this.scope = options.scope;
        this.duplicatesAllowed = options.duplicatesAllowed;
        this.polymorphic = options.isPolymorphic();

        if (options.bufferOptions != null) {
            this.bufferOptions = new BufferOptions();
            this.bufferOptions.lossless = options.bufferOptions.lossless;
            this.bufferOptions.initialBufferSize = options.bufferOptions.initialBufferSize;
            this.bufferOptions.maxBufferSize = options.bufferOptions.maxBufferSize;
            this.bufferOptions.maxBufferTimeDepth = options.bufferOptions.maxBufferTimeDepth;
        }
    }

    public StreamOptions convert() {
        StreamOptions options = new StreamOptions();

        options.name = this.name;
        options.description = this.description;
        options.distributionFactor = this.distributionFactor;
        options.periodicity = this.periodicity;
        options.unique = this.unique;
        options.owner = this.owner;
        options.scope = this.scope;
        options.duplicatesAllowed = this.duplicatesAllowed;
        options.polymorphic = this.polymorphic;

        if (this.bufferOptions != null) {
            options.bufferOptions = new com.epam.deltix.qsrv.hf.tickdb.pub.BufferOptions();
            options.bufferOptions.lossless = this.bufferOptions.lossless;
            options.bufferOptions.initialBufferSize = this.bufferOptions.initialBufferSize;
            options.bufferOptions.maxBufferSize = this.bufferOptions.maxBufferSize;
            options.bufferOptions.maxBufferTimeDepth = this.bufferOptions.maxBufferTimeDepth;
        }

        return options;
    }

    @XmlElement()
    public String           metadata;


    public static class BufferOptions {
        public BufferOptions() { // JAXB
        }

        /**
         *  Initial size of the write buffer in bytes.
         */
        @XmlElement
        public int                          initialBufferSize = 8192;

        /**
         *  The limit on buffer growth in bytes. Default is 64K.
         */
        @XmlElement
        public int                          maxBufferSize = 64 << 10;

        /**
         * The limit on buffer growth as difference between first
         * and last message time. Default is Long.MAX_VALUE.
         */
        @XmlElement
        public long                         maxBufferTimeDepth = Long.MAX_VALUE;

        /**
         * Applicable to transient streams only. When set to <code>true</code>,
         * the loader will be delayed until all currently open cursors
         * have read enough messages to free up space in
         * the buffer. When set to <code>false</code>,
         * older messages will be discarded after the buffer is filled up regardless
         * of whether there are open cursors that have not yet read such messages.
         * Default is <code>false</code>. Durable streams are always lossless.
         */
        @XmlElement
        public boolean                      lossless = false;
    }
}