/*
 * Copyright 2026 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.dtb.store.impl;


import com.epam.deltix.qsrv.dtb.fs.local.LocalFS;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.qsrv.dtb.store.codecs.TSNames;
import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.collections.generated.IntegerArrayList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

@Category(JUnitCategories.TickDBFast.class)
public class Test_SymbolRegistryImpl {
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    private final LocalFS localFS = new LocalFS();

    private AbstractPath path;

    @Before
    public void setup() throws IOException {
        this.path = localFS.createPath(tmpFolder.newFolder().getAbsolutePath());
    }

    /**
     * Ensures that successful reading from a tmp.symbols.dat file works
     * and does not lead to data corruption and duplicate symbols.
     */
    @Test
    public void loadFromTemp() throws IOException {
        SymbolRegistryImpl registry1 = new SymbolRegistryImpl();
        registry1.load(path); // Init from empty folder
        registry1.registerSymbol("AAA", "DAAA");
        registry1.registerSymbol("BBB", "DBBB");

        registry1.store(path);

        AbstractPath normalName = path.append(TSNames.SYM_REGISTRY_NAME);
        // Rename "regular" file to "tmp." name
        normalName.renameTo(TSNames.TMP_PREFIX + TSNames.SYM_REGISTRY_NAME);

        // Try to load the registry from the file with tmp name
        SymbolRegistryImpl registry2 = new SymbolRegistryImpl();
        registry2.load(path);

        IntegerArrayList ids = new IntegerArrayList();
        registry2.listIds(ids);

        Assert.assertEquals("Symbol count mismatch", 2, ids.size());
    }

    /**
     * Ensures that if there is more data in the temp file than in the regular file,
     * then registry will prefer the temp file, even if temp file is incomplete.
     */
    @Test
    public void preferTempIfMoreData() throws IOException {
        AbstractPath path2 = localFS.createPath(tmpFolder.newFolder().getAbsolutePath());

        SymbolRegistryImpl registry1 = new SymbolRegistryImpl();
        registry1.load(path); // Init from empty folder

        SymbolRegistryImpl registry2 = new SymbolRegistryImpl();
        registry2.load(path2); // Init from empty folder

        for (int i = 0; i < 3; i++) {
            registry1.registerSymbol("AAA" + i, "AAA" + i);
        }
        registry1.store(path);
        for (int i = 0; i < 1000; i++) {
            registry2.registerSymbol("BBB" + i, "BBB" + i);
        }
        registry2.store(path2);

        AbstractPath path2name = path2.append(TSNames.SYM_REGISTRY_NAME);
        // Rename "regular" file to "tmp." name and move it to the first folder
        AbstractPath newName = path.append(TSNames.TMP_PREFIX + TSNames.SYM_REGISTRY_NAME);

        File srcFile = new File(path2name.getPathString());
        File dstFile = new File(newName.getPathString());
        long fileLength = srcFile.length();
        // Copy only half of the file to simulate incomplete temp file
        doPartialCopy(srcFile, dstFile, fileLength / 2);

        SymbolRegistryImpl registry3 = new SymbolRegistryImpl();
        registry3.load(path);

        IntegerArrayList ids = new IntegerArrayList();
        registry3.listIds(ids);

        Assert.assertTrue(ids.size() > 400); // Should contain half of the symbols from the second registry
    }

    private void doPartialCopy(File srcFile, File dstFile, long bytesToCopy) {
        try (var in = new java.io.FileInputStream(srcFile);
             var out = new java.io.FileOutputStream(dstFile)) {
            byte[] buffer = new byte[8192];
            long remaining = bytesToCopy;
            int read;
            while (remaining > 0 && (read = in.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                out.write(buffer, 0, read);
                remaining -= read;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy file", e);
        }
    }

    /**
     * Closes the registry and then loads it again from the same file.
     */
    @Test
    public void loadTwice() throws IOException {


        SymbolRegistryImpl registry1 = new SymbolRegistryImpl();
        registry1.load(path); // Init from empty folder
        registry1.registerSymbol("AAA", "DAAA");
        registry1.registerSymbol("BBB", "DBBB");


        registry1.store(path);

        // Try to load the registry from the file with tmp name
        SymbolRegistryImpl registry2 = new SymbolRegistryImpl();
        registry2.load(path);
        registry2.close();
        registry2.load(path);

        IntegerArrayList ids = new IntegerArrayList();
        registry2.listIds(ids);

        Assert.assertEquals("Symbol count mismatch", 2, ids.size());
    }
}
