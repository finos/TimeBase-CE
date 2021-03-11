package com.epam.deltix.qsrv.hf.tickdb.pub;

import javax.xml.bind.annotation.XmlElement;

/**
 * Options controlling data buffering in streams.
 */
public class BufferOptions {
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
