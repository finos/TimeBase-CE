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

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.util.security.AuthorizationController;

public interface DXChannel<T> extends AuthorizationController.ProtectedResource {
    /**
     *  Returns the key, which uniquely identifies the channel
     */
    String                                  getKey();

    /**
     *  Returns a user-readable short name.
     */
    String                                  getName();

    /**
     *  Returns a user-readable multi-line description.
     */
    String                                  getDescription();

    /**
     *  Returns the class descriptors associated with this channel
     */
    RecordClassDescriptor[]                 getTypes();

    /**
     *  <p>Opens a source for reading data from this channel, according to the
     *  specified preferences. Iterator-like approach to consume messages:
     *  <code>
     *      while (source.next())
     *          source.getMessage()
     *  </code>
     *  </p>
     *
     *  @return A message source to read messages from.
     */
    MessageSource<T> createConsumer(ChannelPreferences options);

    /**
     *  Creates a channel for loading data. The publisher must be closed
     *  when the loading process is finished.
     *
     *  @return A consumer of messages to be loaded into the channel.
     */
    MessageChannel<T> createPublisher(ChannelPreferences options);
}