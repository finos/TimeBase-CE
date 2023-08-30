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
package com.epam.deltix.qsrv.dtb.test;

import com.epam.deltix.qsrv.dtb.fs.local.*;
import com.epam.deltix.qsrv.dtb.fs.pub.*;
import com.epam.deltix.util.io.*;

/**
 *
 */
public class TestConfig {
    public int              batchSize = 1;
    public int              numMessages = 10000;
    public int              numEntities = 10;
    public int              numTypes = 4;
    public int              maxFileSize = 5 << 10;
    public int              maxFolderSize = 10;
    public String           compression = "LZ4";
    public long             baseTime = 1356998400000000000L;
    public AbstractPath     path =
        FSFactory.getLocalFS().createPath(Home.getPath("temp/testdtb"));
}