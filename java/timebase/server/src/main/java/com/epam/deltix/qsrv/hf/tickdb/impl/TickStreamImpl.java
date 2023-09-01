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
package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.data.stream.*;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.codec.CachingCodecMetaFactory;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.CompiledCodecMetaFactory;
import com.epam.deltix.qsrv.hf.pub.codec.InterpretingCodecMetaFactory;
import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.comm.UnknownStreamException;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.DBLock;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockEventListener;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockType;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.StreamLockedException;
import com.epam.deltix.timebase.messages.schema.SchemaChangeMessage;
import com.epam.deltix.timebase.messages.service.EventMessage;
import com.epam.deltix.timebase.messages.service.EventMessageType;
import com.epam.deltix.timebase.messages.service.MetaDataChangeMessage;
import com.epam.deltix.timebase.messages.service.RealTimeStartMessage;
import com.epam.deltix.timebase.messages.service.StreamTruncatedMessage;
import com.epam.deltix.util.concurrent.FrequencyLimiter;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.concurrent.UncheckedInterruptedException;
import com.epam.deltix.util.io.AbstractDataStore;
import com.epam.deltix.util.io.CachedLocalIP;
import com.epam.deltix.util.io.GUID;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.lang.Disposable;
import com.epam.deltix.util.lang.DisposableListener;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.text.SimpleStringCodec;
import com.epam.deltix.util.time.Periodicity;
import com.epam.deltix.util.time.TimeKeeper;
import com.epam.deltix.util.vsocket.VSDispatcher;
import net.jcip.annotations.GuardedBy;
import org.apache.commons.lang3.NotImplementedException;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElement;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Level;

/**
 *
 */
public abstract class TickStreamImpl extends ServerStreamImpl
    implements AbstractDataStore, FriendlyStream, DisposableListener
{
    public static final String  BACKUP_SUFFIX = ".n.bak";
    public static final String  NEW_PREFIX = "new.";
    public static final String  UNIQUE_FILE_SUFFIX = ".unique.dat";
    public static final String  VERSIONS_FILE_SUFFIX = ".versions.dat";
    public static String        VERSION = "4.5";

    enum StreamMetaFlushMode {
        ASYNC, // Stream file is updated asynchronously, in separate thread
        DISABLED, // Stream file sync is disabled (for example, due to update from external source).
        DELAY, // Stream will be marked as dirty without immediate metadata file write.
        PROHIBITED // Stream file sync is prohibited. Attempt to call "setDirty" means implementation error or incomplete implementation.
    }

    public static final String USE_DISRUPTOR_QUEUE_PROPERTY_NAME = "deltix.tickdb.useDisruptorQueue";

    private class Saver extends FrequencyLimiter {
        private final TickDBImpl            db;

        Saver (TickDBImpl db) {
            super (db.streamSaver);

            this.db = db;
        }

        @Override
        protected void                      onError (Throwable x) {
            TickDBImpl.LOGGER.log (
                Level.SEVERE,
                "Failed to save stream " + TickStreamImpl.this,
                x
            );
        }

        protected void                      run () throws Exception {
            saveChanges ();
        }

        @Override
        protected long                      getDelay () {
            return (db.getCommitDelay ());
        }
    }

    DXTickDB                                    db;
    protected  File                             file; // Deprecated in favor of "metadataLocation"

    protected  FileLocation                     metadataLocation; // Metadata file location, sync by this
    private    LockFile                         fileLock;
    
    private final CachingCodecMetaFactory       intpFactoryCache =
        new CachingCodecMetaFactory (InterpretingCodecMetaFactory.INSTANCE);

    private final CodecFactory                  intpCodecFactory =
        new CodecFactory (intpFactoryCache);
    
    private final CachingCodecMetaFactory       compFactoryCache =
        new CachingCodecMetaFactory (CompiledCodecMetaFactory.INSTANCE);

    private final CodecFactory                  compCodecFactory =
        new CodecFactory (compFactoryCache);

    @XmlElement (name = "version")
    protected         String                    version;

    @XmlElement (name = "key")
    private         String                      key;
    
    @GuardedBy ("this")
    @XmlElement (name = "name")
    private String                              name;

    @GuardedBy ("this")
    @XmlElement (name = "description")
    private String                              description;
    
    @GuardedBy ("this")
    @XmlElement (name = "polymorphic")
    private boolean                             polymorphic;
    
    @GuardedBy ("this")
    @XmlElement (name = "periodicity")
    private Periodicity                         periodicity = Periodicity.mkIrregular();

    @XmlElement (name = "scope")
    private StreamScope                         scope;

    @GuardedBy ("this")
    @XmlElement (name = "metaData")
    protected final RecordClassSet              md;

    protected FrequencyLimiter                  saver;
    
    @GuardedBy ("this")
    protected boolean                           isOpen = false;
    
    @GuardedBy ("this")
    protected boolean                           isReadOnly;

    @GuardedBy ("this")
    @XmlElement (name = "unique")
    private boolean                             unique;

    @GuardedBy ("this")
    @XmlElement (name = "replicaVersion")
    protected long                              replicaVersion = -1;

    @GuardedBy ("this")
    @XmlElement (name = "final")
    private boolean                             isFinal = false; // stream metadata is final

    @XmlElement (name = "duplicatesAllowed")
    private boolean                             duplicatesAllowed = true;

    @XmlElement (name = "location")
    protected String                            location;

    @XmlElement (name = "versioning")
    protected boolean                           versioning;

    UniqueMessageContainer                      accumulator;

    @GuardedBy ("openCursors")
    private final Set <TickCursor>              openCursors = new HashSet <> ();

    private volatile TickCursor []          snapshotOfOpenCursors = { };
    
    @GuardedBy ("this")
    private long                                mdVersion = System.currentTimeMillis ();

    private final Set<StreamLockImpl>           sharedLocks = new HashSet <StreamLockImpl> ();
    private StreamLockImpl                      exclusiveLock = null;


    @GuardedBy("this")
    @XmlElement(name = "owner")
    protected String                            owner;

    private volatile StreamVersionsContainer    versionsData;

    @GuardedBy ("this")
    protected StreamMetaFlushMode metaFlushMode = StreamMetaFlushMode.ASYNC;
    protected boolean isDirty = false; // If true then file must be saved.
    protected final EnumSet<TickStreamPropertiesEnum> changedProperties = EnumSet.noneOf(TickStreamPropertiesEnum.class);

    /**
     *  Used by JAXB
     */
    TickStreamImpl () { 
        key = null;
        name = null;
        description = null;
        scope = StreamScope.DURABLE;
        md = null;
    }

    TickStreamImpl (
        DXTickDB            db,
        String              key,
        StreamOptions       options
    )
    {
        this.db = db;
        this.key = key;
        
        name = options.name == null ? key : options.name;
        description = options.description == null ? "" : options.description;
        md = options.getMetaData ();
        polymorphic = options.isPolymorphic();
        scope = options.scope;
        duplicatesAllowed = options.duplicatesAllowed;
        unique = options.unique;

        periodicity = options.periodicity;
        location = options.location;

        owner = options.owner;
        
//        if (options.location != null)   //TMP
//            TickDBImpl.LOGGER.warning ("STREAM: " + key + "; LOCATION: " + options.location);

        setUnique(options.unique);
    }

    public final DXTickDB           getDB () {
        return (db);
    }

    public final synchronized RecordClassSet getMetaData () {
        return (md);
    }

    public final File getStreamFolder() {
        if (metadataLocation.isRemote()) {
            // TODO: Implement
            throw new NotImplementedException("Not implemented for remote streams yet");
        }

        return file.getParentFile();
    }
    
    public final String             getId () {
        return (key);
    }

    public final StreamScope        getScope () {
        return (scope);
    }
    
    static DXTickStream             read (File file) {
        try {
            return (DXTickStream) IOUtil.unmarshal(TickDBJAXBContext.createUnmarshaller(), file);
        } catch (Exception ex) {
            throw new com.epam.deltix.util.io.UncheckedIOException("Error reading file: " + file, ex);
        }
    }

    static DXTickStream             read (AbstractPath path) {
        try {
            return (DXTickStream) IOUtil.unmarshal(TickDBJAXBContext.createUnmarshaller(), path.openInput(0));
        } catch (Exception ex) {
            throw new com.epam.deltix.util.io.UncheckedIOException("Error reading file: " + path.getPathString(), ex);
        }
    }
    
    public final String                   getKey () {
        return (key);
    }

    void                                  setKey(String key) {
        this.key = key;
    }

    public final synchronized String      getName () {
        return (name);
    }
        
    public final synchronized String      getDescription () {
        return (description);
    }

    public final synchronized void        setDescription (String description) {
        assertFinal();
        if (!Util.xequals (description, this.description)) {
            this.description = description;
            firePropertyChanged(TickStreamProperties.DESCRIPTION);
            setDirty(TickStreamPropertiesEnum.DESCRIPTION);
        }
    }

    @Override
    public long                     getSizeOnDisk() {
        return 0;
    }

    public void                     setOwner(String owner) {
        assertFinal();

        if (!Util.xequals(owner, this.owner)) {
            this.owner = owner;
            firePropertyChanged(TickStreamProperties.OWNER);
            setDirty();
        }
    }

    public final synchronized void      setName (String name) {
        assertFinal();
        
        if (!Util.xequals (name, this.name)) {
            this.name = name;
            firePropertyChanged(TickStreamProperties.NAME);
            setDirty(TickStreamPropertiesEnum.NAME);
        }
    }

    public String                   getOwner() {
        return owner;
    }

    String                          getVersion() {
        return version;
    }

    public abstract IdentityKey[]   getComposition(IdentityKey ... ids);

    @Override
    public StreamOptions                getStreamOptions() {
        StreamOptions options = 
            new StreamOptions (scope, name, description, getDistributionFactor ());    
        
        options.setMetaData (polymorphic, new RecordClassSet (md));
        options.duplicatesAllowed = this.duplicatesAllowed;
        options.unique = this.unique;
        options.periodicity = new Periodicity(periodicity);
        options.location = this.location;
        options.owner = this.owner;
        options.version = String.valueOf(getFormatVersion());
        
        return options;
    }

    protected void                  onRename (File newFolder, String oldKey) {

        File file = getVersionsFile(oldKey);
        if (versioning && file.exists()) {
            File dest = getVersionsFile(key);

            Util.close(versionsData);
            versionsData = null;

            if (file.renameTo(dest)) {
                enableVersioning();
            } else {
                TickDBImpl.LOG.warn("Failed to rename " + file + " to " + dest);
            }
        }
    }

    protected TickDBImpl            getDBImpl () {
        return ((TickDBImpl) db);
    }

    public synchronized void        rename (String key) {
        if (db == null)
            throw new IllegalStateException ("Cannot rename an anonymous stream");

        if (metadataLocation.isRemote()) {
            // TODO: Implement
            throw new NotImplementedException("Not implemented for remote streams yet");
        }

        assertWritable();

        assertFinal();

        if (key == null)
            throw new IllegalArgumentException("Stream key cannot be null.");

        if (!Util.xequals (key, this.key)) {
            
            for (DXTickStream stream : db.listStreams()) {
                if (Util.xequals(key, stream.getKey()))
                    throw new IllegalArgumentException("Stream with key '" + key + "' already exists.");
            }

            String before = this.key;

            // try to close files before rename
            boolean wasOpen = isOpen();
            if (wasOpen)
                close();

            File folder = file.getParentFile();
            File newFolder = new File(folder.getParentFile(), SimpleStringCodec.DEFAULT_INSTANCE.encode(key));
            newFolder.mkdirs();

            File newFile = new File(newFolder,
                    SimpleStringCodec.DEFAULT_INSTANCE.encode(key) + TickDBImpl.STREAM_EXTENSION);

            // rename metadata file
            if (this.file.renameTo(newFile))
                this.file = newFile;

            // change metadata location
            this.metadataLocation = new FileLocation(file);

            // assign new key to make sure that metadata will be stored correctly
            this.key = key;

            // rename files
            onRename (newFolder, before);

            // save new metadata (in closed state)
            saveToFile();

            if (wasOpen)
                open(isReadOnly());

            getDBImpl ().streamRenamed(key, before);

            try {
                IOUtil.delete(folder);
            } catch (IOException e) {
                TickDBImpl.LOGGER.log(Level.WARNING, e.getMessage(), e);
            }

//            // store metadata
//            setDirty();

            getDBImpl().fireRenamed(this, before);
        }
    }

    public final synchronized RecordClassDescriptor    getFixedType () {
        return (polymorphic ? null : md.getTopType (0));
    }

    public final synchronized RecordClassDescriptor [] getPolymorphicDescriptors () {
        return (polymorphic ? md.getTopTypes () : null);
    }

    public final synchronized ClassDescriptor [] getAllDescriptors () {
        return (md.getClassDescriptors ());
    }

    public final synchronized RecordClassDescriptor [] getClassDescriptors () {
        return isFixedType() ? new RecordClassDescriptor[] {getFixedType()} : getPolymorphicDescriptors();
    }

    public final synchronized long        getTypeVersion () {
        return (mdVersion);
    }

    public final synchronized boolean     isFixedType () {
        return (!polymorphic);
    }

    public final synchronized boolean     isPolymorphic () {
        return (polymorphic);
    }

    public final synchronized boolean     isUnique() {
        return unique;
    }

    public final boolean                  isDuplicatesAllowed() {
        return duplicatesAllowed;
    }

    public final synchronized void        setUnique(boolean unique) {
        assertFinal();
        
        this.unique = unique;
        invalidateUniqueContainer();
    }

    @Override
    public synchronized RecordClassDescriptor[] getTypes() {
        return isFixedType() ? new RecordClassDescriptor[] {getFixedType()} : getPolymorphicDescriptors();
    }

    public final synchronized void        setFinal(boolean value) {
        this.isFinal = value;
    }

    public synchronized void        setPolymorphic (RecordClassDescriptor ... cds) {
        assertFinal();
        
        for (RecordClassDescriptor cd : cds)
            if (cd.isAbstract ())
                throw new IllegalArgumentException (
                    "Class " + cd.getName () + " is abstract."
                );
                
        onDelete ();
        
        polymorphic = true;

        mdVersion++;
        md.clear (); 
        md.addContentClasses (cds);
        
        onMetaDataUpdated();

        saveSchemaChange();
    }

    protected final synchronized void     setMetaData(boolean isPolymorphic, RecordClassSet rcd) {
        assertFinal();

        polymorphic = isPolymorphic;

        mdVersion++;
        md.set(rcd);
        
        onMetaDataUpdated();

        saveSchemaChange();
    }

    public synchronized void        setFixedType (RecordClassDescriptor cd) {
        assertFinal();
        
        if (cd.isAbstract ())
            throw new IllegalArgumentException (
                "Class " + cd.getName () + " is abstract."
            );
        
        onDelete ();
        polymorphic = false;

        mdVersion++;
        md.clear ();         
        md.addContentClasses (cd);
        
        onMetaDataUpdated();

        saveSchemaChange();
    }

    private void saveSchemaChange() {
        if (isRemoteMetadata()) {
            setDirty(TickStreamPropertiesEnum.SCHEMA);
        } else {
            saveToFile();
        }
    }

    protected void                      onMetaDataUpdated() {
        intpFactoryCache.clearCache ();
        compFactoryCache.clearCache ();

        invalidateUniqueContainer();

        firePropertyChanged(TickStreamProperties.SCHEMA);

        if (db != null)
            getDBImpl().invalidateQueryCache(this);
    }

    protected void                        invalidateUniqueContainer() {
        assert Thread.holdsLock(this);

        if (md.getNumTopTypes() == 0) {
            accumulator = null;
        } else {
            accumulator = unique ? new UniqueMessageContainer(
                    getCodecFactory(false),
                    isFixedType() ?
                            new RecordClassDescriptor[]{getFixedType()} :
                            getPolymorphicDescriptors()
            ) : null;
        }
    }
    
    public final void               init (TickDBImpl db, File file) {
        this.db = db;
        this.file = file;
        this.metadataLocation = new FileLocation(file);
    }

    public final void               init (TickDBImpl db, FileLocation metadataLocation) {
        this.db = db;
        this.file = metadataLocation.getFile();
        this.metadataLocation = metadataLocation;
        if (isRemoteMetadata()) {
            this.metaFlushMode = StreamMetaFlushMode.PROHIBITED;
        }
    }
    
    private static final File []      NO_FILES = { };
    
    File []                         listFiles() {
        return (NO_FILES);
    }

    /**
     * Marks stream metadata as changed.
     */
    final synchronized void         setDirty () {
        if (db != null && scope != StreamScope.RUNTIME && !isReadOnly) {
            markStreamForSave(null);
        }
    }

    final synchronized void         setDirty (boolean force) {
        if (db != null && scope != StreamScope.RUNTIME && !isReadOnly) {
            markStreamForSave(null);
            if (saver != null && force)
                saver.execute();
        }
    }

    final synchronized void         setDirty (TickStreamPropertiesEnum changedProperty) {
        if (db != null && scope != StreamScope.RUNTIME && !isReadOnly) {
            markStreamForSave(changedProperty);
        }
    }

    private void markStreamForSave(@Nullable TickStreamPropertiesEnum changedProperty) {
        switch (metaFlushMode) {
            case ASYNC:
                asyncSave();
                break;
            case DISABLED:
                // Do nothing
                break;
            case DELAY:
                isDirty = true;
                if (changedProperty != null)
                    changedProperties.add(changedProperty);
                break;
            case PROHIBITED:
                TickDBImpl.LOGGER.log(Level.WARNING, this + ": Attempt to call setDirty() in PROHIBITED state");
                //throw new IllegalStateException("Attempt to call setDirty() in PROHIBITED state");
                break;

            default:
                throw new IllegalArgumentException("Unknown state: " + metaFlushMode);
        }

    }

    private void asyncSave() {
        if (saver == null) {
            saver = new Saver((TickDBImpl) db);
        }

        saver.arm ();
    }

    public synchronized void                     saveToFile () {
        saveToFile(false);
    }

    synchronized void                     saveToFile (boolean holdsRemoteStreamLock) {
        if (scope == StreamScope.RUNTIME)
            return;

        if (metadataLocation.isLocal()) {
            AbstractPath path = metadataLocation.getPath();

            AbstractPath backupPath = path.getParentPath().append(path.getName() + BACKUP_SUFFIX);
            AbstractPath newFilePath = path.getParentPath().append(NEW_PREFIX + path.getName());
            try {
                twoStepSave(path, backupPath, newFilePath);
            } catch (JAXBException | IOException ex) {
                throw new com.epam.deltix.util.io.UncheckedIOException(ex);
            }
//            try {
//                File backup = new File(file.getPath() + BACKUP_SUFFIX);
//                backup.delete();
//                File newFile = new File(file.getParentFile(), NEW_PREFIX + file.getName());
//
//                if (file.exists()) {
//                    newFile.delete();
//                    IOUtil.marshall(TickDBJAXBContext.createMarshaller(), newFile, this);
//                    file.renameTo(backup);
//                    newFile.renameTo(file);
//                } else {
//                    IOUtil.marshall(TickDBJAXBContext.createMarshaller(), newFile, this);
//                    newFile.renameTo(file);
//                }
//            } catch (JAXBException ex) {
//                throw new com.epam.deltix.util.io.UncheckedIOException(ex);
//            } catch (IOException ex) {
//                throw new com.epam.deltix.util.io.UncheckedIOException(ex);
//            }
        } else {
            if (!holdsRemoteStreamLock) {
                throw new IllegalStateException("Attempt to write remote stream data without lock");
            }
            // Note: this operation should be performed with lock
            // TODO: Lock and update metadata
            AbstractPath path = metadataLocation.getPath();
            AbstractPath backupPath = path.getParentPath().append(path.getName() + BACKUP_SUFFIX);
            AbstractPath newFilePath = path.getParentPath().append(NEW_PREFIX + path.getName());
            try {
                path.setCacheMetadata(false); // Ensure that non-cached value is used
                twoStepSave(path, backupPath, newFilePath);
            } catch (JAXBException | IOException ex) {
                throw new com.epam.deltix.util.io.UncheckedIOException(ex);
            }
        }
    }

    /**
     * Deletes existing config at {@code backupPath} and saves configuration of this steam
     * into a file defined by {@code path} using {@code newFilePath} as temporary path.
     *
     * @param path configuration destination path
     * @param backupPath old backup file path
     * @param newFilePath temporary file path
     */
    private void twoStepSave(AbstractPath path, AbstractPath backupPath, AbstractPath newFilePath) throws IOException, JAXBException {
        backupPath.deleteIfExists();
        if (path.exists()) {
            newFilePath.deleteIfExists();
            saveToOutputStream(newFilePath.openOutput(0));
            path.moveTo(backupPath);
            newFilePath.moveTo(path);
        } else {
            saveToOutputStream(newFilePath.openOutput(0));
            newFilePath.moveTo(path);
        }
        //System.out.println("Written file length: " + (path.length() * 1.0 / 1024 / 1024));
    }

    private void saveToOutputStream(OutputStream out) throws IOException, JAXBException {
        OutputStream bout = createBufferedOutput(out);
        IOUtil.marshall(TickDBJAXBContext.createMarshaller(), bout, this);
    }

    protected OutputStream createBufferedOutput(OutputStream out) {
        assert Thread.holdsLock(this);
        return new BufferedOutputStream(out);
    }

    protected void                    readCache() throws IOException {
        if (isUnique () && accumulator != null)
            accumulator.read (getCacheFile ());
    }

    File                            getCacheFile() {
        if (metadataLocation.isRemote()) {
            // TODO: Implement
            throw new NotImplementedException("Not implemented for remote streams yet");
        }

        return new File(file.getParentFile(),
                        SimpleStringCodec.DEFAULT_INSTANCE.encode(key) + UNIQUE_FILE_SUFFIX);
    }

    File                            getVersionsFile() {
        return getVersionsFile(key);
    }

    File                            getVersionsFile(String streamKey) {
        if (metadataLocation.isRemote()) {
            // TODO: Implement
            throw new UnsupportedOperationException("Not implemented for remote streams yet");
        }

        return new File(file.getParentFile(),
                SimpleStringCodec.DEFAULT_INSTANCE.encode(streamKey) + VERSIONS_FILE_SUFFIX);
    }

    private synchronized void       saveChanges () throws IOException {
        if (!isOpen)
            return;

        saveToFile ();

        if (isUnique() && scope == StreamScope.DURABLE) {
            if (accumulator != null && accumulator.isDirty ())
                accumulator.write(getCacheFile());
        }
    }
    
    @Override
    void                            trimToSize () throws IOException {        
    }
    
    public synchronized void        close () {
        if (!isOpen)
            return;

        if (saver != null && saver.disarm ())
            saver.execute ();

        try {
            if (isUnique() && scope == StreamScope.DURABLE) {
                if (accumulator != null && accumulator.isDirty ())
                    accumulator.write(getCacheFile());
            }
        } catch (IOException e) {
            TickDBImpl.LOGGER.log(Level.WARNING, this + ": cannot save unique data.", e);
        }

        Util.close(versionsData);
        versionsData = null;

        Util.close(fileLock);

        try {
            if (fileLock != null)
                fileLock.delete();
        } catch (com.epam.deltix.util.io.UncheckedIOException ex) {
            TickDBImpl.LOG.warn("Failed to remove lock: %s").with(ex);
        }

        Util.close(versionsData);
        versionsData = null;

        isOpen = false;
    }

    protected void                  onDelete () {
        if (metadataLocation.isLocal()) {
            File vFile = getCacheFile();
            if (vFile.exists())
                IOUtil.deleteUnchecked(vFile);

            vFile = getVersionsFile();
            if (vFile.exists())
                IOUtil.deleteUnchecked(vFile);
        }
    }

    public synchronized void        delete () {
        if (metadataLocation.isRemote()) {
            // TODO: Implement
            throw new NotImplementedException("Not implemented for remote streams yet");
        }

        assertWritable ();

        close ();

        getDBImpl ().streamDeleted (key);
        onDelete ();
        
        if (scope != StreamScope.RUNTIME)
            IOUtil.deleteUnchecked (file.getParentFile());
    }

    public synchronized void  format () {
        assert !isOpen ();
        
        isOpen = true;
        isReadOnly = false;

        try {
            onOpen (false);
        } catch (IOException x) {
            throw new com.epam.deltix.util.io.UncheckedIOException(x);
        }

        setDirty ();
    }

    /**
     * Opens stream.
     * @param verify true, if stream wasn't closed successfully on previous start.
     * @throws IOException
     */
    protected void                  onOpen (boolean verify) throws IOException {
    }

    protected void                  lockStream(boolean readOnly) {
        // Remote streams not locked for read operations TODO: Is this right?
        if (getScope() != StreamScope.RUNTIME && metadataLocation.isLocal())
            fileLock = TickDBImpl.use(getStreamFolder(), readOnly);
    }

    public synchronized void        open (boolean readOnly) {
        assert !isOpen ();
        
        isOpen = true;
        isReadOnly = readOnly;

        lockStream(readOnly);

        try {
            onOpen (fileLock != null && fileLock.getState() == LockFile.State.TERMINATED);
        } catch (IOException x) {
            Util.close(fileLock);
            throw new com.epam.deltix.util.io.UncheckedIOException("Failed to open stream [" + key + "]", x);
        }

        invalidateUniqueContainer();

        try {
            readCache();
        } catch (IOException | NotImplementedException x) {
            TickDBImpl.LOGGER.log(Level.WARNING, this + ": cannot load cache.", x);
        }

        if (!VERSION.equals(version)) {
            version = VERSION; // update version
        }

        setDirty();
    }

//    @Override
//    protected void                  onSubscriptionListenerAdded(SubscriptionChangeListener lnr) {
//        TickCursorImpl[] cursors = getSnapshotOfOpenCursors();
//
//        for (int i = 0; i < cursors.length; i++)
//            if (!cursors[i].isClosed())
//                cursors[i].fireCurrentSubscription();
//    }

    /// locking support

    public synchronized void                     clearLocks(String id) {

        if (exclusiveLock != null && id.equals(exclusiveLock.getClientId())) {
            removeLock(exclusiveLock);
        }

        for (StreamLockImpl sharedLock : sharedLocks) {
            if (id.equals(sharedLock.getClientId()))
                removeLock(sharedLock);
        }

    }

    void                            assertExclusive(StreamLockImpl lock) {
        if (exclusiveLock != null && !exclusiveLock.equals(lock)) {
            throw new StreamLockedException("Stream '" + getKey() + "' is locked by " + exclusiveLock);
        }
        else if (!sharedLocks.isEmpty()) {
            if (sharedLocks.size() == 1 && sharedLocks.contains(lock))
                return;
            
            throw new StreamLockedException("Stream '" + getKey() + "' cannot be locked exclusively. Shared locks exists:" + Arrays.toString(sharedLocks.toArray()));
        }
    }

    void                            assertShared(StreamLockImpl lock) {
        if (exclusiveLock != null && exclusiveLock.equals(lock))
            return;
        
        if (exclusiveLock != null)
            throw new StreamLockedException("Stream '" + getKey() + "' is locked by " + exclusiveLock);

        if (!sharedLocks.contains(lock))
            throw new IllegalStateException("Stream '" + getKey() + "' " + lock + " is not applied.");
    }

    @Override
    public synchronized DBLock      verify(DBLock lock, LockType type) throws StreamLockedException {
        if (lock != null) {
            StreamLockImpl impl = new StreamLockImpl(this, (ServerLock)lock);

            if (type == LockType.READ)
                assertShared(impl);
            else
                assertExclusive(impl);

            return impl;
        } else {
            if (type == LockType.WRITE)
                assertExclusive(null);
            
            return null;
        }
    }

    synchronized void               addLock(StreamLockImpl lock) {
        if (lock.getType() == LockType.WRITE && exclusiveLock != null)
            throw new StreamLockedException("Stream '" + getKey() + "' is locked by " + exclusiveLock);

        if (lock.getType() == LockType.WRITE && !sharedLocks.isEmpty())
            throw new StreamLockedException("Stream '" + getKey() + "' cannot be locked exclusively. Shared locks exists:" + Arrays.toString(sharedLocks.toArray()));

        if (lock.getType() == LockType.READ)
            sharedLocks.add(lock);
        else
            exclusiveLock = lock;

        onLockAdded(lock);
    }

    @Override
    synchronized boolean            hasLock(StreamLockImpl lock) {
        if (exclusiveLock != null && exclusiveLock.equals(lock))
            return true;

        return sharedLocks.contains(lock);
    }

    @Override
    synchronized void               removeLock(StreamLockImpl lock) {

        if (lock.getType() == LockType.WRITE && lock.equals(exclusiveLock))
            exclusiveLock = null;
        else
            sharedLocks.remove(lock);

        onLockRemoved(lock);
    }

    @Override
    public synchronized DBLock          lock() {
        return lock(LockType.WRITE);
    }

    @Override
    public synchronized DBLock          lock(LockType type) {
        if (exclusiveLock != null)
            throw new StreamLockedException("Stream '" + getKey() + "' is locked by " + exclusiveLock);
        
        StreamLockImpl lock = new StreamLockImpl(this, type, new GUID().toStringWithPrefix(CachedLocalIP.getIP()));
        addLock(lock);

        return lock;
    }

    @Override
    public DBLock          tryLock(long timeout) throws StreamLockedException {
        return tryLock(LockType.WRITE, timeout);
    }

    @Override
    public DBLock          tryLock(LockType type, long timeout) throws StreamLockedException {
        if (timeout <= 0)
            return lock(type);

        StreamLockedException error = null;
        
        long timeLimit = TimeKeeper.currentTime + timeout;
        if (timeLimit < 0) // overflow check
            timeLimit = Long.MAX_VALUE;

        long period = Math.min(timeout, 100);
        try {
            while (TimeKeeper.currentTime < timeLimit) {
                try {
                    return lock(type);
                } catch (StreamLockedException e) {
                    error = e;
                }
                Thread.sleep(period);
            }
        } catch (InterruptedException e) {
            throw new UncheckedInterruptedException(e);
        }

        throw error;
    }

    void                            onLockAdded(StreamLockImpl lock) {
        TickDBImpl.LOGGER.fine("Adding " + lock);
        
        LockEventListener[] listeners = getEventListeners();
        for (int i = 0; i < listeners.length; i++)
            listeners[i].lockAdded(lock);

        EventMessageType eventType = lock.getType() == LockType.READ ?
                EventMessageType.READ_LOCK_ACQUIRED : EventMessageType.WRITE_LOCK_ACQUIRED;
        getDBImpl().log(createEventMessage(eventType, key));
        getDBImpl().registerLock(lock);
    }

    public static InstrumentMessage createRealTimeStartMessage(boolean raw, long timestamp) {
        InstrumentMessage message;
        if (raw) {
            message = new RawMessage();
            ((RawMessage)message).setBytes(new byte[0], 0, 0);
            ((RawMessage)message).type = Messages.REAL_TIME_START_MESSAGE_DESCRIPTOR;
        } else {
            message = new RealTimeStartMessage();
        }

        message.setSymbol("");
        message.setTimeStampMs(timestamp);

        return message;
    }

    private static EventMessage createEventMessage(EventMessageType type, String key) {
        EventMessage msg = new EventMessage();
        msg.setEventType(type);
        msg.setSymbol(key);
        return msg;
    }

    public StreamTruncatedMessage createStreamTruncatedMessage(long nanoTime, CharSequence instruments) {
        StreamTruncatedMessage msg = new StreamTruncatedMessage();
        msg.setNanoTime(nanoTime);
        msg.setInstruments(instruments);
        msg.setSymbol("");
        return msg;
    }


    void                            onLockRemoved(StreamLockImpl lock) {
        TickDBImpl.LOGGER.fine("Removing " + lock);

        LockEventListener[] listeners = getEventListeners();
        for (int i = 0; i < listeners.length; i++)
            listeners[i].lockRemoved(lock);

        EventMessageType eventType = lock.getType() == LockType.READ ?
                EventMessageType.READ_LOCK_RELEASED : EventMessageType.WRITE_LOCK_RELEASED;

        getDBImpl().log(createEventMessage(eventType, key));
        getDBImpl().unregisterLock(lock);
    }
    public final synchronized boolean   isOpen () {
        return (isOpen);
    }

    public final synchronized boolean   isReadOnly () {
        return (isReadOnly);
    }

    public final void                   assertClosed () {
        if (isOpen)
            throw new IllegalStateException (getId () + " is open");
    }

    public MetaDataChangeMessage createMetaDataChangeMessage(boolean converted, long timestamp) {
        MetaDataChangeMessage msg = new MetaDataChangeMessage();
        msg.setTimeStampMs(timestamp);
        msg.setConverted(converted);
        msg.setSymbol("");
        return msg;
    }

    final void                          assertOpen () {
        if (!isOpen)
            throw new UnknownStreamException(getId () + " is closed");
    }

    final void                   assertFinal () {
        if (isFinal)
            throw new IllegalStateException (getId () + " is final");
    }
    
    final void                   assertWritable () {
        assertOpen ();
        
        if (isReadOnly)
            throw new IllegalStateException (getId () + " is read-only");
    }
    
    public final TickLoader             createLoader () {
        return (createLoader (null));
    }
    
    public TickLoader                   createLoader (LoadingOptions options) {
        assertWritable ();

        TickLoaderImpl      loader = new TickLoaderImpl (this, options);
        
        return (loader);
    }

    private void                    snapOpenCursors () {
        snapshotOfOpenCursors =
            openCursors.toArray (new TickCursor [openCursors.size ()]);
    }

    TickCursor []               getSnapshotOfOpenCursors () {
        return snapshotOfOpenCursors;
    }

    @Override
    final void                      cursorClosed (TickCursor cursor) {
        //boolean changed = false;

        synchronized (openCursors) {
            if (openCursors.remove (cursor)) {
                //changed = true;
                snapOpenCursors ();
            }
        }

        allEntitiesRemoved(cursor);
        allTypesRemoved(cursor);
    }
    
    @Override
    final void                      cursorCreated (TickCursor cursor) {
        synchronized (openCursors) {
            if (openCursors.add (cursor))
                snapOpenCursors ();
        }
    }

    @Override
    public TickCursor               select(
            long                        time,
            SelectionOptions            options,
            String[]                    types,
            IdentityKey[]        entities)
    {
        assertOpen ();

        TickCursor  cursor;

/*
        //  Support for TickCursorImplFixed
        if (options != null && TickCursorImplFixed.isSupported(options)) {
            cursor = new TickCursorImplFixed(getDBImpl(), options, types, entities, this);
            cursor.reset(time);
            return cursor;
        }
*/
        if (options != null && !options.live && options.restrictStreamType && this instanceof PDStream) {
            cursor = new TickCursorImpl_PDStream(getDBImpl(), options);
        } else {
            // Fallback to common implementation
            cursor = new TickCursorImpl(getDBImpl(), options);
        }
        cursor.reset (time);

        // cursor by default subscribed to all types, so change if types != null
        if (types != null)
            cursor.setTypes(types);

        if (entities == null)
            cursor.subscribeToAllEntities();
        else
            cursor.addEntities(entities);

        // apply whole subscription
        cursor.addStream(this);

        return (cursor);
    }

    @Override
    public TickCursor select(long time, SelectionOptions options, String[] types, CharSequence[] symbols) {
        assertOpen ();

        TickCursor  cursor;

/*
        //  Support for TickCursorImplFixed
        if (options != null && TickCursorImplFixed.isSupported(options)) {
            cursor = new TickCursorImplFixed(getDBImpl(), options, types, entities, this);
            cursor.reset(time);
            return cursor;
        }
*/
        if (options != null && !options.live && options.restrictStreamType && this instanceof PDStream) {
            cursor = new TickCursorImpl_PDStream(getDBImpl(), options);
        } else {
            // Fallback to common implementation
            cursor = new TickCursorImpl(getDBImpl(), options);
        }
        cursor.reset (time);

        // cursor by default subscribed to all types, so change if types != null
        if (types != null)
            cursor.setTypes(types);

        if (symbols == null)
            cursor.subscribeToAllEntities();
        else
            cursor.addSymbols(symbols);

        // apply whole subscription
        cursor.addStream(this);

        return (cursor);
    }

    @Override
    public TickCursor select(long time, SelectionOptions options, String[] types) {
        assertOpen ();

        TickCursor  cursor;

/*
        //  Support for TickCursorImplFixed
        if (options != null && TickCursorImplFixed.isSupported(options)) {
            cursor = new TickCursorImplFixed(getDBImpl(), options, types, entities, this);
            cursor.reset(time);
            return cursor;
        }
*/
        if (options != null && !options.live && options.restrictStreamType && this instanceof PDStream) {
            cursor = new TickCursorImpl_PDStream(getDBImpl(), options);
        } else {
            // Fallback to common implementation
            cursor = new TickCursorImpl(getDBImpl(), options);
        }
        cursor.reset (time);

        // cursor by default subscribed to all types, so change if types != null
        if (types != null)
            cursor.setTypes(types);

        cursor.subscribeToAllEntities();

        // apply whole subscription
        cursor.addStream(this);

        return (cursor);
    }

    @Override
    public TickCursor select(long time, SelectionOptions options) {
        assertOpen ();

        TickCursor  cursor;

/*
        //  Support for TickCursorImplFixed
        if (options != null && TickCursorImplFixed.isSupported(options)) {
            cursor = new TickCursorImplFixed(getDBImpl(), options, types, entities, this);
            cursor.reset(time);
            return cursor;
        }
*/
        if (options != null && !options.live && options.restrictStreamType && this instanceof PDStream) {
            cursor = new TickCursorImpl_PDStream(getDBImpl(), options);
        } else {
            // Fallback to common implementation
            cursor = new TickCursorImpl(getDBImpl(), options);
        }
        cursor.reset (time);

        cursor.subscribeToAllEntities();

        // apply whole subscription
        cursor.addStream(this);

        return (cursor);
    }

    StreamVersionsReader            createVersionsReader(long timestamp, SelectionOptions options) {
        if (versionsData != null)
            return versionsData.getReader(timestamp, options);

        return null;
    }

    public QuickExecutor            getQuickExecutor() {
        return getDBImpl().getQuickExecutor();
    }

    @Override
    public final TickCursor         createCursor (
        SelectionOptions                        options
    )
    {
        assertOpen ();

        if (options != null && options.restrictStreamType && this instanceof PDStream) {
            return new TickCursorImpl_PDStream(getDBImpl(), options, this);
        } else {
            // Fallback to common implementation
            return new TickCursorImpl(getDBImpl(), options, this);
        }
    }

    private static final IdentityKey []  NO_INSTRUMENTS = { };

    @Override
    public void renameInstruments(IdentityKey[] from, IdentityKey[] to) {
        //no instruments
    }

    public long []                  getTimeRange (IdentityKey ... entities) {
        return (null);
    }

    public boolean                  hasWriter (IdentityKey id) {
        return false;
    }

    protected CodecFactory            getCodecFactory (boolean preferInterpreted) {
        return (
            CodecFactory.useInterpretedCodecs (preferInterpreted) ?
                intpCodecFactory :
                compCodecFactory
        );
    }
        
    public abstract void                 truncate(long time, IdentityKey ... ids);

    public abstract void                 clear(IdentityKey... ids);

//    public final void           addFilterChangeListener (FilterChangeListener lnr) {
//        synchronized (filterChangeListeners) {
//            filterChangeListeners.add (lnr);
//        }
//    }
//
//    public final void         removeFilterChangeListener (FilterChangeListener lnr) {
//        synchronized (filterChangeListeners) {
//            filterChangeListeners.remove (lnr);
//        }
//    }


    public final synchronized Periodicity getPeriodicity() {
        return periodicity;
    }

//    @Override
//    public final synchronized void setPeriodicity(Interval interval) {
//        assertFinal();
//
//        if (!Util.xequals(interval, this.periodicity.getInterval())) {
//            this.periodicity = Periodicity.parse(interval);
//            firePropertyChanged(TickStreamProperties.PERIODICITY);
//            setDirty();
//        }
//    }

    @Override
    public synchronized void setPeriodicity(Periodicity p) {
        assertFinal();

        // make copy
        Periodicity local = p == null ? Periodicity.mkIrregular() : new Periodicity(p);

        if (!Util.xequals(local, this.periodicity)) {
            this.periodicity = local;
            firePropertyChanged(TickStreamProperties.PERIODICITY);
            setDirty(TickStreamPropertiesEnum.PERIODICITY);
        }
    }

    @Override
    public String       toString () {
        return String.format("%s STREAM [key: %s]", scope, key);
    }

    abstract MessageChannel<InstrumentMessage> createChannel (
        InstrumentMessage                               msg,
        LoadingOptions                                  options
    );

    protected final boolean         isUnique (InstrumentMessage msg) {
        if (accumulator == null || duplicatesAllowed)
            return true;

        return !accumulator.contains(msg);
    }

    public final boolean accumulateIfRequired (InstrumentMessage msg) {
        if (accumulator == null)
            return true;

        if (accumulator.add(msg, !duplicatesAllowed))
            return true;

        return false;
    }

    @Override
    public void             disposed(Disposable resource) {
        if (resource instanceof VSDispatcher) {
            String id = ((VSDispatcher)resource).getClientId();
            
            synchronized (this) {
                ArrayList<StreamLockImpl> list = new ArrayList<StreamLockImpl>();
                
                if (exclusiveLock != null && id.equals(exclusiveLock.getClientId()))
                   list.add(exclusiveLock);
                
                for (StreamLockImpl lock : sharedLocks) {
                    if (lock != null && id.equals(lock.getClientId()))
                        list.add(lock);
                }

                if (list.size() > 0) {
                    TickDBImpl.LOGGER.log (Level.FINE, this + ": releasing locks from client " + id);

                    for (StreamLockImpl lock : list)
                        lock.release();
                    list.clear();
                }
            }
        }
    }

    @Override
    public int              getFormatVersion() {
        return 4;
    }

    @Override
    public long             getReplicaVersion() {
        return replicaVersion;
    }

    @Override
    public void             setReplicaVersion(long version) {
        if (replicaVersion != version) {
            replicaVersion = version;
            setDirty();
        }
    }

    void             onStreamTruncated(long nstime, IdentityKey ... ids) {
        if (versionsData != null)
            versionsData.add(createStreamTruncatedMessage(nstime, printInstruments(ids)));
    }

    void             onSchemaChanged(boolean converted, long timestamp) {
        if (versionsData != null)
            versionsData.add(createMetaDataChangeMessage(converted, timestamp));
    }

    void             onSchemaChanged(SchemaChangeMessage schemaChangeMessage) {
        if (versionsData != null) {
            versionsData.add(schemaChangeMessage);
        }
    }

    void             onStreamTruncated(long nstime, IdentityKey id) {
        if (versionsData != null)
            versionsData.add(createStreamTruncatedMessage(nstime, id.getSymbol()));
    }

    static CharSequence                    printInstruments(IdentityKey[] identities) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < identities.length; i++) {
            IdentityKey id = identities[i];
            sb.append(i > 0 ? ";" : "");
            sb.append(id.getSymbol());
        }

        return sb;
    }

    @Override
    public synchronized boolean          enableVersioning() {
        if (versionsData == null) {
            try {
                File vf = getVersionsFile();

                if (!isReadOnly || vf.exists()) {
                    versionsData = new StreamVersionsContainer(this, getCodecFactory(false),
                        new Class[] {StreamTruncatedMessage.class, MetaDataChangeMessage.class, SchemaChangeMessage.class },
                        new RecordClassDescriptor[] {
                                Messages.STREAM_TRUNCATED_MESSAGE_DESCRIPTOR,
                                Messages.META_DATA_CHANGE_MESSAGE_DESCRIPTOR,
                                StreamOptions.getSchemaChangeMessageDescriptor()
                        } );
                }

            } catch (IOException e) {
                throw new com.epam.deltix.util.io.UncheckedIOException(e);
            }
        }

        if (!versioning) {
            versioning = true;
            setDirty();
        }

        return (versioning);
    }

    @Override
    public long                     getDataVersion() {
        return versionsData != null ? versionsData.getVersion() : -1;
    }

    /**
     * @return true if metadata for this is stored on remote FS
     */
    boolean isRemoteMetadata() {
        return metadataLocation.isRemote();
    }

    @Override
    public MessageSource<InstrumentMessage> createConsumer(ChannelPreferences options) {
        SelectionOptions opts;

        if (options instanceof SelectionOptions) {
            opts = (SelectionOptions) options;
        } else {
            opts = new SelectionOptions();
            opts.raw = options.raw;
            opts.typeLoader = options.typeLoader;
            opts.channelPerformance = options.channelPerformance;
        }

        return createCursor(opts);
    }

    @Override
    public MessageChannel<InstrumentMessage> createPublisher(ChannelPreferences options) {
        LoadingOptions opts;

        if (options instanceof LoadingOptions) {
            opts = (LoadingOptions) options;
        } else {
            opts = new LoadingOptions();
            opts.raw = options.raw;
            opts.typeLoader = options.typeLoader;
            opts.channelPerformance = options.channelPerformance;
        }

        return createLoader(opts);
    }
}