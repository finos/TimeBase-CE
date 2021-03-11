package com.epam.deltix.qsrv.dtb.store.impl;

import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.qsrv.dtb.store.codecs.TSNames;
import com.epam.deltix.qsrv.dtb.store.pub.EntityFilter;
import com.epam.deltix.qsrv.dtb.store.pub.EntityTimeRange;
import com.epam.deltix.util.io.UncheckedIOException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

/**
 *  Utility class, containing the implementation of tree operations
 *  that span multiple nodes. Methods of this class do not explicitly
 *  synchronize on nodes. Nodes contain synchronized methods, but never
 *  obtain locks on each other.
 */
abstract class TreeOps {
    static void                     use (TSFolderEntry e) {
        for (;;) {
            e.assertNotLockedByCurrentThread ();

            if (!e.incUseCount ())
                break;

            e = e.getParent ();

            if (e == null)
                break;
        }
    }

    static void                     unuse (TSFolderEntry e) {
        for (;;) {
            e.assertNotLockedByCurrentThread ();

            if (!e.decUseCount ())
                break;

            e = e.getParent ();

            if (e == null)
                break;
        }
    }

    /**
     *  Recursively search for the first TSF for reading data
     *  at or after the specified timestamp.
     *
     *  <p>This method returns a "used"
     *  TSF, i.e. it calls {@link TSFolderEntry#useMore()} ()} on the return value,
     *  if not null. The caller must {@link TSFolderEntry#useLess()}  the
     *  returned TSF when it's no longer needed.</p>
     *
     *  <p>Calling thread must be holding a structure lock.</p>
     *
     *  @return     A "used" TSF, or null if none found.
     */
    static TSFile                   findTSFForRead (
        TSFolder                        folder,
        long                            timestamp
    )
        throws IOException
    {
        use (folder);

        try {
            TSFolderEntry       child =
                folder.findEntry (timestamp, false);

            if (child instanceof TSFile) {
                use (child);
                return ((TSFile) child);
            } else if (child != null) {
                return (findTSFForRead ((TSFolder) child, timestamp));
            }

            return null;
        } finally {
            unuse (folder);
        }
    }

    static TSFile                   findTSFByRef (TSFolder folder, TSRefImpl tsref)
        throws IOException
    {
        return (findTSFByPath (folder, tsref.getIdPath (), 0));
    }

    private static TSFile           findTSFByPath (
        TSFolder                        folder,
        int []                          idPath,
        int                             idx
    )
        throws IOException
    {
        use (folder);

        try {
            int                 id = idPath [idx];
            TSFolderEntry       child = folder.findEntryById (id);

            if (child == null)
                throw new IllegalArgumentException (
                    "Child id " + id + " not found in " + folder +
                    " [TEMPORARILY A IllegalArgumentException]"
                );

            if (child instanceof TSFile) {
                use (child);
                return ((TSFile) child);
            }

            idx++;

            if (idx == idPath.length)
                throw new IllegalArgumentException ("Ref to TSF ends at " + child);

            return (findTSFByPath ((TSFolder) child, idPath, idx));
        } finally {
            unuse (folder);
        }
    }

    /**
     *  Recursively search for the first TSF that is suitable for inserting data
     *  at the specified timestamp, possibly creating one.
     *
     *  <p>This method returns a "used"
     *  TSF, i.e. it calls {@link TSFolderEntry#useMore()} on the return value.
     *  The caller must {@link TSFolderEntry#useLess()} the
     *  returned TSF when it's no longer needed.</p>
     *
     *  <p>Calling thread must be holding an exclusive structure lock.</p>
     *
     *  @return     A "used" TSF.
     */
    static TSFile                   findTSFForInsert (
        TSFolder                        folder,
        long                            timestamp
    )
        throws IOException
    {
        use (folder);

        try {
            TSFolderEntry       child = folder.findEntry (timestamp, true);

            if (child instanceof TSFile) {
                TSFile file = (TSFile) child;
                use (child);

                file.limitTimestamp = getLimitTimestamp(file);

                return (file);
            } else if (child instanceof TSFolder) {
                return (findTSFForInsert((TSFolder) child, timestamp));
            }

            return null;
        } finally {
            unuse (folder);
        }
    }

    /**
     *  Find next TSF for reading data after the specified one.
     *
     *  <p>This method returns a "used"
     *  TSF, i.e. it calls {@link TreeOps#use(TSFolderEntry)} on the return value,
     *  if not null. The caller must {@link TreeOps#unuse(TSFolderEntry)} the
     *  returned TSF when it's no longer needed.</p>
     *
     *  <p>Calling thread must be holding a structure lock.</p>
     *
     *  @return     A "used" TSF, or null if none found.
     */
    @Nullable
    static TSFile                   getNextFile (TSFile tsf, EntityFilter filter)
        throws IOException
    {
        // TODO: add flag to return not "used" file
        TSFolder parent = tsf.getParent();

        TSFile          nextFile = tsf;

        while (nextFile != null) {
            nextFile = (TSFile) tsf.getNextSibling ();

            if (nextFile != null && parent.isAccepted(nextFile, filter)) {
                use (nextFile);
                return (nextFile);
            } else if (nextFile != null) {
                tsf = nextFile;
            }
        }

        TSFolder        currentEntry = tsf.getParent ();
        TSFolder        nextSibling;

        for (;;) {
            nextSibling = (TSFolder) currentEntry.getNextSibling ();

            if (nextSibling != null) {
                nextFile = getFirstFile (nextSibling, filter);
                if (nextFile != null) {
                    return nextFile;
                } else {
                    currentEntry = nextSibling;
                    continue;
                }
            }

            currentEntry = currentEntry.getParent ();

            if (currentEntry == null)
                return (null);
        }
    }

    /*
        Returns next sibling having any data for given entity
     */
    static TSFolderEntry                   getNextSibling (TSFolderEntry tsf, int entity) {
        TSFolderEntry          next = tsf;

        while (next != null) {
            next = tsf.getNextSibling ();
            if (next != null && hasDataFor(next, entity))
                return next;
            else if (next != null)
                tsf = next;
        }

        return null;
    }


    /*
        Returns previous sibling having any data for given entity
     */
    static TSFolderEntry                   getPreviousSibling (TSFolderEntry tsf, int entity)
    {
        TSFolderEntry          next = tsf;

        while (next != null) {
            next = tsf.getPreviousSibling ();
            if (next != null && hasDataFor(next, entity))
                return next;
            else if (next != null)
                tsf = next;
        }

        return null;
    }

    public static boolean           hasDataFor(TSFolderEntry entry, int entity) {
        boolean used = false;
        try {
            use(entry);
            used = true;

            if (entry instanceof TSFile) {
                return ((TSFile) entry).hasDataFor(entity);
            } else if (entry instanceof TSFolder){
                TSFolderEntry next = ((TSFolder)entry).getFirstEntry();

                for (;;) {
                    if (next == null)
                        break;
                    else if (hasDataFor(next, entity))
                        return true;
                    else
                        next = next.getNextSibling();
                }
            }

        } catch (IOException e) {
            return false;
        } finally {
            if (used)
                unuse(entry);
        }

        return false;
    }

    /**
     *  Find previous TSF for reading data before the specified one.
     *
     *  <p>This method returns a "used"
     *  TSF, i.e. it calls {@link TSFolderEntry#useMore()} on the return value,
     *  if not null. The caller must {@link TSFolderEntry#useLess()} the
     *  returned TSF when it's no longer needed.</p>
     *
     *  <p>Calling thread must be holding a structure lock.</p>
     *
     *  @return     A "used" TSF, or null if none found.
     */
    static TSFile                   getPreviousFile (TSFile tsf, EntityFilter filter)
        throws IOException
    {
        TSFile          prevFile = (TSFile) tsf.getPreviousSibling ();

        if (prevFile != null) {
            use (prevFile);
            return prevFile;
        }

        TSFolder        currentEntry = tsf.getParent ();
        TSFolder        prevSibling;

        for (;;) {
            prevSibling = (TSFolder) currentEntry.getPreviousSibling ();

            if (prevSibling != null)  {
                prevFile = getLastFile (prevSibling, filter);
                if (prevFile != null) {
                    return prevFile;
                } else {
                    currentEntry = prevSibling;
                    continue;
                }
            }

            currentEntry = currentEntry.getParent ();

            if (currentEntry == null)
                return (null);
        }
    }

    static TSFile                  getFirstFile (TSFolder folder, EntityFilter filter)
        throws IOException
    {
        use (folder);

        try {
            TSFolderEntry       firstChild = folder.getFirstChildWithDataFor(filter);

            if (firstChild instanceof TSFile) {

                use(firstChild);
                return ((TSFile) firstChild);
            }

            return firstChild != null ? (getFirstFile ((TSFolder) firstChild, filter)) : null;
        } finally {
            unuse (folder);
        }
    }

    static TSFile                  getLastFile (TSFolder folder, EntityFilter filter)
        throws IOException
    {
        use (folder);

        try {
            TSFolderEntry       lastChild = folder.getLastChildWithDataFor(filter);

            if (lastChild instanceof TSFile) {
                use (lastChild);
                return ((TSFile) lastChild);
            }

            return lastChild != null ? (getLastFile ((TSFolder) lastChild, filter)) : null;
        } finally {
            unuse (folder);
        }
    }

    static long            getLimitTimestamp(TSFile file) throws IOException {

        TSFile after = null;
        try {
            after = getNextFile(file, null);

            if (after != null)
                return after.getStartTimestamp();

            return Long.MAX_VALUE;
        } finally {
            if (after != null)
                unuse(after);
        }
    }

    static void                     insertNotify (TSFolder f)
        throws IOException
    {
        if (f.hasRoomFor (1))
            return;

        TSFolder        parent = f.getParent ();
        boolean         isRoot = parent == null;

        if (isRoot)
            splitRoot ((TSRootFolder) f);
        else {
            insertNotify (parent);
            splitIntermediate (f);
        }
    }

    private static void             splitIntermediate (TSFolder f)
        throws IOException
    {
        assert f.root.currentThreadHoldsWriteLock ();

        use (f);    // prevent deactivation in the middle

        try {
            TSFolder                parent = f.getParent ();
            int                     numChildren = f.getNumChildren ();
            int                     firstLatterChild = numChildren / 2;

            long                    ts = f.getChild (firstLatterChild).getStartTimestamp ();

            TSIntermediateFolder    newFolder = parent.createFolderAfter (f, ts);

            parent.storeIndexFile();
            f.storeIndexFile();

            try {
                newFolder.setTmpName (TSNames.TMP_PREFIX + newFolder.getName ());
                newFolder.getPath ().makeFolder ();
                newFolder.moveChildrenHere (f, firstLatterChild, false);

                f.storeIndexFile();
                boolean stored = newFolder.storeDirtyData();
                if (!stored)
                    PDSImpl.LOGGER.warn(newFolder + " failed to store index!");

            } catch (Throwable ex) {
                PDSImpl.LOGGER.warn(newFolder + " error: %s").with(ex);
                throw ex;
            } finally {
                unuse (newFolder);
            }

            f.finalizeIndexFile();
            parent.finalizeIndexFile();

            newFolder.renameToNormal ();
        } finally {
            unuse (f);
        }
    }

//    private static void             splitIntermediate (TSFolder f)
//            throws IOException
//    {
//        assert f.root.currentThreadHoldsWriteLock ();
//
//        use (f);    // prevent deactivation in the middle
//
//        try {
//            TSFolder                parent = f.getParent ();
//            int                     numChildren = f.getNumChildren ();
//            int                     firstLatterChild = numChildren / 2;
//
//            long                    ts = f.getChild (firstLatterChild).getStartTimestamp ();
//
//            TSIntermediateFolder    newFolder = parent.createFolderAfter (f, ts);
//
//            try {
//                newFolder.setTmpName (TSNames.TMP_PREFIX + f.getName ()); //TODO: use newFolder.getName() instead? Otherwise temp name for child has parent's name
//                newFolder.getPath ().makeFolder ();
//
//                newFolder.moveChildrenHere (f, firstLatterChild, false);
//
//                newFolder.storeDirtyData ();
//                newFolder.renameToNormal ();
//            } finally {
//                unuse (newFolder);
//            }
//
//            storeDirtyDataInPath (f);
//        } finally {
//            unuse (f);
//        }
//    }

//    static void             storeDirtyDataInPath (TSFolder folder)
//            throws IOException
//    {
//        do {
//            if (!folder.storeDirtyData ())
//                break;
//
//            folder = folder.getParent ();
//        } while (folder != null);
//    }

    private static void             splitRoot (TSRootFolder root)
        throws IOException
    {
        assert root.currentThreadHoldsWriteLock ();

        use (root);

        try {
            int                     pivotIdx = root.getNumChildren () / 2;

            root.storeIndexFile();

            TSFolder                left = splitRootPart (root, pivotIdx, true);
            TSFolder                right = splitRootPart (root, 0, false);

            root.setChildren (left, right);
            root.storeDirtyData ();
        } finally {
            unuse (root);
        }
    }

    private static TSFolder         splitRootPart (
        TSRootFolder                    root,
        int                             pivotIdx,
        boolean                         fromLeftOfPivot
    )
        throws IOException
    {
        long                    ts =
            fromLeftOfPivot ?
                root.getStartTimestamp () :
                root.getChild (pivotIdx).getStartTimestamp ();

        TSIntermediateFolder    subf = root.createChildFolder (ts);

        try {
            subf.setTmpName (TSNames.TMP_ROOT_SUBFOLDER_NAME);
            subf.getPath ().makeFolder ();

            subf.moveChildrenHere (root, pivotIdx, fromLeftOfPivot);

            subf.storeDirtyData ();
            subf.renameToNormal ();
        } finally {
            unuse (subf);
        }

        return (subf);
    }

    static AbstractPath             makeTempPath (
        AbstractPath                    folder,
        String                          finalName
    )
    {
        return (folder.append (TSNames.TMP_PREFIX + finalName));
    }

//    static void                     recoverIfNecessary (AbstractPath actual)
//        throws IOException
//    {
//        if (!actual.exists ()) {
//            String          name = actual.getName ();
//            AbstractPath    save = 
//                actual.getParentPath ().append (TSNames.SAVE_PREFIX + name);
//            
//            if (!save.exists ())
//                return;
//            
//            save.moveTo (actual);       
//        }
//    }

    static void                     finalize (AbstractPath tmp)
        throws IOException
    {
        String          tmpName = tmp.getName ();

        if (!tmpName.startsWith (TSNames.TMP_PREFIX))
            throw new IllegalArgumentException ("Not a TMP file: " + tmp);

        AbstractPath    actual =
            tmp.getParentPath ().append (tmpName.substring (TSNames.TMP_PREFIX_LENGTH));

        // delete save.* file
        AbstractPath    save = actual.getParentPath().append(TSNames.SAVE_PREFIX + actual.getName ());
        try {
            save.deleteIfExists();
        } catch (IOException iox) {
            iox.printStackTrace(System.out);
        } finally {
            save = null;
        }

        try {
            if (actual.exists())
                save = actual.renameTo (TSNames.SAVE_PREFIX + actual.getName ());
        } catch (IOException iox) {
            iox.printStackTrace(System.out);
            save = null;
        }

        tmp.moveTo (actual);

        if (save != null)
            save.deleteExisting ();
    }

    static void         storeIndexFile(TSFolder folder) throws IOException {
        do {
            folder.storeIndexFile();
            folder = folder.getParent();
        } while (folder != null);
    }

    static void         finalizeIndex(TSFolder folder) throws Throwable {
        do {
            folder.finalizeIndexFile();
            folder = folder.getParent();
        } while (folder != null);
    }

    static void             propagateDataAddition (
        TSFolderEntry           entry,
        int                     newEntity
    )
        throws IOException
    {
        for (;;) {
            TSFolder        parent = entry.getParent ();

            if (parent == null)
                break;

            boolean         wasNewEntity = parent.dataAdded (entry, newEntity);

            if (!wasNewEntity)
                break;

            entry = parent;
        }
    }

    static void             propagateDataDeletion (
            TSFolderEntry           entry,
            int                     entity
    )
    {
        for (;;) {
            TSFolder        parent = entry.getParent ();

            if (parent == null)
                break;

            boolean         removed = parent.dataRemoved(entry, entity);

            if (!removed)
                break;

            entry = parent;
        }
    }

    static void             getFromTimestamp (
            TSFolder                entry,
            List<EntityTimeRange>   ranges
    )
            throws IOException
    {

        use (entry);

        try {
            Hashtable<TSFolderEntry, List<EntityTimeRange>> groups = new Hashtable<>();

            for (int i = 0; i < ranges.size(); i++) {
                EntityTimeRange range = ranges.get(i);

                TSFolderEntry first = entry.getFirstChildWithDataFor(range.entity);

                if (first == null)
                    continue;

                List<EntityTimeRange> list = groups.get(first);
                if (list == null)
                    groups.put(first, list = new ArrayList<EntityTimeRange>());

                list.add(range);
            }

            for (Map.Entry<TSFolderEntry, List<EntityTimeRange>> e : groups.entrySet()) {
                TSFolderEntry key = e.getKey();

                if (key instanceof TSFolder)
                    getFromTimestamp((TSFolder) key, e.getValue());
                else if (key instanceof TSFile)
                    getFromTimestamp((TSFile) key, e.getValue());
            }

        } finally {
            unuse(entry);
        }
    }

    static void             getFromTimestamp (
            TSFile                  file,
            List<EntityTimeRange>   ranges
    ) {
        use(file);

        try {
            for (int i = 0; i < ranges.size(); i++) {
                EntityTimeRange range = ranges.get(i);

                range.from = file != null ? file.getFromTimestamp(range.entity) : Long.MAX_VALUE;
            }
        } finally {
            unuse(file);
        }
    }

    static void             getToTimestamp (
            TSFolder                entry,
            List<EntityTimeRange>   ranges
    )
            throws IOException
    {
        use (entry);

        try {
            Hashtable<TSFolderEntry, List<EntityTimeRange>> groups = new Hashtable<>();

            for (int i = 0; i < ranges.size(); i++) {
                EntityTimeRange range = ranges.get(i);

                TSFolderEntry last = entry.getLastChildWithDataFor(range.entity);

                if (last == null)
                    continue;

                List<EntityTimeRange> list = groups.get(last);
                if (list == null)
                    groups.put(last, list = new ArrayList<EntityTimeRange>());

                list.add(range);
            }

            for (Map.Entry<TSFolderEntry, List<EntityTimeRange>> e : groups.entrySet()) {
                TSFolderEntry key = e.getKey();

                if (key instanceof TSFolder)
                    getToTimestamp((TSFolder) key, e.getValue());
                else
                    getToTimestamp((TSFile) key, e.getValue());
            }
        } finally {
            unuse(entry);
        }
    }

    static void             getToTimestamp (
            TSFile                  file,
            List<EntityTimeRange>   ranges
    ) {
        if (file != null)
            use(file);

        try {
            for (int i = 0; i < ranges.size(); i++) {
                EntityTimeRange range = ranges.get(i);

                range.to = file != null ? file.getToTimestamp(range.entity) : Long.MAX_VALUE;
            }
        } finally {
            if (file != null)
                unuse(file);
        }
    }

    static long             getFromTimestamp (
        TSFolderEntry           entry,
        int                     entity
    )
        throws IOException
    {
        if (entry == null)
            return (Long.MAX_VALUE);

        use (entry);

        try {
            if (entry instanceof TSFile) {
                TSFile file = (TSFile) entry;
                return file.getFromTimestamp(entity);
            }

            TSFolderEntry   down = ((TSFolder) entry).getFirstChildWithDataFor (entity);

            return (getFromTimestamp (down, entity));
        } finally {
            unuse (entry);
        }
    }

    static long             getToTimestamp (
        TSFolderEntry           entry,
        int                     entity
    )
        throws IOException
    {
        if (entry == null)
            return (Long.MIN_VALUE);

        use (entry);

        try {
            if (entry instanceof TSFile)
                return (((TSFile) entry).getToTimestamp (entity));

            TSFolderEntry   down = ((TSFolder) entry).getLastChildWithDataFor (entity);

            return (getToTimestamp (down, entity));
        } finally {
            unuse (entry);
        }
    }

    static void             tryDrop(TSFolderEntry file) {
        try {
            AbstractPath path = file.getPath();
            unuse(file);
            path.deleteIfExists();

            assert !file.isActive();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void             setStartTimestamp(TSFolder entry, long timestamp) {
        TSFolder parent = changeStartTimestamp(entry, timestamp);

        while (parent != null)
            parent = changeStartTimestamp(parent, timestamp);
    }

    // Returns valid parent folder if timestamp changed

    private static TSFolder changeStartTimestamp(TSFolder entry, long timestamp) {

        if (entry.setStartTimestamp(timestamp)) {
            entry.setDirty();
            TSFolder parent = entry.getParent();

            if (parent != null)
                parent.setDirty();

            if (parent == null || parent == entry.root)
                return null;

            if (entry.getIdxInParent() == 0)
                return parent;
        }

        return null;
    }

    static boolean   isValid(long time) {
        return (time != Long.MIN_VALUE && time != Long.MAX_VALUE);
    }
}
