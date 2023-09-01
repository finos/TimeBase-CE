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
package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.groups;

import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.GroupByFilterState;
import org.ehcache.Cache;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;

import java.io.File;

public class ProcessedStatesStorage {

    private PersistentCacheManager persistentCacheManager;
    private File cacheDirectory;
    private Cache<GroupByFilterState, Boolean> cache;

    void add(GroupByFilterState state) {
        if (cache == null) {
            cacheDirectory = cacheDirectory();
            persistentCacheManager = createDiskCache(cacheDirectory, state);
            cache = persistentCacheManager.getCache("GroupsCache", GroupByFilterState.class, Boolean.class);
        }

        cache.put(state, true);
    }

    boolean has(GroupByFilterState state) {
        if (cache == null) {
            return false;
        }

        return cache.containsKey(state);
    }

    void close() {
        if (persistentCacheManager != null) {
            persistentCacheManager.close();
            persistentCacheManager = null;
            if (cacheDirectory != null) {
                cacheDirectory.delete();
            }
        }
        cache = null;
    }

    private File cacheDirectory() {
        return QSHome.getFile("temp/qql/groups" + System.currentTimeMillis());
    }

    private static synchronized PersistentCacheManager createDiskCache(File cacheDirectory, GroupByFilterState state) {
        return CacheManagerBuilder.newCacheManagerBuilder()
            .with(CacheManagerBuilder.persistence(cacheDirectory.getAbsolutePath()))
            .withCache("GroupsCache",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(GroupByFilterState.class, Boolean.class,
                    ResourcePoolsBuilder.newResourcePoolsBuilder()
                        .heap((int) ((float) GroupsCountManager.QQL_MAX_GROUPS_COUNT * 0.02f), EntryUnit.ENTRIES)
                        .disk(128, MemoryUnit.GB)
                ).withKeySerializer(state.getClass())
            ).build(true);
    }
}