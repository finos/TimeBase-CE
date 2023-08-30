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
package com.epam.deltix.util.vsocket;

import com.epam.deltix.util.lang.Disposable;

import java.io.*;

/**
 *  A virtual socket.
 */
public interface VSChannel extends Disposable {

    public int                  getLocalId ();

    public int                  getRemoteId ();

    public String               getRemoteAddress();

    public String               getRemoteApplication();

    public String               getClientId();

    public VSOutputStream       getOutputStream ();

    public DataOutputStream     getDataOutputStream ();

    public InputStream          getInputStream ();

    public DataInputStream      getDataInputStream ();

    public VSChannelState       getState();

    public boolean              setAutoflush(boolean value);

    public boolean              isAutoflush();
    
    public void                 close(boolean terminate);

    public void                 setAvailabilityListener (Runnable lnr);
    
    public Runnable             getAvailabilityListener ();

    public boolean              getNoDelay();

    public void                 setNoDelay(boolean value);
    
    public String               encode(String value);

    public String               decode(String value);
}