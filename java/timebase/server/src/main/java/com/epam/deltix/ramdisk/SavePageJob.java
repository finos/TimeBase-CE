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
package com.epam.deltix.ramdisk;

/**
 *
 */
final class SavePageJob {

    SavePageJob(WriterThread writer) {
        this.writer = writer;
    }

    Page                page;
    FD                  fd;
    long                address;
    final byte []       data = new byte [DataCache.PAGE_SIZE];
    int                 length;

    /**
     *  Whether at the time of assigning the job all pages prior to the current
     *  one were clean. This flag triggers the clean commit event handler.
     */
    boolean             followsCleanRange;

    /**
     *  Whether at the time of assigning this job the current page was the last one
     *  in the dirty range. Note that isLastDirtyPage does not imply
     *  followsCleanRange, as pages prior to the current one may have become
     *  dirty. This flag triggers the auto-commit logic (if enabled).
     */
    boolean             isLastDirtyPage;

    WriterThread        writer;
}
