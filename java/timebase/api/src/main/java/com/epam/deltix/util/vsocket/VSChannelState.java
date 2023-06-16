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
package com.epam.deltix.util.vsocket;

/**
 * Date: Mar 30, 2010
 */
public enum VSChannelState {
    /**
     *  Just created no handshake yet. Writing to output stream is possible,
     *  but might block waiting for handshake (with remote capacity report).
     *  Reading from input stream will obviously block waiting for data to be
     *  sent over.
     */
    NotConnected,

    /**
     *  Normal connected state.
     */
    Connected, 

    /**
     *  Remote endpoint has been closed. Writing to output stream will result in
     *  a {@link ChannelClosedException} being thrown. Reading from input stream
     *  will return all data that was sent prior to remote endpoint being closed,
     *  then EOF.
     */
    RemoteClosed,

    /**
     *  Local endpoint has been closed. Either reading or writing will
     *  immediately throw a {@link ChannelClosedException}.
     */
    Closed,

    /**
     *  Local close has been confirmed by remote side; therefore its id can
     *  now be re-used. To the caller, this state's behavior is identical to
     *  {@link #Closed}.
     */
    Removed
}