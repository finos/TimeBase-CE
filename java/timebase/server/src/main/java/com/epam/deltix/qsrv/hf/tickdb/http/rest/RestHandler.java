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
package com.epam.deltix.qsrv.hf.tickdb.http.rest;

import com.epam.deltix.qsrv.hf.tickdb.http.HTTPProtocol;
import com.epam.deltix.util.concurrent.QuickExecutor;

import java.io.DataOutput;
import java.io.IOException;

/**
 *
 */
public abstract class RestHandler extends QuickExecutor.QuickTask {

    public RestHandler(QuickExecutor quickExecutor) {
        super(quickExecutor);
    }

    @Override
    public abstract void        run() throws InterruptedException;

    public abstract void        sendKeepAlive() throws IOException;

    public void                 sendResponse(DataOutput dout, Throwable t) throws IOException {
        dout.write(HTTPProtocol.RESPONSE_BLOCK_ID);
        if (t == null) {
            dout.writeInt(HTTPProtocol.RESP_OK);
        } else {
            dout.writeInt(HTTPProtocol.RESP_ERROR);
            dout.writeUTF(t.getClass().getName());

            String msg = t.getMessage();
            dout.writeUTF(msg != null ? msg : "");
        }
    }
}