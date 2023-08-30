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

/**
 *
 */
public interface VSocket extends Disposable {

    VSocketInputStream          getInputStream();

    VSocketOutputStream         getOutputStream();

    String                      getRemoteAddress();

    //void                        restore(VSocket from);

    void                        close();

    int                         getCode();

    void                        setCode(int code);

    /**
     * Serial number of this socket.
     * Should be used only for easier identification of the socket during debug.
     */
    int getSocketNumber();

    default String getSocketIdStr() {
        return '@' + Integer.toHexString(getCode()) + "#" + getSocketNumber();
    }
}