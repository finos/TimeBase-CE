package com.epam.deltix.ramdisk;

import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.io.GUID;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.io.IOUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.IOException;

/**
 *
 */
@Category(RAMDisk.class)
public class Test_PageIndex {

    private final static int    FILES_COUNT = 1_000_000;
    private final static int    PAGES_PER_FILE = 5;

    private final File      workFolder = Home.getFile("temp" + File.separator + new GUID().toString() + File.separator + "pageindextest");
    private RAMDisk         ramdisk;

    @Before
    public void         setup() throws IOException {
        workFolder.mkdirs();
        IOUtil.removeRecursive(workFolder, null, false);

        // pages will be created in test
        ramdisk = RAMDisk.createCacheByNumPages(Integer.MAX_VALUE, 0, 0);
        ramdisk.start();
    }

    @After
    public void         teardown() throws InterruptedException {
        System.out.println("Shutting down RAM disk");
        ramdisk.shutdownAndWait();
        ramdisk = null;
    }

    @Test
    public void         testPageIndexHashCollisions() {
        // create files
        FD[] files = new FD[FILES_COUNT];
        for (int i = 0; i < files.length; ++i)
            files[i] = new FD(ramdisk, new File(workFolder, i + ".txt"));

        // create pages for each file
        Page[][] pages = new Page[FILES_COUNT][PAGES_PER_FILE];
        for (int i = 0; i < pages.length; ++i) {
            for (int j = 0; j < pages[i].length; ++j) {
                Page newPage = new Page(10);
                newPage.fd = files[i];
                pages[i][j] = newPage;
            }
        }

        // map pages
        for (int i = 0; i < files.length; ++i)
            for (int index = 0; index < pages[i].length; ++index)
                files[i].pageIndex.set(index, pages[i][index]);

        // check count of pages
        Assert.assertEquals(files[0].dataCache.pages.size(), FILES_COUNT * PAGES_PER_FILE);

        // check
        for (int i = 0; i < files.length; ++i) {
            for (int index = 0; index < pages[i].length; ++index) {
                Page page = files[i].pageIndex.get(index);

                Assert.assertEquals(page.fd, files[i]);
            }
        }

        // clear pages and check size
        for (int i = 0; i < files.length; ++i)
            for (int index = 0; index < pages[i].length; ++index)
                files[i].pageIndex.clear(index);

        Assert.assertEquals(files[0].dataCache.pages.size(), 0);
    }

}
