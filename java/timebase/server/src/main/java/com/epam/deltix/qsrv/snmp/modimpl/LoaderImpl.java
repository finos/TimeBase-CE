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
package com.epam.deltix.qsrv.snmp.modimpl;

import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBLoader;
import com.epam.deltix.qsrv.snmp.model.timebase.Loader;

/**
 *
 */
public class LoaderImpl implements Loader {
    private int id;
    private TBLoader loader;

    public LoaderImpl(TBLoader loader) {
        this.id = (int) loader.getId();
        this.loader = loader;
    }

    @Override
    public int      getLoaderId() {
        return id;
    }

    @Override
    public String getSource() {
        return loader.getTargetStreamKey();
    }

    @Override
    public String   getLoaderLastMessageTime() {
        return SnmpUtil.formatDateTimeMillis(loader.getLastMessageSysTime());
    }
}
