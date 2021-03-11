package com.epam.deltix.qsrv.hf.tickdb.tool;

import com.epam.deltix.util.io.IOUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;

/**
 * Downgrade schema version from deltix.qsrv.hf.tickdb.impl.TickStreamImpl#VERSION to (deltix.qsrv.hf.tickdb.impl.TickStreamImpl#VERSION - 0.1)
 */
public class TDBDowngrade {

    public static double        VERSION = 4.5;
    private static final String PREVIOUS_VERSION = String.valueOf(VERSION - 0.1);

    public static boolean       isActualVersion(File timebase) {
        double actual = -1;

        File[] dirs = timebase.listFiles();
        for (int i = 0; dirs != null && i < dirs.length; i++) {
            File file = dirs[i];
            try {
                if (file.isDirectory())
                    actual = Math.max(getMetaDataVersion(file), actual);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return VERSION == actual || actual == -1;
    }

    public static double getMetaDataVersion(File folder) throws IOException, InterruptedException {
        File[] files = folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".uhfq.xml");
            }
        });

        if (files != null && files.length == 1) {
            String text = IOUtil.readTextFile (files[0]);
            if (text.contains("<version>4.3</version>"))
                return 4.3;
            else if (text.contains("<version>4.4</version>"))
                return 4.4;
            else if (text.contains("<version>4.5</version>"))
                return 4.5;
        }

        return -1;
    }

    /**
     * @param files - paths to db folders
     */
    public static void downgrade (File[] files) {

        System.out.println("Downgrade from version " +
                VERSION + " to " +
                TDBDowngrade.PREVIOUS_VERSION + ".");

        for (File file : files) {
            if (file.isDirectory())
                downgradeDbDir(file.listFiles());
        }
    }

    /**
     * @param files - streams folders
     */
    private static void downgradeDbDir (File[] files) {
        if (files != null) {
            Arrays.sort(files);

            for (File file : files) {
                if (file.isDirectory()) {
                    File[] list = file.listFiles();
                    downgradeStream(list);
                }
            }
        }
    }

    private static void downgradeStream (File[] files) {
        if (files == null)
            return;

        Arrays.sort(files);

        File mainFile = null;
        File backupFile = null;

        for (File file : files) {
            final String fileName = file.getName();
            if (isMainSchemaFile(fileName))
                mainFile = file;
            else if (isBackupSchemaFile(fileName))
                backupFile = file;
        }

        if (mainFile != null && backupFile != null)
            renameFiles(mainFile, backupFile);
        else if (files.length > 0) {
            String parentPath = files[0].getParent();
            System.err.println("Cannot downgrade stream folder: " + parentPath);

            if (mainFile == null)
                System.err.println("Reason is: file *.uhfq.xml was not found!");

            if (backupFile == null)
                System.err.println("Reason is: file *.uhfq.xml." + PREVIOUS_VERSION + ".bak was not found!");
            System.err.println();
        }

    }

    private static void renameFiles(File mainFile, File backupFile) {
        if (renameMainFile(mainFile)) {
           if (!renameBackupFile(backupFile)) {
               System.err.println("Cannot downgrade stream folder: " + backupFile.getParent());
               System.err.println("Reason is: cannot rename " + backupFile.getName());
               if (!restoreMainFile(mainFile))
                   System.err.println("Cannot restore " + mainFile.getName());
               System.err.println();
           } else
               System.out.println("File " + mainFile.getName() + " downgrade successfully!");
        } else {
            System.err.println("Cannot downgrade stream folder: " + mainFile.getParent());
            System.err.println("Reason is: cannot rename " + mainFile.getName());
            System.err.println();
        }
    }

    private static boolean restoreMainFile(File file) {
        return getBackupMainFile(file, false).renameTo(file);
    }

    private static boolean renameMainFile(File file) {
        return file.renameTo(getBackupMainFile(file, true));
    }

    private static File getBackupMainFile (File file, boolean doDelete) {
        File backupFile = new File(file.getAbsolutePath() + "." + VERSION + ".bak");
        if (doDelete && backupFile.exists())
            backupFile.delete();
        return backupFile;
    }

    private static boolean renameBackupFile(File file) {
        String filePath = file.getAbsolutePath();
        int pos = filePath.indexOf("." + PREVIOUS_VERSION + ".bak");
        return (pos != -1) && file.renameTo(new File(filePath.substring(0, pos)));
    }

    private static boolean isMainSchemaFile (String fileName) {
        return fileName.endsWith(".uhfq.xml");
    }

    private static boolean isBackupSchemaFile (String fileName) {
        return fileName.endsWith(".uhfq.xml." + PREVIOUS_VERSION + ".bak");
    }

}
