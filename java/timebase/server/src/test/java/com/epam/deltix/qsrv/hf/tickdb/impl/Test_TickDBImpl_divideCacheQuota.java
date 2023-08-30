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
package com.epam.deltix.qsrv.hf.tickdb.impl;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Alexei Osipov
 */
public class Test_TickDBImpl_divideCacheQuota {

    @Test
    public void allSmall() {
        int total = 3 << 20;
        TickDBImpl.CacheQuotaSettings quota = TickDBImpl.divideCacheQuota(total, true, true, true, null);
        Assert.assertEquals(total / 3, quota.ramdiskSize);
        Assert.assertEquals(total / 3, quota.localFsCacheSize);
        Assert.assertEquals(total / 3, quota.dsfCacheSize);
    }

    @Test
    public void allBig() {
        long total = 3L << 30;
        TickDBImpl.CacheQuotaSettings quota = TickDBImpl.divideCacheQuota(total, true, true, true, null);
        Assert.assertEquals(128 << 20, quota.ramdiskSize); // Ramdisk is capped to 128Mb
        long expectedSize = (total - (128 << 20)) / 2;
        Assert.assertEquals(expectedSize, quota.localFsCacheSize);
        Assert.assertEquals(expectedSize, quota.dsfCacheSize);
    }

    @Test
    public void allBigDfsPreset() {
        int dfsSizeMb = 768;
        long total = 3L << 30;
        TickDBImpl.CacheQuotaSettings quota = TickDBImpl.divideCacheQuota(total, true, true, true, dfsSizeMb);
        Assert.assertEquals(128 << 20, quota.ramdiskSize); // Ramdisk is capped to 128Mb
        Assert.assertEquals(total - (128 << 20) - (dfsSizeMb << 20), quota.localFsCacheSize);
        Assert.assertEquals(dfsSizeMb << 20, quota.dsfCacheSize);
    }

    @Test
    public void ramdiskAndLocalBig() {
        long total = 3L << 30;
        TickDBImpl.CacheQuotaSettings quota = TickDBImpl.divideCacheQuota(total, true, true, false, null);
        Assert.assertEquals(128 << 20, quota.ramdiskSize); // Ramdisk is capped to 128Mb
        Assert.assertEquals(total - (128 << 20), quota.localFsCacheSize);
        Assert.assertEquals(0, quota.dsfCacheSize);
    }
}