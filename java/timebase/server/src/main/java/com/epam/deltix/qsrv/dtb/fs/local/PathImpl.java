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
package com.epam.deltix.qsrv.dtb.fs.local;

import com.epam.deltix.qsrv.dtb.fs.pub.*;
import com.epam.deltix.util.io.*;
import com.epam.deltix.util.lang.Util;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.Arrays;

/**
 *
 */
class PathImpl extends File implements AbstractPath {

    private final static String LONG_PATH_PREFIX = "\\\\?\\"; // for windows only
    private final AbstractFileSystem fs;

    PathImpl (String pathname, AbstractFileSystem fs) {
        super (wrapLongName(pathname));
        this.fs = fs;
    }   

    PathImpl (PathImpl parent, String name, AbstractFileSystem fs) {
        super (wrapLongName(parent.getAbsolutePath()), name);
        this.fs = fs;
    }

    private static String              wrapLongName(String path) {
        if (path.length() >= 256 && Util.IS_WINDOWS_OS) {
            if (!path.startsWith(LONG_PATH_PREFIX))
                path = LONG_PATH_PREFIX + path;
        }

        return path;
    }

    @Override
    public AbstractFileSystem   getFileSystem () {
        return fs;
    }

    @Override
    public String               getPathString () {
        return getPath();
    }
    
    @Override
    public String []        listFolder () throws IOException {
        if (!isDirectory ())
            throw new IOException ("Not a folder: " + toString ());
        
        String []               children = list ();
        
        if (children == null)
            throw new FileNotFoundException (toString ());

        Arrays.sort(children);
        
        return (children);
    }

    @Override
    public boolean          isFolder () {
        return (isDirectory ());
    }    
    
    @Override
    public PathImpl         getParentPath () {
        return (new PathImpl (getParent (), fs));
    }
    
    @Override
    public PathImpl         append (String name) {
        String []   components = name.split ("[/\\\\]");
        PathImpl    ret = this;
        
        for (String s : components) {
            switch (s) {
                case ".":   break;
                case "..":  ret = ret.getParentPath (); break;
                default:    ret = new PathImpl (ret, s, fs); break;
            }            
        }
        
        return (ret);
    }

    @Override
    public InputStream      openInput (long offset) throws IOException {
        FileInputStream     fis = new FileInputStream (this);
        
        if (offset != 0)
            fis.skip (offset);
                
        return (fis);
    }
    
    @Override
    public OutputStream     openOutput (long size) throws IOException {
        if (size < 0)
            return (new FileOutputStream (this));
        else {
            RandomAccessFile    raf = new RandomAccessFile (this, "rw");
            raf.setLength (size);
            
            return new RAFAdapter(raf, this);
        }
    }

    @Override
    public void             makeFolder () throws IOException {
        if (!mkdir ())
            throw new IOException ("Failed to create " + this);

        syncFolder(this);
    }

    @Override
    public void             makeFolderRecursive () throws IOException {
        if (!isDirectory () && !mkdirs ())
            throw new IOException ("Failed to create " + this);

        syncFolder(this);
    }

    @Override
    public void             moveTo (AbstractPath newPath) throws IOException {
        newPath = Util.unwrap(newPath);

        if (newPath.exists())
            throw new IOException ("Failed to move " + this + " --> " + newPath + " already exists");

        if (!renameTo ((PathImpl) newPath))
            throw new IOException ("Failed to move " + this + " --> " + newPath);

        PathImpl parent1 = getParentPath();
        syncFolder(parent1);

        PathImpl parent2 = (PathImpl)newPath.getParentPath();
        if (parent1.equals(parent2))
            syncFolder(parent2);
    }
    
    @Override
    public PathImpl         renameTo (String newName) throws IOException {
        PathImpl    newPath = new PathImpl (getParentPath(), newName, fs);

        if (newPath.exists())
            throw new IOException ("Failed to rename " + this + " --> " + newPath + " already exists");
        
        if (!renameTo (newPath))
            throw new IOException ("Failed to rename " + this + " --> " + newPath);

        syncFolder(getParentPath());
        
        return (newPath);
    }

    @Override
    public void             deleteExisting () throws IOException {
        if (!delete ())
            throw new IOException ("Failed to delete " + this);

        syncFolder(getParentPath());
    }
    
    @Override
    public void             deleteIfExists () throws IOException {
        if (exists () && !delete ())
            throw new IOException ("Failed to delete " + this);

        syncFolder(getParentPath());
    }

    static void syncFolder(PathImpl file) throws IOException {
        fsync(file.toPath(), true);
    }

    static void fsync(Path fileToSync, boolean isDir) throws IOException {
        // If the file is a directory we have to open read-only, for regular files we must open r/w for the fsync to have an effect.
        // See http://blog.httrack.com/blog/2013/11/15/everything-you-always-wanted-to-know-about-fsync/

        if (isDir && Constants.WINDOWS) {
            // opening a directory on Windows fails, directories can not be fsynced there
            if (!Files.exists(fileToSync)) {
                // yet do not suppress trying to fsync directories that do not exist
                throw new NoSuchFileException(fileToSync.toString());
            }
            return;
        }

        try (final FileChannel file = FileChannel.open(fileToSync, isDir ? StandardOpenOption.READ : StandardOpenOption.WRITE)) {
            try {
                file.force(true);
            } catch (final IOException e) {
                if (isDir) {
                    assert !(Constants.LINUX || Constants.MAC_OS_X) :
                            "On Linux and MacOSX fsyncing a directory should not throw IOException, " +
                                    "we just don't want to rely on that in production (undocumented). Got: " + e;
                    // Ignore exception if it is a directory
                    return;
                }
                // Throw original exception
                throw e;
            }
        }
    }
}
