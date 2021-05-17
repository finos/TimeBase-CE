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

import com.epam.deltix.data.stream.ChannelPreferences;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.stream.MessageFileHeader;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.DBLock;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockEventListener;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockType;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.StreamLockedException;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.TransformationTask;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.stream.Protocol;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.TimeStamp;
import com.epam.deltix.timebase.messages.service.EventMessageType;
import com.epam.deltix.util.concurrent.UncheckedInterruptedException;
import com.epam.deltix.util.io.*;
import com.epam.deltix.util.lang.Disposable;
import com.epam.deltix.util.lang.DisposableListener;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.text.SimpleStringCodec;
import com.epam.deltix.timebase.messages.service.EventMessage;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.JAXBException;

import com.epam.deltix.util.time.Periodicity;
import com.epam.deltix.util.time.TimeKeeper;
import com.epam.deltix.util.vsocket.VSDispatcher;
import net.jcip.annotations.GuardedBy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

@XmlRootElement(name = "fileStream")
public class FileStreamImpl extends ServerStreamImpl
    implements AbstractDataStore, FriendlyStream, SingleChannelStream, DisposableListener
{
    private final Set<StreamLockImpl>           locks = new HashSet<StreamLockImpl>();

    TickDBImpl                                  db;    
    protected  File                             file;

    @GuardedBy ("this")
    private boolean                             isOpen = false;
    
    @GuardedBy ("this")
    private volatile boolean                    isDirty;
    
    @XmlElement(name = "key")
    private         String                      key;

    @GuardedBy("this")
    @XmlElement (name = "name")
    private String                              name;

    @GuardedBy ("this")
    @XmlElement (name = "description")
    private String                              description;

    @GuardedBy("this")
    @XmlElement(name = "owner")
    protected String                            owner;

    @GuardedBy ("this")
    @XmlElement (name = "periodicity")
    private Periodicity                         periodicity = Periodicity.mkIrregular();

    @GuardedBy ("this")
    @XmlElement (name = "dataFile")
    private String                              dataFile;

    @GuardedBy ("this")
    private long                                mdVersion = System.currentTimeMillis ();

    @XmlElement (name = "range")
    @GuardedBy ("this")
    private volatile long[]                     range;
        
    @XmlElement (name = "fileLength")
    private long                                fileLength;

    protected FileStreamImpl() { } // for jaxb

    public FileStreamImpl(String dataFile, String name) {
        this.dataFile = dataFile;
        this.key = this.name = name;
        
        this.isDirty = true;
    }

    public DXTickDB getDB () {
        return (db);
    }

    public int getDistributionFactor() {
        return 1;
    }

    @Override
    public void setTargetNumFiles(int value) {
    }

    public void setName(String name) {
        if (!StringUtils.equals(this.name, name)) {
            this.name = name;
            this.isDirty = true;
        }
    }

    @Override
    public void addInstrument(IdentityKey id) {
        throw new UnsupportedOperationException();
    }

    public File     getStreamFolder() {
        return file.getParentFile();
    }

    public String getName() {
        return name;
    }

    public void setDescription(String description) {
        if (!StringUtils.equals(this.description, description)) {
            this.description = description;
            this.isDirty = true;
        }
    }

    public String getDescription() {
        return description;
    }

    public void setOwner(String owner) {
        if (!StringUtils.equals(this.owner, owner)) {
            this.owner = owner;
            this.isDirty = true;
        }
    }

    @Override
    public long getSizeOnDisk() {
        return 0;
    }

    public String getOwner() {
        return owner;
    }

    @Override
    public StreamOptions                getStreamOptions() {
        StreamOptions options = 
            new StreamOptions (StreamScope.EXTERNAL_FILE, name, description, 1);
        
        options.setPolymorphic (getPolymorphicDescriptors ());
        options.periodicity = this.periodicity;
        options.owner = this.owner;
        return options;
    }

    public void rename(String key) {
        if (key == null)
            throw new IllegalArgumentException("Stream key cannot be null.");

        if (!Util.xequals (key, this.key)) {
            for (DXTickStream stream : db.listStreams()) {
                if (Util.xequals(key, stream.getKey()))
                    throw new IllegalArgumentException("Stream with key '" + key + "' already exists.");
            }

            File folder = file.getParentFile();
            File newFolder = new File(folder.getParentFile(), SimpleStringCodec.DEFAULT_INSTANCE.encode(key));
            newFolder.mkdirs();

            db.streamRenamed(key, this.key);
            this.key = key;

            File newFile = new File(newFolder,
                    SimpleStringCodec.DEFAULT_INSTANCE.encode(key) + TickDBImpl.STREAM_EXTENSION);

            if (this.file.renameTo(newFile))
                this.file = newFile;

            try {
                IOUtil.delete(folder);
            } catch (IOException e) {
                TickDBImpl.LOGGER.log(Level.WARNING, e.getMessage(), e);
            }

            this.isDirty = true;
            saveChanges();
        }
    }

    public String getId() {
        return getKey();
    }

    public String       getKey() {
        return key;
    }

    public long         getTypeVersion() {
        return mdVersion;
    }

    public StreamScope  getScope() {
        return StreamScope.EXTERNAL_FILE;
    }

    public void setPolymorphic(RecordClassDescriptor... cds) {
         throw new UnsupportedOperationException();
    }

    public void setFixedType(RecordClassDescriptor cd) {
        throw new UnsupportedOperationException();
    }

    public boolean isPolymorphic() {
        return true;
    }

    public boolean isFixedType() {
        return false;
    }       

    public IdentityKey[] listEntities() {
        return new IdentityKey[0];
    }

    @Override
    public void renameInstruments(IdentityKey[] from, IdentityKey[] to) {
        throw new UnsupportedOperationException();
    }

    public synchronized long[] getTimeRange(IdentityKey... entities) {
        File data = new File(dataFile);

        if (data.exists()) {
            if (fileLength != data.length())
                range = null;

            if (range == null)
                cacheTimeRange();
        }

        return range;
    }

    @Override
    public TimeInterval[] listTimeRange(IdentityKey... entities) {
        TimeRange[] ranges = new TimeRange[entities.length];

        for (int i = 0; i < entities.length; i++) {
            long[] time = getTimeRange(entities[i]);
            ranges[i] = new TimeRange(time != null ? time[0] : Long.MAX_VALUE, time != null ? time[1] : Long.MIN_VALUE);
        }

        return ranges;
    }

    private void                cacheTimeRange() {
        TickCursor cursor = null;
        try {
            cursor = select(Long.MIN_VALUE, new SelectionOptions(true, false));
            range = new long[] {0, Protocol.MAX_TIME};
            if (cursor.next())
                range[0] = cursor.getMessage().getTimeStampMs();
            cursor.close();
            cursor = null;
        } finally {
            Util.close(cursor);
        }
    }

    void                        setRange(long[] times) {
        long[] r = range;
        if (r != null && r[0] == times[0] && r[1] == times[1])
            return;

        range = times;
        fileLength = new File(dataFile).length();
        isDirty = true;
    }

    public RecordClassDescriptor    getFixedType() {
        return null;
    }

    public RecordClassDescriptor[]  getPolymorphicDescriptors() {
        try {
            return MessageFileHeader.migrate(Protocol.readHeader(new File(dataFile))).getTypes();
        } catch (Throwable e) {
            TickDBImpl.LOGGER.log(Level.SEVERE, e.getMessage(), e);
            return new RecordClassDescriptor[0];
        }
    }

    public ClassDescriptor[]        getAllDescriptors() {
        return getPolymorphicDescriptors();
    }

    public RecordClassSet           getMetaData() {
        RecordClassSet set = new RecordClassSet();
        set.addContentClasses(getPolymorphicDescriptors());
        
        return set;
    }

    @Override
    public boolean                  hasWriter(IdentityKey id) {
        return false;
    }

    private void                    saveChanges2 () {
        try {
            File backup = new File (file.getPath () + ".n.bak");

            // Ignore return values - just try.
            backup.delete ();
            file.renameTo (backup);

            TickDBJAXBContext.createMarshaller ().marshal (this, file);
        } catch (JAXBException ex) {
            throw new com.epam.deltix.util.io.UncheckedIOException(ex);
        }
    }

    public synchronized void        saveChanges () {
        assert isOpen ();

        if (!isDirty)
            return;

        saveChanges2 ();

        isDirty = false;
    }

    public void                     delete() {
        close ();
        db.streamDeleted (key);
        IOUtil.deleteUnchecked (file.getParentFile());
    }
    
    @Override
    public TickCursor               createCursor(SelectionOptions options) {
        return new TickCursorImpl (db, options, this);
    }

    @Override
    public TickCursor               select(
            long                        time,
            SelectionOptions            options,
            String[]                    types,
            IdentityKey[]        entities)
    {
        TickCursor  cursor = new TickCursorImpl (db, options);
        cursor.reset (time);

        if (types == null)
            cursor.subscribeToAllTypes();
        else
            cursor.setTypes(types);

        if (entities == null)
            cursor.subscribeToAllEntities();
        else
            cursor.addEntities(entities, 0, entities.length);

        // we should add stream at the end when reading file stream
        cursor.addStream(this);

        return (cursor);
    }

    @Override
    public TickCursor               select(
            long                        time,
            SelectionOptions            options,
            String[]                    types,
            CharSequence[]              symbols)
    {
        TickCursor  cursor = new TickCursorImpl (db, options);
        cursor.reset (time);

        if (types == null)
            cursor.subscribeToAllTypes();
        else
            cursor.setTypes(types);

        if (symbols == null)
            cursor.subscribeToAllEntities();
        else
            cursor.addSymbols(symbols);

        // we should add stream at the end when reading file stream
        cursor.addStream(this);

        return (cursor);
    }

    @Override
    public TickCursor select(long time, SelectionOptions options, String[] types) {
        TickCursor  cursor = new TickCursorImpl (db, options);
        cursor.reset (time);

        if (types == null)
            cursor.subscribeToAllTypes();
        else
            cursor.setTypes(types);

        cursor.subscribeToAllEntities();

        // we should add stream at the end when reading file stream
        cursor.addStream(this);

        return (cursor);
    }

    @Override
    public TickCursor select(long time, SelectionOptions options) {
        TickCursor  cursor = new TickCursorImpl (db, options);
        cursor.reset (time);

        cursor.subscribeToAllTypes();

        cursor.subscribeToAllEntities();

        // we should add stream at the end when reading file stream
        cursor.addStream(this);

        return (cursor);
    }

    @Override
    public MessageReaderSource      createSource (
        long                            time,
        SelectionOptions                options,
        QuickMessageFilter              filter
    )
    {
        return (new MessageReaderSource (this, time, new File (dataFile), options));
    }

    @Override
    public InstrumentMessageSource createSource(long time, SelectionOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InstrumentMessageSource createSource(long time, SelectionOptions options, IdentityKey[] identities, String[] types) {
        throw new UnsupportedOperationException();
    }

    public void                     format() {
        throw new UnsupportedOperationException();
    }

    public void                     purge(long time) {
         throw new UnsupportedOperationException();
    }

    public void                     truncate(long time, IdentityKey... ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(TimeStamp from, TimeStamp to, IdentityKey... ids) {
        throw new UnsupportedOperationException();
    }

    public void                     clear(IdentityKey... ids) {
        throw new UnsupportedOperationException();
    }

    public void                     execute(TransformationTask task) {
        throw new UnsupportedOperationException();
    }

    public BackgroundProcessInfo    getBackgroundProcess() {
        return null;
    }

    public void                     abortBackgroundProcess() {
    }

    public Periodicity              getPeriodicity() {
        try {
            Periodicity p = Periodicity.parse(Protocol.readHeader(new File(dataFile)).periodicity);
            return p.getInterval() != null ? p : periodicity;
        } catch (IOException e) {
            return periodicity;
        }
    }

    @Override
    public void                     setPeriodicity(Periodicity value) {
        if (periodicity == null)
            periodicity = Periodicity.mkIrregular();

        periodicity = value;
    }

    public void init(TickDBImpl tickDB, File file) {
        this.db = tickDB;
        this.file = file;
    }

    public void                     open(boolean readOnly) {
        assert !isOpen ();

        isOpen = true;        
    }

    public synchronized void        close () {
        if (!isOpen)
          return;

        if (isDirty)
          saveChanges ();

        isOpen = false;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public boolean isReadOnly() {
        return true;
    }

    @Override
    synchronized void warmUp() {
        cacheTimeRange();
    }

    public TickLoader createLoader() {
        throw new UnsupportedOperationException();
    }

    public TickLoader createLoader(LoadingOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized DBLock          lock() {
        return lock(LockType.WRITE);
    }

    @Override
    public synchronized DBLock          lock(LockType type) {
        if (type == LockType.WRITE)
            throw new UnsupportedOperationException();

        StreamLockImpl lock = new StreamLockImpl(this, type, new GUID().toStringWithPrefix(CachedLocalIP.getIP()));
        addLock(lock);

        return lock;
    }

    @Override
    public DBLock                       tryLock(long timeout) throws StreamLockedException {
        return tryLock(LockType.WRITE, timeout);
    }

    @Override
    public DBLock                       tryLock(LockType type, long timeout) throws StreamLockedException {
        if (type == LockType.WRITE)
            throw new UnsupportedOperationException();
        
        long timeLimit = TimeKeeper.currentTime + timeout;

        if (timeLimit < 0) // overflow check
            timeLimit = Long.MAX_VALUE;

        StreamLockedException error = null;
        DBLock lock;
        long period = Math.min(timeout, 100);
        try {
            while (TimeKeeper.currentTime < timeLimit) {

                try {
                    lock = lock(type);
                    return lock;
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

    @Override
    synchronized boolean            hasLock(StreamLockImpl lock) {
        return locks.contains(lock);
    }

    synchronized void               addLock(StreamLockImpl lock) {
        if (lock.getType() == LockType.READ)
            locks.add(lock);        

        onLockAdded(lock);
    }

    void                            onLockAdded(StreamLockImpl lock) {
        LockEventListener[] listeners = getEventListeners();
        for (int i = 0; i < listeners.length; i++)
            listeners[i].lockAdded(lock);

        EventMessageType eventType = lock.getType() == LockType.READ ?
                EventMessageType.READ_LOCK_ACQUIRED : EventMessageType.WRITE_LOCK_ACQUIRED;
        EventMessage eventMessage = new EventMessage();
        eventMessage.setEventType(eventType);
        eventMessage.setSymbol(key);
        db.log(eventMessage);   //new EventMessage(eventType, key));
    }

    void                            onLockRemoved(StreamLockImpl lock) {
        LockEventListener[] listeners = getEventListeners();
        for (int i = 0; i < listeners.length; i++)
            listeners[i].lockRemoved(lock);

        EventMessageType type = lock.getType() == LockType.READ ?
                EventMessageType.READ_LOCK_RELEASED : EventMessageType.WRITE_LOCK_RELEASED;
        EventMessage eventMessage = new EventMessage();
        eventMessage.setEventType(type);
        eventMessage.setSymbol(key);
        db.log(eventMessage);   //new EventMessage(type, key));
    }

    @Override
    public DBLock               verify(DBLock lock, LockType type) throws StreamLockedException {
        return lock;
    }

    @Override
    void                        removeLock(StreamLockImpl lock) {
        if (locks.remove(lock))
            onLockRemoved(lock);
    }

    @Override
    public void             disposed(Disposable resource) {
        if (resource instanceof VSDispatcher) {
            String id = ((VSDispatcher)resource).getClientId();

            synchronized (this) {
                ArrayList<StreamLockImpl> list = new ArrayList<StreamLockImpl>();

                for (StreamLockImpl lock : locks)
                    if (lock != null && id.equals(lock.getClientId()))
                        list.add(lock);

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
    public IdentityKey[]     getComposition(IdentityKey... ids) {
        return ids;
    }

    @Override
    public void         setHighAvailability(boolean value) {
    }

    @Override
    public boolean      getHighAvailability() {
        return false;
    }

    @Override
    public boolean      enableVersioning() {
        return true;
    }

    @Override
    public long         getDataVersion() {
        return -1;
    }

    @Override
    public int getFormatVersion() {
        return 4;
    }

    @Override
    public long getReplicaVersion() {
        return -1;
    }

    @Override
    public void setReplicaVersion(long version) {

    }

    @Override
    void cursorCreated(TickCursor cur) {
    }

    @Override
    void cursorClosed(TickCursor cursor) {
    }

    @Override
    public RecordClassDescriptor[]          getTypes() {
        return getPolymorphicDescriptors();
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
        throw new UnsupportedOperationException();
    }
}
