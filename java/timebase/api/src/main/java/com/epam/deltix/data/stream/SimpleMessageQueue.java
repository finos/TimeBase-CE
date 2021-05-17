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
package com.epam.deltix.data.stream;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.util.concurrent.*;
import java.util.*;

/**
 *  An unbounded queue of arbitrary messages, exposing a MessageChannel and a 
 *  MessageSource as a way of accessing the queue. The MessageSource (output) end of
 *  the queue can be opened and closed. Closing the output causes messages to 
 *  stop being accumulated.
 */
public class SimpleMessageQueue <T> implements MessageChannel<T> {
    private volatile Runnable           avLnr = null;
    private ArrayDeque <T>              q = null;
    
    private class MessageSourceImpl
        implements MessageSource<T>, IntermittentlyAvailableResource
    {
        private T       message;
        
        @Override
        public void     setAvailabilityListener (Runnable maybeAvailable) {
            avLnr = maybeAvailable;
        }
        
        @Override
        public T        getMessage () {
            return (message);
        }
        
        @Override
        public boolean  next () {
            for (;;) {
                synchronized (SimpleMessageQueue.this) {
                    message = q.pollFirst ();
                    
                    if (message != null)
                        return (true);                    
                    
                    if (avLnr != null)
                        throw UnavailableResourceException.INSTANCE;
                    
                    try {
                        SimpleMessageQueue.this.wait ();
                    } catch (InterruptedException x) {
                        throw new UncheckedInterruptedException (x);
                    }
                }
            }
        }

        @Override
        public boolean  isAtEnd () {
            return (false);
        }

        @Override
        public void     close () {
            synchronized (SimpleMessageQueue.this) {
                q = null;
            }
        }
    }
    
    public void                             send (T msg) {
        Runnable        avlnrSnapshot = this.avLnr;

        synchronized (this) {
            q.offerLast (msg);

            if (avlnrSnapshot == null)
                SimpleMessageQueue.this.notify ();                                            
        }

        if (avlnrSnapshot != null)
            avlnrSnapshot.run ();
    }
    
    @Override
    public void                             close () {
        throw new UnsupportedOperationException ();
    }

    public synchronized MessageSource <T>   open () {
        if (q != null)
            throw new IllegalStateException ("Already open");
        
        q = new ArrayDeque <T> ();
        
        return (new MessageSourceImpl ());
    }
}
