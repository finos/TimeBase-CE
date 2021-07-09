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
package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.util.io.*;
import com.epam.deltix.util.lang.Util;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 *
 */
public class TickDBFolderManager {
    static final Logger                         LOGGER =
        Logger.getLogger ("deltix.tickdb");

    static int FOLDER_HAS_STREAM_FILES  = 0x1;
    static int FOLDER_HAS_SUBFOLDERS    = 0x10;
    static int FOLDER_HAS_KNOWN_FILES   = 0x100;
    static int FOLDER_IS_EMPTY          = 0x1000;
    static int FOLDER_RENAMED           = 0x10000;

    public static class FolderInfo {
        File                                    folder;
        List <File>                             files = new ArrayList <File> ();
        
        FolderInfo (File folder) {
            this.folder = folder;                        
        }
    }

    private final boolean                       readOnly;
    private Map <File, FolderInfo>              dbDirs = new TreeMap <File, FolderInfo> ();
    private Map <String, File>                  nameToFileMap = new TreeMap <String, File> ();
    
    public TickDBFolderManager (boolean readOnly, File ... dirs) throws IOException {
        this.readOnly = readOnly;
        addDirs(dirs);
    }

    public void                             addDirs(File ... dirs) throws IOException {
        
        ArrayList<String> list = new ArrayList<String>();
        
        for (File file : dbDirs.keySet())
            list.add(file.getCanonicalPath());

        for (File dbDir : dirs) {
            if (!dbDir.isDirectory ())
                throw new FileNotFoundException (dbDir + " is not a valid folder.");

            String canonicalDir = dbDir.getCanonicalPath ();

            boolean append = true;
            for (int i = 0; i < list.size(); i++) {
                String dir = list.get(i);

                if (canonicalDir.contains(dir)) {
                    append = false;
                    break;
                } else if (dir.contains(canonicalDir)) {
                    list.set(i, canonicalDir);
                    append = false;
                    break;
                }
            }

            if (append)
                list.add(canonicalDir);
        }

        for (String dir : list) {
            File file = new File(dir);
            if (!dbDirs.containsKey(file))
                addDir (file);
        }
    }

    private void                            addDir (File dir) 
        throws IOException
    {
        File                canonicalDir = dir.getCanonicalFile ();

        if (!dbDirs.containsKey (canonicalDir)) {            
            FolderInfo      fi = new FolderInfo (canonicalDir);
            File []         ff = canonicalDir.listFiles ();

            if ((getFolderState(dir) & FOLDER_HAS_STREAM_FILES) == FOLDER_HAS_STREAM_FILES) {
                String message = String.format(
                    "Folder '%s' contains stream definition file (%s) and cannot be opened as timebase.",
                    dir, dir.getName() + TickDBImpl.STREAM_EXTENSION);
                
                throw new IllegalStateException(message);
            }

            if (ff != null)
                addFiles(fi, ff, true);

            dbDirs.put (canonicalDir, fi);
        }

        addCatalogToDirSet (canonicalDir);
    }

    private boolean isDbFile(File file) {
        return file.getName().equals(TickDBImpl.CATALOG_NAME) ||
                file.getName().equals(TickDBImpl.MD_FILE_NAME) ||
                file.getName().endsWith(TickDBImpl.STREAM_EXTENSION) ||
                file.getName().endsWith(TickDBImpl.DATA_EXTENSION);
    }

    private void addFiles(FolderInfo info, File[] files, boolean isRootFolder)
        throws IOException
    {
        ArrayList<File> toRename = new ArrayList<>();

        StringBuilder   dups = null;

        for (File f : files) {
            // OK to run read-only directly from a project
            if (readOnly && f.isHidden())
                continue;
            
            if (f.isDirectory() && isRootFolder) {
                int state = getFolderState(f);
                String message = null;

                if ((state & FOLDER_IS_EMPTY) == FOLDER_IS_EMPTY) {
                    f.delete();
                } 
                else if (!readOnly) {
                    if ((state & FOLDER_HAS_STREAM_FILES) != FOLDER_HAS_STREAM_FILES) {
                        if ((state & FOLDER_HAS_KNOWN_FILES) != FOLDER_HAS_KNOWN_FILES)
                            message = String.format(
                                "Folder '%s' does not contain stream definition file (%s) as expected.",
                                f, f.getName() + TickDBImpl.STREAM_EXTENSION);
                    }

//                    else if ((state & FOLDER_HAS_SUBFOLDERS) == FOLDER_HAS_SUBFOLDERS) {
//                        message = String.format("Folder %s cannot contain any sub-folders.", f);
//                    }
                }

                if (message != null)
                    throw new IllegalStateException(message);

                boolean renamed = (state & FOLDER_RENAMED) == FOLDER_RENAMED;

                if (readOnly || !renamed) {
                    File[] ff = f.getCanonicalFile().listFiles();
                    if (ff != null)
                        addFiles(info, ff, false);
                } else if (!readOnly && renamed) {
                    toRename.add(f);
                }
            }

            if (isDbFile(f)) {
                String  name = f.getName ();
                File    dupfile = nameToFileMap.put (name, f);

                if (dupfile != null && !dupfile.equals(f)) {
                    if (dups == null)
                        dups = new StringBuilder ("Duplicate files:");

                    dups.append ("\n    ");
                    dups.append (f);
                    dups.append (" & ");
                    dups.append (dupfile);
                }
                else
                    info.files.add (f);
            }
        }

        if (dups != null)
            throw new IOException (dups.toString ());

        while (!toRename.isEmpty()) {
            for (int i = toRename.size() - 1; i >= 0; i--) {
                File f = rename(toRename.get(i));

                if (f != null) {
                    toRename.remove(i);
                    File[] ff = f.getCanonicalFile().listFiles();
                    if (ff != null)
                        addFiles(info, ff, false);
                }
            }
        }
    }

    private void                            addCatalogToDirSet (
        File                                    inDir
    )
        throws IOException
    {
        File            cat = new File (inDir, TickDBImpl.CATALOG_NAME);

        try {
            for (String path : IOUtil.readLinesFromTextFile (cat)) {
                File    folder = new File (path.trim ());

                if (folder.isDirectory ())
                    addDirs (folder);
                else
                    LOGGER.warning ("Folder " + path + " referenced in " + cat + " is invalid.");
            }
        } catch (FileNotFoundException x) {
            // do nothing
        } catch (InterruptedException ix) {
            throw new com.epam.deltix.util.io.UncheckedIOException(ix);
        }
    }

    public Set <File>                   getDbDirSet () {
        return (dbDirs.keySet ());
    }

    public void                         createCatalogs ()
        throws IOException
    {
        for (File dbDir : dbDirs.keySet ())
            createCatalog (dbDir);
    }

    public void                         createCatalog (File inDir)
        throws IOException
    {
        File            cat = new File (inDir, TickDBImpl.CATALOG_NAME);
        FileWriter      fwr = new FileWriter (cat);

        try {
            for (File dbDir : dbDirs.keySet ()) {
                if (inDir.equals (dbDir))
                    continue;

                fwr.write (dbDir.getAbsolutePath ());
                fwr.write ("\n");
            }
            
            fwr.close ();
            fwr = null;
        } finally {
            Util.close (fwr);
        }
    }

    public Map <String, File>           getNameToFileMap () {
        return (Collections.unmodifiableMap (nameToFileMap));
    }
    
    public void                         format (File ... newFolders)
        throws IOException
    {
        for (File f : newFolders) {
            
            if (!f.mkdirs ()) {
                File[] files = f.isDirectory() ? f.listFiles() : new File[0];
                
                for (File file : files) {

                    if (file.isDirectory()) {
                        int state = getFolderState(file);
                        
                        if ((state & FOLDER_HAS_STREAM_FILES) == FOLDER_HAS_STREAM_FILES ||
                            (state & FOLDER_HAS_KNOWN_FILES) == FOLDER_HAS_KNOWN_FILES) {
                            IOUtil.removeRecursive (file, null, true);
                        } else {
                            String message = String.format(
                                "Folder '%s' does not contain stream definition file (%s), so it cannot be formatted.",
                                file, file.getName() + TickDBImpl.STREAM_EXTENSION);

                            throw new IllegalStateException(message);
                        }
                    }
                }
            }
            
            IOUtil.removeRecursive(f, null, false);
            addDirs(f);
        }

        createCatalogs ();
    }

    /*
         Renames folder to make sure that stream and folder name are equals
      */
    public static File               rename(File folder) {

        File[] files = folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(TickDBImpl.STREAM_EXTENSION);
            }
        });

        if (files.length != 1)
            throw new IllegalStateException("Folder" + folder + " contains more that one definition file.");

        String fileName = files[0].getName();
        int index = fileName.indexOf(TickDBImpl.STREAM_EXTENSION);

        String stream = fileName.substring(0, index);

        File dest = new File(folder.getParentFile(), stream);
        int counter = 0;
        while (dest.exists())
            dest = new File(folder.getParentFile(), stream + counter++);

        TickDBImpl.LOG.warn("Renaming folder '" + folder.getParentFile().getName() + "\\" + folder.getName() +
                "' to '" + dest.getParentFile().getName() + "\\" + dest.getName() + "'");

        return folder.renameTo(dest) ? dest : null;
    }

    public static int                   getFolderState(File folder) {
        int result = 0;
        boolean hasUnknownFiles = false;

        File[] files = folder.listFiles();

        for (int i = 0; files != null && i < files.length; i++) {
            File file = files[i];

            if (file.isDirectory()) {
                result |= FOLDER_HAS_SUBFOLDERS;
                if ((getFolderState(file) & FOLDER_HAS_KNOWN_FILES) != FOLDER_HAS_KNOWN_FILES)
                    hasUnknownFiles = true;
            } else {
                String fileName = file.getName();

                int index = fileName.indexOf(TickDBImpl.STREAM_EXTENSION);

                if (index > 0) {
                    String stream = fileName.substring(0, index);
                    if (stream.startsWith(TickStreamImpl.NEW_PREFIX))
                        stream = stream.substring(TickStreamImpl.NEW_PREFIX.length());

                    if (!stream.equals(folder.getName()))
                        result |= FOLDER_RENAMED;

                    result |= FOLDER_HAS_STREAM_FILES;
                } else {
                    if (!hasUnknownFiles)
                        hasUnknownFiles = !TickDBImpl.isDBFile(folder, fileName);
                }
            }
        }

        if (!hasUnknownFiles)
            result |= FOLDER_HAS_KNOWN_FILES;

        if (files == null || files.length == 0)
            result |= FOLDER_IS_EMPTY;

        return result;
    }

}
