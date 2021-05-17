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
package com.epam.deltix.qsrv.dtb.fs.local;

import com.epam.deltix.qsrv.dtb.fs.pub.*;
import com.epam.deltix.util.lang.Util;

import java.io.File;

/**
 *
 */
public class LocalFS implements AbstractFileSystem {

    public LocalFS () {
    }
    
    @Override
    public boolean              isAbsolutePath (String path) {
        return (new File (path).isAbsolute ());
    }
        
    @Override
    public AbstractPath         createPath (String path) {
        return (new PathImpl (path, this));
    }

    @Override
    public AbstractPath         createPath(AbstractPath parent, String child) {
        return new PathImpl((PathImpl) Util.unwrap(parent), child, this);
    }

    @Override
    public long getReopenOnSeekThreshold() {
        return Long.MAX_VALUE;
    }

    @Override
    public String getSeparator() {
        return File.separator;
    }
}
