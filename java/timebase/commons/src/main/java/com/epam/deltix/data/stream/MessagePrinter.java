/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.data.stream;

import com.epam.deltix.streaming.MessageChannel;

import java.io.*;

/**
 *  Prints messages, one per line,
 *  optionally prefixed by the specified string.
 */
public final class MessagePrinter <T> extends MessageFork <T> {
    private final StringBuilder     mStringBuilder = new StringBuilder ();
    private final PrintStream       mOut;
    public volatile String          prefix = "";
    
    public MessagePrinter () {
        this (null);
    }        
    
    public MessagePrinter (MessageChannel<T> next) {
        this (System.out, next);
    }
    
    public MessagePrinter (PrintStream out, MessageChannel <T> next) {
        super (next, null);
        mOut = out;
    }
    
    public boolean                  accept (T message) {
        mStringBuilder.setLength (0);
        mStringBuilder.append (prefix);
        
        if (message instanceof PrintableMessage)
            ((PrintableMessage) message).print (mStringBuilder);
        else
            mStringBuilder.append (message);
        
        mOut.println (mStringBuilder);
        return (true);
    }

    public void                     close () {
        mOut.flush ();
    }    
}