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
package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.SubscriptionChangeListener;
import com.epam.deltix.timebase.messages.MessageInfo;

import java.io.Flushable;

/**
 *
 */
public interface TickLoader<T extends MessageInfo> extends MessageChannel<T>, Flushable {

    public WritableTickStream   getTargetStream ();

    public void         addEventListener (LoadingErrorListener listener);

    public void         removeEventListener (LoadingErrorListener listener);

    public void         addSubscriptionListener (SubscriptionChangeListener listener);

    public void         removeSubscriptionListener (SubscriptionChangeListener listener);

    public void         removeUnique(T msg);
}