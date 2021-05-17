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
package com.epam.deltix.qsrv.hf.tickdb.pub;

public class FSOptions {

    public int     maxFileSize = 1 << 23;

    public int     maxFolderSize = 100;

    public String  compression = "LZ4(5)";

    public String  url = null;

    public FSOptions() {
    }

    public FSOptions(int maxFileSize, int maxFolderSize) {
        this.maxFileSize = maxFileSize;
        this.maxFolderSize = maxFolderSize;
    }

    public FSOptions withMaxFolderSize(int size) {
        this.maxFolderSize = size;
        return this;
    }

    public FSOptions withMaxFileSize(int size) {
        this.maxFileSize = size;
        return this;
    }

    public FSOptions withCompression(String compression) {
        this.compression = compression;
        return this;
    }
}
