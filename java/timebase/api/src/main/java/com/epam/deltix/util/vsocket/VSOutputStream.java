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

import java.io.IOException;
import java.io.OutputStream;

/**
 * Date: Mar 29, 2010
 */
public abstract class VSOutputStream extends OutputStream {

    public abstract void    enableFlushing() throws IOException;

    /*
     *  Disables flushing internal buffer when it reach capacity.
     *  Buffer will be extended when new data is written until flashing will be enabled again
     */

    public abstract void    disableFlushing();

    /*
     *  Flushes portion of data that can recieved on the remote side immediately.
     */
    public abstract void    flushAvailable() throws IOException;

    //public abstract int     available();
}