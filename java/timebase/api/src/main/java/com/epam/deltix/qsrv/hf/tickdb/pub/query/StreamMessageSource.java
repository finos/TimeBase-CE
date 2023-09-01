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
package com.epam.deltix.qsrv.hf.tickdb.pub.query;

import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;

/**
 *  Supplies an InstrumentMessage as well as information about
 *  its source stream and RecordClassDescriptor.
 */
public interface StreamMessageSource<T> extends TypedMessageSource {
    /**
     *  Returns the current message.
     */
    public T getMessage ();

    /**
     *  Return the index of the stream that is the source of the current
     *  message.
     *
     *  @return The current message source stream's index.
     */
    public int                              getCurrentStreamIndex ();

    /**
     *  Return the key of the stream that is the source of the current
     *  message.
     *
     *  @return The source stream key.
     */
    public String                           getCurrentStreamKey ();

    /**
     *  Return the current stream instance, unless it has been removed,
     *  in which case null is returned.
     */
    public TickStream                       getCurrentStream ();
    
    /**
     *  Return a small number identifying the returned entity. This number
     *  is unique throughout the life of the message source. Removing
     *  entities from subscription does not create reusable holes in the
     *  "space" of entity indexes.
     * 
     *  @see EntitySubscriptionController
     */
    public int                              getCurrentEntityIndex ();   
}