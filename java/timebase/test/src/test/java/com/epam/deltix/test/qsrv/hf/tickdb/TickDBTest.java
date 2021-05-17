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
package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.tickdb.comm.client.TickDBClient;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TickDBServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.util.lang.Util;

/**
 *  Encapsulates the logic for starting a server.
 */
public abstract class TickDBTest {
    private boolean                 isRemote;

    public abstract void            run (DXTickDB db) throws Exception;

    protected final boolean         isRemote () {
        return (isRemote);
    }

    public final void               runRemote (DXTickDB localDB)
        throws Exception
    {
        TickDBServer            server = new TickDBServer (0, localDB);

        DXTickDB                conn = null;

        try {
            server.start ();

            conn = new TickDBClient ("localhost", server.getPort ());

            conn.open (localDB.isReadOnly ());

            if (!Boolean.getBoolean ("quiet"))
                System.out.println ("Connected to " + conn.getId ());

            isRemote = true;
            run (conn);

            conn.close ();
            conn = null;
        } finally {
            isRemote = false;
            Util.close (conn);
            server.shutdown (true);
        }
    }
}
