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
package com.epam.deltix.qsrv.dtb.store.impl;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.qsrv.dtb.fs.pub.FSUtils;
import com.epam.deltix.qsrv.dtb.store.codecs.TSNames;
import com.epam.deltix.qsrv.dtb.store.impl.IndexInfo.ChildInfo;
import com.epam.deltix.qsrv.dtb.store.pub.TSRoot;
import com.epam.deltix.qsrv.dtb.store.raw.MutableRawTSF;
import com.epam.deltix.qsrv.dtb.store.raw.DiagPrinter;
import com.epam.deltix.qsrv.dtb.store.raw.RawFolder;
import com.epam.deltix.qsrv.dtb.store.raw.RawTSF;

import static com.epam.deltix.qsrv.dtb.store.impl.PDSImpl.LOGGER;

public class Restorer {

    private final DiagPrinter printer = new DiagPrinter();

    //static final Logger LOGGER = Logger.getLogger("deltix.qsrv.dtb.store.impl.Restorer");

    private final AbstractPath          propertyFile;
    private final boolean       isReadOnly;

    private         Restorer(AbstractPath root, boolean isReadOnly) {
        this.isReadOnly = isReadOnly;
        this.propertyFile = root.append(TSNames.ROOT_PROPS_NAME);
    }

    /*
        Verify given root for the errors and restore consistency
     */

    public static void restore(TSRoot root, boolean isReadOnly) throws IOException {
        Restorer restorer = new Restorer(root.getPath(), isReadOnly);
        restorer.restoreFolders(root.getPath(), true);

        // root MUST contains index file
        AbstractPath indexPath = root.getPath().append(TSNames.INDEX_NAME);
        if (!indexPath.exists())
            new IndexInfo().saveTo(indexPath);
    }

    /*
        Verify given path for the errors and restore consistency
     */
    public static void restore(AbstractPath path) throws IOException {
        new Restorer(path, false).restoreFolders(path, false);
    }

    private void restoreFolders(AbstractPath entry, boolean isFirst) throws IOException {

        restoreTempFolders(entry, isFirst);

        String[] children = entry.listFolder();
        int index = 0;

        for (String name : children) {
            AbstractPath child = entry.getFileSystem().createPath(entry, name);
            if (child.isFolder()) {
                restoreFolders(child, isFirst && index == 0);

                if (!isEmpty(child) && !child.getName().startsWith("tmp"))
                    index++;
            }
        }

        restoreFolder(entry, isFirst);
    }

    /**
     * Indicates that path is not contains any actual data.
     * (It may contain empty index file only)
     * @param  path to verify
     * @return true, if path does not contain actual data
     * @throws IOException
     */
    private static boolean              isEmpty(AbstractPath path) throws IOException {
        if (path.isFolder()) {
            String[] items = path.listFolder();

            if (items == null || items.length == 0)
                return true;

            // does not contains any files or folders
            for (String name : items) {
                if (TSNames.isTSFileName(name) || TSNames.isTSFolder(name))
                    return false;
            }

            return true;
        }

        return false;
    }

    private void restoreTempFolders(AbstractPath entry, boolean isFirst) throws IOException {

        AbstractPath indexPath = entry.append(TSNames.INDEX_NAME);
        AbstractPath tmpIndexPath = entry.append(TSNames.TMP_PREFIX + TSNames.INDEX_NAME);

        String[] items = entry.listFolder();
        boolean hasUnsaved = Stream.of(items).anyMatch(x -> x.startsWith(TSNames.TMP_PREFIX));

        IndexInfo indexInfo = IndexInfo.createFrom(indexPath);

        if (indexInfo != null && hasUnsaved) {
            if (checkReadonly(entry))
                return;

            LOGGER.warn("Folder %s has unsaved data. Recovering...").with(entry.getPathString());

            IndexInfo tmpIndexInfo = IndexInfo.createFrom(tmpIndexPath);
            if (!tmpIndexPath.exists() || tmpIndexInfo == null) {
                tmpIndexPath.deleteIfExists();
                tmpIndexInfo = IndexInfo.createFrom(entry.append(TSNames.SAVE_PREFIX + TSNames.INDEX_NAME));
            }

            if (tmpIndexPath.exists() && tmpIndexInfo != null) {
                restoreConsistency(indexInfo, tmpIndexInfo, entry, isFirst);

                tmpIndexPath.deleteIfExists();
                indexInfo.saveTo(indexPath);
            } else {
                restoreFolderConsistency(indexInfo, entry);
            }
        } else if (hasUnsaved) {
            LOGGER.warn("Folder %s has unsaved data. Recover temp folders ...").with(entry.getPathString());
            // index is missing - just rename tmp folders
            Stream.of(items).filter(x -> x.startsWith(TSNames.TMP_PREFIX))
                    .forEach(x -> {
                        AbstractPath p = entry.append(x);
                        if (p.isFolder()) {
                            try {
                                AbstractPath path = p.renameTo(x.substring(TSNames.TMP_PREFIX.length()));
                                LOGGER.warn("Rename temporary path to %s").with(path);
                            } catch (IOException e) {
                                LOGGER.warn("Failed to rename %s. Error: %s").with(p).with(e);
                            }
                        }
                    });
        }
    }

    private void        dropFiles(List<AbstractPath> skipped)  {
        for (AbstractPath p : skipped) {
            LOGGER.warn("Delete unused path: %s").with(p);

            try {
                if (p.isFile())
                    p.deleteIfExists();
                else if (p.isFolder())
                    FSUtils.removeRecursive(p, true, null);
            } catch (IOException e) {
                LOGGER.warn("Failed to delete %s. Error: %s").with(p).with(e);
            }
        }
    }

    private boolean checkReadonly(AbstractPath entry) {
        if (isReadOnly)
            LOGGER.warn("index for %s is not valid. Please turn off ReadOnly mode and restart TimeBase Server.").with(entry.getPathString());
        return isReadOnly;
    }

    private void restoreFolder(AbstractPath entry, boolean isFirst) throws IOException {

        AbstractPath indexPath = entry.append(TSNames.INDEX_NAME);
        IndexInfo indexInfo = IndexInfo.createFrom(indexPath);

        if (indexInfo == null) {
            if (checkReadonly(entry))
                return;

            ArrayList<AbstractPath> skipped = new ArrayList<>();
            LOGGER.warn("Missing or invalid index: %s. Restoring ...").with(indexPath);
            indexInfo = rebuildIndex(indexPath, isFirst, skipped);

            if (indexInfo != null)
                indexInfo.saveTo(indexPath);

            dropFiles(skipped);

        } else {
            if (!verifyIndex(indexInfo, entry)) {
                if (checkReadonly(entry))
                    return;

                ArrayList<AbstractPath> skipped = new ArrayList<>();

                indexInfo = restoreIndex(indexInfo, indexPath, isFirst, skipped);
                if (indexInfo != null) {
                    indexInfo.saveTo(indexPath);
                    dropFiles(skipped);
                    LOGGER.warn("index.dat for %s corrected.").with(entry.getPathString());
                }
            } else {
                restoreFolderConsistency(indexInfo, entry);
                synchronizeIndexWithFolder(indexInfo, entry, isFirst);
                indexInfo.saveTo(indexPath);
            }
        }

        LOGGER.trace("Validation finished.");
    }

    private void restoreConsistency(IndexInfo indexInfo, IndexInfo tmpIndexInfo, AbstractPath entry, boolean isFirst) throws IOException {
        restoreFolderConsistency(indexInfo, entry);
        //restoreIndex(tmpIndexInfo, entry, isFirst);

//        synchronizeIndexWithFolder(indexInfo, entry, isFirst);
//
//        if (tmpIndexInfo == null)
//            return;
//
//        List<ChildInfo> children = indexInfo.getChildrenInfo();
//        clearFolder(children, entry);
    }

    private IndexInfo restoreIndex(IndexInfo index, AbstractPath path, boolean isFirst, List<AbstractPath> skipped) throws IOException {
        AbstractPath parentPath = path.getParentPath();

        IndexInfo actual = rebuildIndex(path, isFirst, null);

        if (actual != null) {
            ArrayList<ChildInfo> toAdd = new ArrayList<>();

            // saved index maybe ahead of current index (not contains deleted files or not saved files)
            List<ChildInfo> current = index.getChildrenInfo();
            for (ChildInfo c : current) {
                ChildInfo info = actual.getChild(c.eid);
                if (info != null)
                    toAdd.add(info);
                else
                    skipped.add(parentPath.append(c.getName()));
            }

            // re-add files to validate timestamps order
            index.setChildrenInfo(new ArrayList<>());

            for (ChildInfo c : toAdd) {
                if (!index.addChild(c))
                    LOGGER.warn("Cannot add path to index: %s").with(path);
            }

            if (isFirst && index.hasChildren())
                index.getChildrenInfo().get(0).ts = Long.MIN_VALUE;

            rebuildEntities(index, parentPath);
            index.setVersion(index.getVersion() + 1);

            return index;
        }

        return null;
    }

    private IndexInfo rebuildIndex(AbstractPath path, boolean isFirst, ArrayList<AbstractPath> skipped) throws IOException {

        IndexInfo index = new IndexInfo();

        AbstractPath parentPath = path.getParentPath();
        for (String name : parentPath.listFolder()) {

            if (TSNames.isTSFileName(name))  {
                ChildInfo info = getFileInfo(name, parentPath);
                if (info != null) {
                    if (!index.addChild(info))
                        LOGGER.warn("Cannot add file to index: %s").with(parentPath.append(name));
                } else if (skipped != null) {
                    skipped.add(parentPath.append(name));
                }

            } else if (TSNames.isTSFolder(name))  {
                ChildInfo info = getFolderInfo(name, parentPath);
                if (info != null) {
                    if (!index.addChild(info))
                        LOGGER.warn("Cannot add folder to index: %s").with(parentPath.append(name));
                } else if (skipped != null) {
                    skipped.add(parentPath.append(name));
                }
            }
        }

        if (isFirst && index.hasChildren())
            index.getChildrenInfo().get(0).ts = Long.MIN_VALUE;

        if (!index.hasChildren())
            index.setVersion(-1);
        else
            index.setVersion(index.getVersion() + 1);

        rebuildEntities(index, parentPath);

        return index.hasChildren() || path.exists() ? index : null;
    }

    private void rebuildEntities(IndexInfo index, AbstractPath parentPath) {
        // clear current info
        index.getEntitiesInfo().clear();

        List<ChildInfo> list = index.getChildrenInfo();

        for (int i = 0; i < list.size(); i++) {
            ChildInfo child = list.get(i);

            if (child.isFile) {
                String name = TSNames.buildFileName(child.eid);
                updateFileEntities(child.eid, parentPath.append(name), index);
            } else {
                String name = TSNames.buildFolderName(child.eid);
                updateFolderEntities(child.eid, parentPath.append(name), index);
            }
        }
    }

    private ChildInfo getFileInfo(String name, AbstractPath parentPath) {
        AbstractPath path = parentPath.append(name);
        try {
            RawTSF raw = new RawTSF ();
            raw.setPath (path);
            raw.readIndex(printer);

            long minTimestamp = raw.getMinTimestamp();

            if (raw.getNumEntities() == 0 || minTimestamp == Long.MAX_VALUE) {
                LOGGER.warn("Skipping file with no data: %s").with(path);
                return null;
            }

            return new ChildInfo(true, TSNames.getEID(name), minTimestamp, raw.getVersion());
        } catch (IOException ex) {
            LOGGER.warn("Cannot read file %s: %s").with(path).with(ex);
            return null;
        }
    }

    private ChildInfo getFolderInfo(String name, AbstractPath parentPath) {
        IndexInfo subIndex = IndexInfo.createFrom(parentPath.append(name).append(TSNames.INDEX_NAME));

        if (subIndex != null && subIndex.hasChildren())
            return new ChildInfo(false, TSNames.getEID(name), subIndex.getChildrenInfo().get(0).ts, subIndex.getVersion());

        return null;
    }

    private void    updateFileEntities(int eid, AbstractPath path, IndexInfo index) {
        try {
            RawTSF raw = new RawTSF ();
            raw.setPath (path);
            raw.readIndex(printer);

            for (int i = 0; i < raw.getNumEntities(); i++) {
                int entity = raw.getEntity(i);
                long time = raw.getBlock(i).getFirstTimestamp();

                if (time == Long.MAX_VALUE) // no data
                    continue;

                IndexInfo.EntityInfo info = index.findEntity(entity);
                if (info == null) {
                    index.addEntity(entity, eid);
                } else {
                    long start = index.getChild(info.firstId).ts;
                    long end = index.getChild(info.lastId).ts;

                    if (time < start)
                        info.firstId = eid;
                    if (time > end)
                        info.lastId = eid;
                }
            }
        } catch (IOException ex) {
            LOGGER.warn("Cannot read file %s: %s").with(path).with(ex);
        }
    }

    private void updateFolderEntities(int eid, AbstractPath path, IndexInfo index) {
        IndexInfo subIndex = IndexInfo.createFrom(path.append(TSNames.INDEX_NAME));

        // assuming that index is valid here, because we already read it before
        assert subIndex != null;

        if (subIndex.hasChildren()) {
            List<IndexInfo.EntityInfo> list = subIndex.getEntitiesInfo();

            long startTime = subIndex.getChildrenInfo().get(0).ts;

            for (int i = 0; i < list.size(); i++) {
                int entity = list.get(i).entity;

                IndexInfo.EntityInfo info = index.findEntity(entity);

                if (info != null) {
                    long firstIdTS = index.getChild(info.firstId).ts;
                    long lastIdTS = index.getChild(info.lastId).ts;

                    if (startTime < firstIdTS)
                        info.firstId = eid;
                    if (startTime > lastIdTS)
                        info.lastId = eid;
                } else {
                    index.addEntity(entity, eid);
                }
            }
        }
    }

    private void synchronizeIndexWithFolder(IndexInfo indexInfo, AbstractPath path, boolean isFirst) throws IOException {

        List<IndexInfo.ChildInfo> children = indexInfo.getChildrenInfo();
        List<IndexInfo.ChildInfo> toRemove = new ArrayList<>();

        boolean     firstChild = isFirst;

        for (int i = 0; i < children.size(); ++i) {
            IndexInfo.ChildInfo curChild = children.get(i);
            AbstractPath curPath = getChild(path, curChild);

            if (!curPath.exists()) {
                toRemove.add(curChild);
                LOGGER.warn("File '" + curPath.getName() + "' will be removed from index.");
            } else {
                long time = getStartTime(curPath, curChild.ts);

                if ((time != curChild.ts) || (!firstChild && time == Long.MIN_VALUE) || (firstChild && time != Long.MIN_VALUE)) {
                    if (time != curChild.ts) {
                        if (!(curChild.ts == Long.MIN_VALUE && firstChild))
                            LOGGER.warn(curPath + " time " + time + " is different than index: " + curChild.ts);
                    }

                    if (curPath.isFile()) {
                        MutableRawTSF tsf = new MutableRawTSF(propertyFile);
                        tsf.setPath(curPath);

                        if (!firstChild) {
                            time = tsf.resetStartTS();
                            tsf.saveTo();
                        }
                    }

                    if (firstChild && time != Long.MIN_VALUE)
                        time = Long.MIN_VALUE;

                    curChild.ts = time;
                }

                if (isFirst && firstChild) // reset to false
                    firstChild = false;
            }
        }

        if (toRemove.size() > 0) {
            LOGGER.info(toRemove.size() + " children are removed.");
            indexInfo.removeAll(toRemove);
        }
    }

    private boolean         verifyIndex(IndexInfo index, AbstractPath folder) throws IOException {

        HashSet<String> files = new HashSet<>(Arrays.asList(folder.listFolder()));
        List<ChildInfo> children = index.getChildrenInfo();

        for (ChildInfo c : children) {
            AbstractPath path = getChild(folder, c);

            if (!files.contains(path.getName()))
                return false;

            long fileVersion = getVersion(path, c.ts);
            if (c.version != fileVersion || fileVersion == -1) {
                LOGGER.warn("File " + path + " version(" + fileVersion + ") != " + " index version(" + c.version + ")");
                return false;
            }
        }

        int formatVersion = index.getFormatVersion();

        if (formatVersion < 2)
            LOGGER.warn("Index in %s has old format (version = %s). Rebuilding ...").with(folder).with(formatVersion);

        return formatVersion >= 2;

    }

    private String getRelativePath(AbstractPath path) {
        return path.toString();
    }

//    private void clearFolder(List<ChildInfo> children, AbstractPath path) throws IOException {
//        String[] files = path.listFolder();
//        LOGGER.log(Level.INFO, "Checking files absent in 'index.dat'.");
//
//        boolean needRemove;
//        for (int i = 0; i < files.length; i++) {
//            if (files[i].equals(TSNames.INDEX_NAME) || files[i].equals(TSNames.ROOT_PROPS_NAME) || (files[i].equals(TSNames.SYM_REGISTRY_NAME))) {
//                continue;
//            }
//            needRemove = true;
//            for (ChildInfo child : children) {
//                AbstractPath curPath = getChild(path, child);
//
//                if (curPath.getName().equals(files[i]))
//                    needRemove = false;
//            }
//            AbstractPath entryToRemove = path.append(files[i]);
//            if (needRemove) {
//                if(entryToRemove.isFile()) {
//                    entryToRemove.deleteIfExists();
//                    LOGGER.log(Level.INFO, "File '" + files[i] + "' is removed.");
//                } else {
//                    FSUtils.removeRecursive(entryToRemove, true, null);
//                    LOGGER.log(Level.INFO, "Folder '" + files[i] + "' is removed.");
//                }
//            }
//        }
//    }

    private void restoreFolderConsistency(IndexInfo indexInfo, AbstractPath entry) throws IOException {
        List<IndexInfo.ChildInfo> children = indexInfo.getChildrenInfo();

        List<AbstractPath> tmpFoldersList = getTmpFolders(children, entry);
        //LOGGER.log(Level.INFO, "Restoring folder consistency.");

        int numMovedFiles = 0;

        for (int i = 0; i < children.size(); ++i) {
            IndexInfo.ChildInfo curChild = children.get(i);
            AbstractPath curPath = getChild(entry, curChild);
            if (!curPath.exists()) {
                // try to find file in foldersList
                AbstractPath filePath = null;
                for (AbstractPath folder : tmpFoldersList) {
                    AbstractPath curFilePath = getChild(folder, curChild);

                    if (curFilePath.exists()) {
                        filePath = curFilePath;
                        break;
                    }
                }

                if (filePath != null) {
                    LOGGER.info("Entry '" + filePath.getName() + "' is moved from '" + filePath.getParentPath() + "' to '" + curPath.getParentPath() + "'.");
                    filePath.moveTo(curPath);
                    numMovedFiles++;
                }
            } else {
                if (curPath.isFile())
                    continue;

                AbstractPath tmpPath = curPath.getFileSystem().createPath(curPath.getParentPath(), TSNames.TMP_PREFIX + curPath.getName());
                if (tmpPath.exists()) {
                    //Move from one folder to another;
                    LOGGER.info("Moving from folder '" + tmpPath.getName() + "' to '" + curPath.getName() + "'.");
                    moveFromTemp(tmpPath, curPath);
                }
            }
        }

        if (numMovedFiles != 0)
            LOGGER.info(numMovedFiles + " entries are restored.");

        dropFiles(tmpFoldersList);
    }

    private long    getStartTime(AbstractPath path, long indexTime) {
        if (path.isFile()) {
            MutableRawTSF file = new MutableRawTSF(propertyFile);
            file.setPath(path);

            try {
                if (path.exists()) {
                    file.readIndex(indexTime, Long.MAX_VALUE, printer);
                    return file.getMinTimestamp();
                }
            } catch (IOException e) {
                LOGGER.warn("Cannot read file %s: %s").with(path).with(e);
            }
        } else if (path.isFolder()) {
            RawFolder folder = new RawFolder();
            folder.setPath(path);
            try {
                if (folder.getIndexPath().exists()) {
                    folder.readIndex(indexTime, Long.MAX_VALUE, printer);
                    // getting time from first child
                    if (folder.getNumChildren() > 0)
                        return getStartTime(path.append(folder.getChild(0).getName()), indexTime);
                }
            } catch (IOException e) {
                LOGGER.warn("Cannot read folder %s: %s").with(path).with(e);
            }
        }

        return Long.MIN_VALUE;
    }

    private long    getVersion (AbstractPath path, long indexTime) {
        if (path.isFile()) {
            MutableRawTSF file = new MutableRawTSF(propertyFile);
            file.setPath(path);

            try {
                if (path.exists()) {
                    file.readIndex(indexTime, Long.MAX_VALUE, new DiagPrinter());
                    return file.getVersion();
                }
            } catch (IOException e) {
                LOGGER.warn("Cannot read file %s: %s").with(path).with(e);
            }
        } else if (path.isFolder()) {
            RawFolder folder = new RawFolder();
            folder.setPath(path);
            try {
                if (folder.getIndexPath().exists()) {
                    folder.readIndex(indexTime, Long.MAX_VALUE, new DiagPrinter());
                    // getting time from first child
                    return folder.getVersion();
                }
            } catch (IOException e) {
                LOGGER.warn("Cannot read index in %s: %s").with(path).with(e);
            }
        }

        return -1;
    }

    private void moveFromTemp(AbstractPath tmpPath, AbstractPath curPath) throws IOException {
        String[] files = tmpPath.listFolder();
        if (files == null || files.length == 0)
            return;

        int count = 0;
        for (int ii = 0; ii < files.length; ii++) {
            if (!files[ii].contains("index")) {

                AbstractPath tmpFilePath = tmpPath.getFileSystem().createPath(tmpPath, files[ii]);
                AbstractPath tmpFilePathTo = curPath.getFileSystem().createPath(curPath, files[ii]);
                tmpFilePath.moveTo(tmpFilePathTo);
                LOGGER.warn("File '" + tmpFilePath.getName() + "' is moved from '" + tmpFilePath.getParentPath() + "' to '" + tmpFilePath.getParentPath().getPathString() + "'.");
                count++;
            }

        }
        LOGGER.info("Totally: " + count + " entries are moved back. ");
    }

    private List<AbstractPath> getTmpFolders(List<ChildInfo> children, AbstractPath entry) throws IOException {
        String[] folders = entry.getFileSystem().createPath(entry.getPathString()).listFolder();
        List<AbstractPath> foldersList = new ArrayList<>();

        for (int j = 0; j < folders.length; ++j) {
            AbstractPath folderPath = entry.getFileSystem().createPath(entry, folders[j]);

            boolean found = false;
            for (int i = 0; i < children.size(); ++i) {
                IndexInfo.ChildInfo curChild = children.get(i);
                AbstractPath childPath = getChild(entry, curChild);

                if (childPath.getName().equals(folderPath.getName())) {
                    found = true;
                    break;
                }
            }

            if (!found && folderPath.isFolder())
                foldersList.add(folderPath);
        }

        return foldersList;
    }

    private AbstractPath     getChild(AbstractPath parent, IndexInfo.ChildInfo child) {
        return parent.getFileSystem().createPath(parent,
                (child.isFile ? TSNames.buildFileName(child.eid) : TSNames.buildFolderName(child.eid)));
    }

//    private static int      getFolderSize(AbstractPath entry) throws IOException {
//        String[] names = entry.listFolder();
//
//        int count = 0;
//        for (int i = 0; i < names.length; i++) {
//            if (TSNames.isTSFolder(names[i]) || TSNames.isTSFileName(names[i]))
//                count++;
//        }
//        return count;
//
//    }
}
