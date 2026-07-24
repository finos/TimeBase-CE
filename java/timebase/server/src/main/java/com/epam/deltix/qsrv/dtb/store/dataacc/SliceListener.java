/*
 * Copyright 2024 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.dtb.store.dataacc;

public interface SliceListener {

    /**
     * Invoked when new slice is checked out for inserting data
     * @param slice slice
     */
    void        checkoutForInsert(TimeSlice slice);

    /**
     * Invoked when new slice is checked out for reading data
     * @param slice slice
     */
    void        checkoutForRead(TimeSlice slice);

    /**
     * Invoked by TSRoot when slice is removed.
     * @param slice slice that was changed, null if whole TSRoot is removed
     */
    void        onClosed(TimeSlice slice);
}