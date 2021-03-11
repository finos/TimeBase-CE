package com.epam.deltix.test.qsrv.hf.tickdb;


import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TomcatServer;
import com.epam.deltix.test.qsrv.hf.tickdb.ui.tbshell.TickDBShellTestAccessor;
import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.qsrv.hf.tickdb.util.ZIPUtil;
import com.epam.deltix.util.lang.StringUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.*;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category(JUnitCategories.TickDBFast.class)
public class Test_Downgrade {

    private static final String DB_TO_DOWNGRADE = Home.getPath("testdata", "tickdb", "misc", "db_for_downgrade.zip");


    private static String createDB(String zipFileName) throws IOException, InterruptedException {
        File folder = new File(TDBRunner.getTemporaryLocation());

        FileInputStream is = new FileInputStream(zipFileName);
        ZIPUtil.extractZipStream(is, folder);
        is.close();

        return folder.getAbsolutePath();
    }

    @Test
    public void testDowngrade() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream old = System.out;


        TickDBShellTestAccessor tickDBShell = new TickDBShellTestAccessor();
        String tempDbLocation = createDB(DB_TO_DOWNGRADE);
        tickDBShell.doCommand("set", "db " + tempDbLocation);

        System.setOut(ps);
        tickDBShell.dbmgr.doCommand("downgrade", "");

        // Put things back
        System.out.flush();
        System.setOut(old);

        String[] expected = new String[] {
                "Downgrade from version 4.5 to 4.4.",
                "File __b_ars.uhfq.xml downgrade successfully!",
                "File __bbo_s.uhfq.xml downgrade successfully!",
                "File __c_ustoms.uhfq.xml downgrade successfully!",
                "File __l_evel2.uhfq.xml downgrade successfully!",
                "File __t_rades.uhfq.xml downgrade successfully!",
                ""
        };

        assertEquals(StringUtils.join(System.lineSeparator(), expected), baos.toString());

        //System.out.println(baos.toString());

        checkFiles(tempDbLocation, true, false, true);
    }

    private void checkFiles(String dbLocation, boolean isMainBarsExists, boolean isBackup44BarsExists, boolean isBackup45BarsExists) {
        assertTrue(new File(dbLocation + "/__bbo_s/__bbo_s.uhfq.xml").exists());
        assertTrue(new File(dbLocation + "/__bbo_s/__bbo_s.uhfq.xml.4.5.bak").exists());

        if (isMainBarsExists)
            assertTrue(new File(dbLocation + "/__b_ars/__b_ars.uhfq.xml").exists());

        if (isBackup45BarsExists)
            assertTrue(new File(dbLocation + "/__b_ars/__b_ars.uhfq.xml.4.5.bak").exists());

        if (isBackup44BarsExists)
            assertTrue(new File(dbLocation + "/__b_ars/__b_ars.uhfq.xml.4.4.bak").exists());


        assertTrue(new File(dbLocation + "/__c_ustoms/__c_ustoms.uhfq.xml").exists());
        assertTrue(new File(dbLocation + "/__c_ustoms/__c_ustoms.uhfq.xml.4.5.bak").exists());

        assertTrue(new File(dbLocation + "/__l_evel2/__l_evel2.uhfq.xml").exists());
        assertTrue(new File(dbLocation + "/__l_evel2/__l_evel2.uhfq.xml.4.5.bak").exists());

        assertTrue(new File(dbLocation + "/__t_rades/__t_rades.uhfq.xml").exists());
        assertTrue(new File(dbLocation + "/__t_rades/__t_rades.uhfq.xml.4.5.bak").exists());
    }

    private void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists())
            file.delete();
        else
            fail("File " + filePath + "must be exists!");
    }

    @Test
    public void testDowngradeWithoutMainSchemaFile() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream old = System.err;


        TickDBShellTestAccessor tickDBShell = new TickDBShellTestAccessor();
        String tempDbLocation = createDB(DB_TO_DOWNGRADE);
        tickDBShell.doCommand("set", "db " + tempDbLocation);

        deleteFile(tempDbLocation + "/__b_ars/__b_ars.uhfq.xml");

        System.setErr(ps);
        tickDBShell.dbmgr.doCommand("downgrade", "");


        // Put things back
        System.out.flush();
        System.setErr(old);
        String[] expected = new String[] {
                "Cannot downgrade stream folder: " + Paths.get(tempDbLocation,"__b_ars"),
                "Reason is: file *.uhfq.xml was not found!",
                "",
                ""
        };

        assertEquals(StringUtils.join(System.lineSeparator(), expected),baos.toString());
        System.err.println(baos.toString());

        checkFiles(tempDbLocation, false, true, false);
    }

    @Test
    public void testDowngradeWithoutBackupSchemaFile() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream old = System.err;


        TickDBShellTestAccessor tickDBShell = new TickDBShellTestAccessor();
        String tempDbLocation = createDB(DB_TO_DOWNGRADE);
        tickDBShell.doCommand("set", "db " + tempDbLocation);

        deleteFile(tempDbLocation + "/__b_ars/__b_ars.uhfq.xml.4.4.bak");

        System.setErr(ps);
        tickDBShell.dbmgr.doCommand("downgrade", "");


        // Put things back
        System.out.flush();
        System.setErr(old);
        String[] expected = new String[] {
                "Cannot downgrade stream folder: " + Paths.get(tempDbLocation,"__b_ars"),
                "Reason is: file *.uhfq.xml.4.4.bak was not found!",
                "",
                ""
        };

        assertEquals(StringUtils.join(System.lineSeparator(), expected), baos.toString());

        System.err.println(baos.toString());

        checkFiles(tempDbLocation, true, false, false);
    }

    @Test
    public void testDowngradeOnRemoteTDB() throws Exception {
        TickDBShellTestAccessor tickDBShell = new TickDBShellTestAccessor();
        String tempDbLocation = createDB(DB_TO_DOWNGRADE);
        TDBRunner runner = new TDBRunner(true, false, tempDbLocation, new TomcatServer());
        runner.startup();

        tickDBShell.doCommand("set", "db dxtick://localhost:" + runner.getPort());
        try {
            tickDBShell.dbmgr.doCommand("downgrade", "");
        } catch (UnsupportedOperationException ex) {
            assertEquals("Not allowed on client", ex.getMessage());
        }
        runner.close();
    }

    @Test
    public void testDoubleDowngrade() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream old = System.out;


        TickDBShellTestAccessor tickDBShell = new TickDBShellTestAccessor();
        String tempDbLocation = createDB(DB_TO_DOWNGRADE);
        tickDBShell.doCommand("set", "db " + tempDbLocation);

        System.setOut(ps);
        tickDBShell.dbmgr.doCommand("downgrade", "");


        // Put things back
        System.out.flush();
        System.setOut(old);
        String[] expected = new String[]{
                "Downgrade from version 4.5 to 4.4.",
                "File __b_ars.uhfq.xml downgrade successfully!",
                "File __bbo_s.uhfq.xml downgrade successfully!",
                "File __c_ustoms.uhfq.xml downgrade successfully!",
                "File __l_evel2.uhfq.xml downgrade successfully!",
                "File __t_rades.uhfq.xml downgrade successfully!",
                ""
        };

        assertEquals(StringUtils.join(System.lineSeparator(), expected), baos.toString());
        System.out.println(baos.toString());

        checkFiles(tempDbLocation, true, false, true);

        //the second downgrade
        baos.reset();
        old = System.err;
        System.setErr(ps);

        tickDBShell.dbmgr.doCommand("downgrade", "");

        System.out.flush();
        System.setErr(old);

        expected = new String[] {
                "Cannot downgrade stream folder: " + Paths.get(tempDbLocation,"__b_ars"),
                "Reason is: file *.uhfq.xml.4.4.bak was not found!",
                "",
                "Cannot downgrade stream folder: " + Paths.get(tempDbLocation,"__bbo_s"),
                "Reason is: file *.uhfq.xml.4.4.bak was not found!",
                "",
                "Cannot downgrade stream folder: " + Paths.get(tempDbLocation,"__c_ustoms"),
                "Reason is: file *.uhfq.xml.4.4.bak was not found!",
                "",
                "Cannot downgrade stream folder: " + Paths.get(tempDbLocation,"__l_evel2"),
                "Reason is: file *.uhfq.xml.4.4.bak was not found!",
                "",
                "Cannot downgrade stream folder: " + Paths.get(tempDbLocation,"__t_rades"),
                "Reason is: file *.uhfq.xml.4.4.bak was not found!",
                "",
                ""
        };
        assertEquals(StringUtils.join(System.lineSeparator(), expected), baos.toString());

        System.err.println(baos.toString());


        checkFiles(tempDbLocation, true, false, true);
    }

    @Test
    public void testDowngradeOnOpenedTDB() throws Exception {

        TickDBShellTestAccessor tickDBShell = new TickDBShellTestAccessor();
        String tempDbLocation = createDB(DB_TO_DOWNGRADE);
        tickDBShell.doCommand("set", "db " + tempDbLocation);
        tickDBShell.dbmgr.doCommand("open", "");

        try {
            tickDBShell.dbmgr.doCommand("downgrade", "");
            fail("Cannot downgrade opened timebase!");
        } catch (IllegalStateException e) {
            assertEquals("Database is open.", e.getMessage());
        }
    }

}
