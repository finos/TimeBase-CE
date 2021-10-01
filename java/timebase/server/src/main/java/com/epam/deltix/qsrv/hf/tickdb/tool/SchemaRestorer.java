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
package com.epam.deltix.qsrv.hf.tickdb.tool;

import com.epam.deltix.qsrv.hf.tickdb.impl.TickStreamImpl;

import java.io.File;
import java.io.IOException;

/**
 * downgrade schema version from deltix.qsrv.hf.tickdb.impl.TickStreamImpl#VERSION to (deltix.qsrv.hf.tickdb.impl.TickStreamImpl#VERSION - 0.1)
 */
public class SchemaRestorer {

    public static final String PREVIOUS_VERSION = String.valueOf((Double.parseDouble(TickStreamImpl.VERSION) - 0.1));

    /**
     * @param files - paths to db folders
     */
    public static void downgrade (File[] files) {
        for (File file : files) {
            if (file.isDirectory())
                downgradeDbDir (file.listFiles());
        }
    }

    /**
     * @param files - streams folders
     */
    private static void downgradeDbDir (File[] files) {
        for (File file : files) {
            if (file.isDirectory())
                downgradeStream (file.listFiles());
        }
    }

    private static void downgradeStream (File[] files) {
        if (files == null)
            return;

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
        File backupFile = new File(file.getAbsolutePath() + "." + TickStreamImpl.VERSION + ".bak");
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