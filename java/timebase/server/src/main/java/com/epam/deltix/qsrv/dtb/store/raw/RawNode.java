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
package com.epam.deltix.qsrv.dtb.store.raw;

import com.epam.deltix.qsrv.dtb.fs.pub.*;
import java.io.*;

/**
 *
 */
public abstract class RawNode {
    protected AbstractPath                      path;
    protected int                               formatVersion;
    protected long                              version;
    protected int                               numEntities;    

    public AbstractPath         getPath () {
        return path;
    }

    public int                  getFormatVersion () {
        return formatVersion;
    }

    public long                 getVersion () {
        return version;
    }

    public int                  getNumEntities () {
        return numEntities;
    }

    public abstract void        setPath (AbstractPath path);
    
    public final void           readIndex (DiagListener dlnr)
        throws IOException
    {
        readIndex (Verifier.TS_UNKNOWN, Verifier.TS_UNKNOWN, dlnr);
    }

    public abstract void        readIndex (
        long                        startTimestamp,
        long                        limitTimestamp,
        DiagListener                dlnr
    )
        throws IOException;

    public abstract int         getEntity (int idx);
    
    
}