package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.data.stream.DXChannel;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.dtb.fs.FSLocator;
import com.epam.deltix.qsrv.dtb.fs.azure2.Azure2FS;
import com.epam.deltix.qsrv.dtb.fs.hdfs.DistributedFS;
import com.epam.deltix.qsrv.dtb.fs.lock.atomicfs.AtomicFsLockManager;
import com.epam.deltix.qsrv.dtb.fs.lock.atomicfs.FsLock;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractFileSystem;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.qsrv.dtb.fs.pub.FSFactory;
import com.epam.deltix.qsrv.dtb.store.impl.PDSFactory;
import com.epam.deltix.qsrv.dtb.store.pub.PersistentDataStore;
import com.epam.deltix.qsrv.hf.pub.ChannelQualityOfService;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.impl.mon.TBMonitorImpl;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.qcache.PQCache;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.CompilerUtil;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.ParamSignature;
import com.epam.deltix.qsrv.hf.tickdb.pub.BufferOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.DataCacheOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.FSOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamScope;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamStateListener;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamStateNotifier;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickLoader;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeConstants;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockType;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.PropertyMonitor;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBMonitor;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.Parameter;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.PreparedQuery;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;
import com.epam.deltix.qsrv.hf.tickdb.tool.FolderSpreader;
import com.epam.deltix.qsrv.hf.tickdb.tool.TDBUpgrade;
import com.epam.deltix.ramdisk.GlobalStats;
import com.epam.deltix.ramdisk.RAMDisk;
import com.epam.deltix.timebase.messages.TimeStampedMessage;
import com.epam.deltix.timebase.messages.service.EventMessage;
import com.epam.deltix.timebase.messages.service.EventMessageType;
import com.epam.deltix.util.ContextContainer;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.concurrent.ThrottlingExecutor;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.lang.*;
import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.util.parsers.Element;
import com.epam.deltix.util.runtime.Shutdown;
import com.epam.deltix.util.text.SimpleStringCodec;
import com.epam.deltix.util.time.Periodicity;
import net.jcip.annotations.GuardedBy;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.JAXBException;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.epam.deltix.qsrv.hf.tickdb.impl.TickDBFolderManager.FOLDER_HAS_STREAM_FILES;
import static com.epam.deltix.qsrv.hf.tickdb.impl.TickDBFolderManager.getFolderState;

/**
 *
 */
public class TickDBImpl
    extends TBMonitorImpl
    implements DXTickDB, TBMonitor, StreamStateNotifier, StreamStateListener, PQExecutor
{
    // Verifying, that we are under JDK, not JRE.
    static {
        JavaVerifier.verify();
    }

    static boolean ASSERTIONS_ENABLED = Assertions.ENABLED;

    // If enabled, TimeBase will scan folders on remote FS for streams.
    private static final boolean REMOTE_STREAMS = Boolean.valueOf(System.getProperty("TimeBase.remoteStreams.enable", "false"));

    // This prefix can be used for testing.
    private static final String REMOTE_STREAM_PATH_PREFIX = System.getProperty("TimeBase.remoteStreams.pathPrefix", "");

    // Chunked cache quota (in MB)
    private static final Integer CHUNKED_CACHE_DFS_SIZE_MB = Integer.getInteger("TimeBase.fileSystem.chunkedCache.DFSCacheSizeMb", null);
    // Enables chunked cache for local FS
    private static final boolean ENABLE_CHUNKED_CACHE_FOR_LOCAL = Boolean.valueOf(System.getProperty("TimeBase.fileSystem.chunkedCache.enableForLocal", "true"));
    // Enables chunked cache for DFS (like Azure)
    private static final boolean ENABLE_CHUNKED_CACHE_FOR_DFS = Boolean.valueOf(System.getProperty("TimeBase.fileSystem.chunkedCache.enableForDFS", CHUNKED_CACHE_DFS_SIZE_MB != null ? "true" : "false"));

    private static final boolean ENABLE_RAMDISK_CACHE = Boolean.valueOf(System.getProperty("TimeBase.fileSystem.cache.enableRamDisk", "true"));
    private static final int MAX_RAM_DISK_SIZE = 128 << 20; // 128MB max

    public static final String                  VERSION_PROPERTY = "TimeBase.version";
    public static final String                  SAFE_MODE_PROPERTY = "TimeBase.safeMode";
    public static final String                  OLD_DATA_FORMAT_VERSION = "4.3";

    public static final String                  STREAM_EXTENSION = ".uhfq.xml";
    public static final String                  DATA_EXTENSION = ".dat";
    public static final String                  MD_FILE_NAME = "md.xml";
    public static final String                  TIMECACHE_FILE_NAME = "index.cache.dat";
    public static final String                  LOCK_FILE_NAME = "tb.lock";
    public static final String                  CATALOG_NAME = "dbcat.txt";

    public static final String                  CONFIG_FILE = "config.properties";
    public static final String                  SYMBOLS_FILE = "symbols.txt";
    public static final String                  INDEX_FILE = "index.dat";

    public static final String                  EVENTS_STREAM_NAME = "events#";

    @Deprecated
    static final Logger                         LOGGER = //TODO: Replace with GFLog
        Logger.getLogger ("deltix.tickdb");

    static final Log LOG = LogFactory.getLog("deltix.tickdb");

    final RAMDisk                               ramdisk;
    private final File []                       dbDirs;
    private final LockFile[]                    locks;
    private File []                             expandedDbDirs;
    private boolean                             isOpen = false;
    private boolean                             isReadOnly;

    private String                              uid;
    
    @GuardedBy ("streamsLock")
    private final Map <String, ServerStreamImpl>    streams =
        new HashMap<>();

    private final ReadWriteLock                 streamsLock = new ReentrantReadWriteLock();

    @GuardedBy ("dataFiles")
    private final Map <String, File>            dataFiles =
        new TreeMap<>();
    
    @GuardedBy ("expandedDbDirs")
    private int                                 dirIdx = 0;
    
    volatile GrowthPolicy                       growthPolicy =
        new MixedGrowthPolicy (2, 1 << 10, 1 << 24);

    volatile long                               commitDelay = 5000;
    private RecordClassSet                      metaData;
    private File                                mdFile;

    @GuardedBy ("metaData")
    private long                                mdVersion;

    private final Runnable                      mdUpdater =
        new Runnable () {
            public void         run () {
                storeMetaData ();
            }
        };

    private TickLoader                          logLoader; // loader for the event stream
    Timer                                       streamSaver = null;
    ThrottlingExecutor                          saver;
    private PQCache                             pqCache = new PQCache (this);

    final PersistentDataStore                   store;
    private FSLocator                           locator;
    FSOptions                                   fs;
    private final ContextContainer              contextContainer;

    // Note: this implementation supports only one remote FS at at time. To use multiple FS we need a collection here.
    private RemoteStreamSyncChecker remoteStreamSyncChecker;
    private AbstractPath remoteStreamRootPath;

    /**
     * Guards {@link #open(boolean)} and {@link #close()} operations.
     *
     * This lock MUST NOT be acquired if current thread al already own lock on {@code this}.
     */
    private final Object openCloseLock = new Object();

    private final Thread emergencyShutdownThread = createEmergencyShutdownThread();

    // If true then TimeBase must be closed and can't be re-opened anymore
    private volatile boolean criticalFailure = false;

    private Thread createEmergencyShutdownThread() {
        Thread thread = new Thread(this::emergencyShutdown);
        thread.setName("TimeBase-EmergencyShutdownThread");
        return thread;
    }

    static boolean                       isVersion5(String version) {
        if (version == null)
            version = System.getProperty(VERSION_PROPERTY);

        return "5.0".equals(version) || "5".equals(version);
    }

    public static String    getFolderName(String      version) {
        return isVersion5(version) ? "timebase" : "tickdb";

//        String      version = config.getString("version", System.getProperty(TickDBImpl.VERSION_PROPERTY, "4.3"));
//        System.setProperty(TickDBImpl.VERSION_PROPERTY, version);
//        File tbFolder = QSHome.getFile ("5.0".equals(version) ? "timebase" : "tickdb");
    }

    public TickDBImpl (File ... dbDirs) {
        this(new DataCacheOptions(), dbDirs);
    }

    public TickDBImpl (DataCacheOptions options, File ... dbDirs) {
        this(null, options, dbDirs);
    }

    public TickDBImpl (String uid, DataCacheOptions options, File ... dbDirs) {

        if (dbDirs.length == 0)
            throw new IllegalArgumentException ("Must supply a folder list.");

        this.dbDirs = dbDirs;
        this.locks = new LockFile[dbDirs.length];
        this.uid = uid;

        //int heapSize = (int) Math.min(PDSFactory.MAX_HEAP_SIZE, Util.getAvailableMemory() / 2);
        //long cacheSize = options.cacheSize - heapSize;

        this.contextContainer = new ContextContainer();
        this.contextContainer.setQuickExecutorName("TickDBImpl Executor");

        //PDSFactory.allocate(heapSize);
        store = PDSFactory.create(this.contextContainer.getQuickExecutor());
        store.setEmergencyShutdownControl(TickDBImpl.this::startEmergencyShutdown);

        if (isVersion5(null)) {
            final long cacheSize = options.cacheSize;
            long ramdiskSize = 0;
            if (cacheSize > 0) {
                // We have to split cache quota between RamDisk, LocalFS cache and DFS cache. Each of them can be disabled.
                CacheQuotaSettings quota = divideCacheQuota(cacheSize, ENABLE_RAMDISK_CACHE, ENABLE_CHUNKED_CACHE_FOR_LOCAL, ENABLE_CHUNKED_CACHE_FOR_DFS, CHUNKED_CACHE_DFS_SIZE_MB);
                ramdiskSize = quota.ramdiskSize;

                FSFactory.init(options.fs.maxFileSize, quota.localFsCacheSize, quota.dsfCacheSize, options.preallocateRatio);
            }
            if (ramdiskSize > 0) {
                this.ramdisk = RAMDisk.createCached(options.maxNumOpenFiles, ramdiskSize, options.getInitialCacheSize());
            } else {
                this.ramdisk = RAMDisk.createNonCached(options.maxNumOpenFiles);
            }
        } else {
            if (options.cacheSize > 0)
                this.ramdisk = RAMDisk.createCached(options.maxNumOpenFiles, options.cacheSize, options.getInitialCacheSize());
            else
                this.ramdisk = RAMDisk.createNonCached(options.maxNumOpenFiles);

            this.ramdisk.setShutdownTimeout(options.shutdownTimeout);
        }

        this.fs = options.fs != null ? options.fs : new FSOptions();
    }

    /**
     * We have to split cache quota between RamDisk, LocalFS cache and DFS cache. Each of them can be disabled.
     * DFS may have pre-set size.
     *
     * @param cacheSize Total cache size to be split between all candidates.
     * @param enableRamDiskCache If RamDisk enabled.
     * @param enableChunkedCacheForLocal If cache enable for local FS.
     * @param enableChunkedCacheForDfs If cache enabled for DFS.
     * @param chunkedCacheDfsSizeMb Pre-set size of cache for DFS.
     */
    static CacheQuotaSettings divideCacheQuota(long cacheSize, boolean enableRamDiskCache, boolean enableChunkedCacheForLocal, boolean enableChunkedCacheForDfs, Integer chunkedCacheDfsSizeMb) {
        byte candidateCount = 0;
        if (enableRamDiskCache) {
            candidateCount++;
        }
        if (enableChunkedCacheForLocal) {
            candidateCount++;
        }
        if (enableChunkedCacheForDfs) {
            candidateCount++;
        }

        long totalSizeToSplit = cacheSize;

        long allocatedToDfs = 0;
        // Note: We allocate to DFS only of explicit DFS cache size size is set
        if (enableChunkedCacheForDfs && chunkedCacheDfsSizeMb != null) {
            allocatedToDfs = chunkedCacheDfsSizeMb << 20;
            if (totalSizeToSplit < allocatedToDfs) {
                throw new IllegalArgumentException("DFS settings request more cache space than available: " + allocatedToDfs + " bytes requested but cache size is " + cacheSize);
            }
            totalSizeToSplit -= allocatedToDfs;
            candidateCount--; // No need to allocate for DFS anymore
        }

        long allocatedToRamDisk = 0;
        if (enableRamDiskCache) {
            allocatedToRamDisk = Math.min(totalSizeToSplit / candidateCount, MAX_RAM_DISK_SIZE);
            totalSizeToSplit -= allocatedToRamDisk;
            candidateCount--;
        }
        long allocatedToLocalCache = 0;
        if (enableChunkedCacheForLocal) {
            allocatedToLocalCache = totalSizeToSplit / candidateCount;
            totalSizeToSplit -= allocatedToLocalCache;
            candidateCount--;
        }
        if (candidateCount > 0) {
            assert candidateCount == 1;
            assert enableChunkedCacheForDfs;
            assert allocatedToDfs == 0;
            allocatedToDfs = totalSizeToSplit / candidateCount;
            candidateCount--;
            //totalSizeToSplit -= allocatedToDFS;
        }
        assert candidateCount == 0;

        return new CacheQuotaSettings(allocatedToRamDisk, allocatedToLocalCache, allocatedToDfs);
    }

    static class CacheQuotaSettings {
        final long ramdiskSize;
        final long localFsCacheSize;
        final long dsfCacheSize;

        CacheQuotaSettings(long ramdiskSize, long localFsCacheSize, long dsfCacheSize) {
            this.ramdiskSize = ramdiskSize;
            this.localFsCacheSize = localFsCacheSize;
            this.dsfCacheSize = dsfCacheSize;
        }
    }

    public String                           getId () {
        return ("TimeBase in " + dbDirs [0].getPath ());
    }

    public long                             getCommitDelay () {
        return commitDelay;
    }

    public void                             setCommitDelay (long commitDelay) {
        this.commitDelay = commitDelay;
    }

    public void                             delete () {
        if (!isOpen)
            open (false);
        
        for (DXTickStream s : streams ())
            s.delete ();

        assert streams.isEmpty ();
        
        for (File f : new ArrayList <File> (dataFiles.values ()))
            f.delete ();

        close ();
    }

    void                                    streamDeleted (String key) {
        ServerStreamImpl stream;
        streamsLock.writeLock().lock();
        try {
            stream = streams.remove(key);
        } finally {
            streamsLock.writeLock().unlock();
        }

        invalidateQueryCache(stream);

        if (stream != null)
            fireDeleted(stream);
    }

    void                                    streamRenamed (String newKey, String key) {
        ServerStreamImpl tickStream;
        streamsLock.writeLock().lock();
        try {
            tickStream = streams.remove(key);
            if (tickStream != null)
                streams.put(newKey, tickStream);
        } finally {
            streamsLock.writeLock().unlock();
        }

        invalidateQueryCache(tickStream);
    }
    
    private void                            storeMetaData () {
        try {
            synchronized (metaData) {
                UHFJAXBContext.createMarshaller ().marshal (metaData, mdFile);
                mdVersion++;
            }
        } catch (JAXBException x) {
            throw new com.epam.deltix.util.io.UncheckedIOException("Failed to store TimeBase Meta Data to file " + mdFile, x);
        }
    }

    public void                             format () {
        if (isOpen ())
            throw new IllegalStateException ("Database is open");

        if (REMOTE_STREAMS) {
            throw new IllegalStateException ("Can't format database with remote streams");
        }

        expandedDbDirs = dbDirs;

        boolean success = false;
        try {
            lock(false); // just try to lock
            unlock(true);
            
            TickDBFolderManager     fm = new TickDBFolderManager (false);
            fm.format (dbDirs);
            success = true;
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            if (!success) // unlock folders
                unlock(true);
        }

        mdVersion = System.currentTimeMillis ();
        mdFile = createDataFile (MD_FILE_NAME);
        metaData = new RecordClassSet ();
        metaData.addChangeListener (mdUpdater);

        // clear cache
        pqCache.clear();

        lock(false);
        storeMetaData ();
        setUp ();
        isOpen = true;
    }

    public synchronized boolean             isOpen () {
        return (isOpen);
    }

    public synchronized boolean             isReadOnly () {
        return (isReadOnly);
    }

//    public static RandomAccessFileStore     use(File folder, boolean readOnly) {
//        return use(folder, readOnly, null);
//    }

    static LockFile    use(File folder, boolean readOnly) {
        try {
            File lockFile = new File(folder, LOCK_FILE_NAME);

            boolean exists = lockFile.exists();

            if (!exists) {
                if (!lockFile.createNewFile())
                    throw new RuntimeException("Failed create lock file in " + folder);
            }
            
            LockFile raf = new LockFile(lockFile, exists ? LockFile.State.TERMINATED : LockFile.State.CLOSED);
            boolean success = false;
            try {
                raf.open(readOnly);
                
//                if (!readOnly && magic != null)
//                    raf.setRoot(magic);

                success = true;
                return raf;
            } finally {
                if (!success)
                    Util.close(raf);
            }
        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }
    }

    private void                            lock(boolean readonly) {
        for (int i = 0; i < dbDirs.length; i++)
            if (dbDirs[i].exists()) {
                locks[i] = use(dbDirs[i], readonly);

                try {
                    if (uid == null)
                        uid = locks[i].readMagic();
                    else
                        locks[i].writeMagic(uid);
                } catch (IOException e) {
                    // ignore
                }
            }
    }

    private void                            unlock(boolean cleanup) {
        
        for (int i = 0; i < locks.length; i++) {
            LockFile lock = locks[i];

            try {
                if (lock != null && uid != null)
                    lock.writeMagic(uid);
            } catch (IOException e) {
                LOGGER.warning("Cannot write uid into file:" + lock);
            }

            Util.close(lock);
            
            if (lock != null && cleanup)
                lock.delete();
            
            locks[i] = null;
        }
    }

    static boolean                          isRootFolder42(File dir) {
        String[] files = dir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(STREAM_EXTENSION);
            }
        });

        if (files != null && files.length > 0) {
            try {
                String text = IOUtil.readTextFile(new File(dir, files[0]));
                return text.contains("http://xml.deltixlab.com/internal/quantserver/2.0");
            } catch (IOException e) {
                return false;
            } catch (InterruptedException e) {
                return false;
            }
        }

        return false;
    }

    public static boolean                   isDBFile(File dir, String name) {
        if (name.endsWith(DATA_EXTENSION))
            return name.startsWith("x") ||
                    name.startsWith("m") ||
                    name.startsWith("z") ||
                    name.endsWith(TIMECACHE_FILE_NAME) ||
                    name.endsWith(TickStreamImpl.UNIQUE_FILE_SUFFIX) ||
                    name.endsWith(TickStreamImpl.VERSIONS_FILE_SUFFIX) ||
                    name.equals(INDEX_FILE);
        
        return MD_FILE_NAME.equals(name)||CATALOG_NAME.equals(name) ||
               LOCK_FILE_NAME.equals(name) || name.contains(STREAM_EXTENSION) ||
               SYMBOLS_FILE.equals(name) || CONFIG_FILE.equals(name);
    }

    public void                open (boolean readOnly) {
        assert !Thread.holdsLock(this);

        synchronized (openCloseLock) {
            if (criticalFailure) {
                throw new IllegalStateException("Can't open database because of previously encountered critical error");
            }

            synchronized (this) {
                try {

                    if (isVersion5(null))
                        LOGGER.warning("Timebase running using 5.0 data streams engine.");

                    openLocked(readOnly);

                } catch (IllegalStateException e) {
                    unlock(true);
                    throw e;
                } catch (IOException e) {
                    unlock(false);
                    throw new com.epam.deltix.util.io.UncheckedIOException(e);
                } catch (RuntimeException e) {
                    unlock(false);
                    throw e;
                } finally {
//            if (LicenseController.instance().getDbSizeLimit() != null) {
//
//                streamSaver.schedule(new TimerRunner() {
//                    @Override
//                    protected void runInternal() throws Exception {
//                        try {
//                            validateDbSize();
//                        } catch (IllegalStateException e) {
//                            closeActiveLoaders();
//                        }
//                    }
//
//                }, 0, 1000 * 3600); // one hour
//            }
                }
            }
        }
    }

//    void                                    validateDbSize() {
//        if (LicenseController.instance().getDbSizeLimit() != null)
//            LicenseController.instance().validateDbSize(getActualSize());
//    }

//    private void                            closeActiveLoaders() {
//        for (TBLoader loader : getOpenLoaders()) {
//            if (loader instanceof TickLoader && ((TickLoader)loader).getTargetStream() instanceof DurableStreamImpl) {
//                LOGGER.severe("Closing loader [" + loader + "] due to license violation: db size limit exceeds.");
//                ((TickLoader)loader).close();
//            }
//        }
//    }

    private static boolean                  isSafeMode() {
        return Boolean.getBoolean(SAFE_MODE_PROPERTY);
    }

    private  void                           openLocked (boolean readOnly) throws IOException {
        if (isOpen ())
            throw new IllegalStateException ("Database is already open");

        Throwable suppressedError = cleanup(false);
        if (suppressedError != null) {
            this.criticalFailure = true;
            ExceptionUtils.rethrow(suppressedError);
        }

        ArrayList<File> toDistribute = new ArrayList<File>();

        for (File dbDir : dbDirs)
            if (isRootFolder42(dbDir))
                toDistribute.add(dbDir);

        lock(readOnly);

        if (fs != null && fs.url != null)
            this.locator = new FSLocator(fs.url + "/" + uid);

        // distribute folders from version 4.2 only
        FolderSpreader fs = new FolderSpreader();
        fs.distribute(toDistribute.toArray(new File[toDistribute.size()]));

        TickDBFolderManager fm = new TickDBFolderManager (readOnly, dbDirs);
        Map <String, File> fileMap = fm.getNameToFileMap ();

        Collection <File> dbDirCollection = fm.getDbDirSet ();
        expandedDbDirs = dbDirCollection.toArray (new File [dbDirCollection.size ()]);

        Map<String, FileLocation> streamFiles = new TreeMap<>();

        mdFile = null;

        for (Map.Entry <String, File> e : fileMap.entrySet ()) {
            File            f = e.getValue ();
            String          fname = e.getKey ();

            if (fname.endsWith (STREAM_EXTENSION)) {
                if (fname.startsWith(TickStreamImpl.NEW_PREFIX)) {
                    fname = fname.substring(TickStreamImpl.NEW_PREFIX.length());

                    File correct = new File(f.getParent(), fname);
                    File backup = new File(f.getParent(), fname + TickStreamImpl.BACKUP_SUFFIX);
                    
                    if (!correct.exists() && backup.exists() && f.renameTo(correct)) {
                        streamFiles.put (fname, new FileLocation(correct));
                        LOGGER.info("Recovering incomplete stream definition file from " + f.getName());
                    } else {
                        f.delete();
                    }

                } else {
                    streamFiles.put (fname, new FileLocation(f));
                }
            }
            else if (fname.equals (MD_FILE_NAME))
                mdFile = f;
            else
                dataFiles.put (fname, f);
        }

        mdVersion = System.currentTimeMillis ();

        try {
            if (mdFile == null) {
                mdFile = createDataFile (MD_FILE_NAME);
                metaData = new RecordClassSet ();
            } else {
                //TDBUpgrade23.upgradeClassSet(mdFile);
                metaData = (RecordClassSet) IOUtil.unmarshal(UHFJAXBContext.createUnmarshaller(), mdFile);
            }
        } catch (JAXBException | IOException x) {
            throw new com.epam.deltix.util.io.UncheckedIOException("Failed to load TimeBase Meta Data from file " + mdFile, x);
        }

        metaData.addChangeListener (mdUpdater);
        
        TDBUpgrade.EventListener uglnr =
            new TDBUpgrade.EventListener () {
                public void     beginUpgrading (File f, String streamKey) {
                }

                public void     doneUpgrading (File f, String streamKey) {
                    LOGGER.warning (
                        "Stream " + streamKey +
                        " has been automatically upgraded to current format. Backup of " +
                        f + " has been saved."
                    );
                }

                public void     folderNotFound (File catalog, File folder) {
                    throw new RuntimeException ("unexpected");
                }

                public void     upToDate (File f, String streamKey) {
                }
            };

        // set up all threads
        setUp ();

        // now we can open streams
        
        Map <File, File>                  actualFiles = new HashMap <File, File>();
        actualFiles.put(mdFile, mdFile);
        File catFile = new File(mdFile.getParentFile(), CATALOG_NAME);
        actualFiles.put(catFile, catFile);

        // TODO: @LEGACY
        //SchemaUpdater migrator = new SchemaUpdater(new ClassMappings());

        addRemoteStreams(streamFiles);

        for (FileLocation sf : streamFiles.values ()) {
            boolean isLocal = sf.isLocal();
            // TODO: Implement version upgrade for remote files
//            if (isLocal) {
//                File streamFile = sf.getFile();
//                try {
//                    TDBUpgrade3.removeJavaClassName(streamFile);
//
//                    boolean updated = TDBUpgrade23.upgradeStream(streamFile, uglnr);
//                    if (!updated)
//                        TDBUpgrade23.upgradeContentClasses(streamFile, uglnr);
//                    TDBUpgrade23.upgradeDataConnectorStatus(streamFile, uglnr);
//                    TDBUpgrade23.renameL2Messages(streamFile, uglnr);
//
//                } catch (IOException iox) {
//                    throw new com.epam.deltix.util.io.UncheckedIOException(// "Attempt to upgrade stream file " + streamFile +// " has resulted in " + iox,// iox//);
//                } catch (InterruptedException iox) {
//                    throw new com.epam.deltix.util.io.UncheckedIOException(// "Interrupted while upgrading stream file " + streamFile,// iox//);
//                }
//            }

            DXTickStream  tickStream = TickStreamImpl.read (sf.getPath());
            if (tickStream instanceof TickStreamImpl) {
                TickStreamImpl stream = (TickStreamImpl)tickStream;

                // TODO: Implement version upgrade for remote files
                if (isLocal) {
                    File streamFile = sf.getFile();
                    assert streamFile != null;

                    try {
                        if (!TickStreamImpl.VERSION.equals(stream.getVersion())) { // backup file
                            String name = streamFile.getName() + "." + stream.getVersion() + ".bak";
                            File backup = new File(streamFile.getParent(), name);
                            IOUtil.copyFile1(streamFile, backup);
                        }
                    } catch (IOException ex) {
                        LOGGER.log(Level.WARNING, "Cannot backup file: " + streamFile, ex);
                    }
                }

                stream.init (this, sf);

                // securities in general may have periodic data

//                //securities stream must have static periodicity
//                if (stream.getKey().equals(TickStreamImpl.SECURITIES))
//                    stream.setPeriodicity(Periodicity.mkStatic());

                if (EVENTS_STREAM_NAME.equals(stream.getKey())) {
                    // delete TRANSIENT stream - now it's RUNTIME
                    stream.open (false);
                    stream.delete();
                    continue;
                } else {
                    try {
                        stream.open (readOnly);
                        stream.getSizeOnDisk(); // will fail in case of stream errors
                    } catch (Throwable e) {
                        if (!isSafeMode())
                            throw e;

                        LOGGER.log(Level.WARNING, "Disabling broken stream [" + stream.getKey() + "] according to 'SAVE MODE'", e);
                        continue;
                    }
                }

//                // TODO: Implement upgrade for remote files
//                if (!readOnly && isLocal) {
//                    if (TDBUpgrade3.upgradeStaticValues(stream.getClassDescriptors()))
//                        stream.saveToFile();
//
//                    RecordClassSet set = TDBUpgrade23.upgradePeriodicity(stream);
//                    if (set != null)
//                        stream.setMetaData(stream.isPolymorphic(), set);
//
//                    set = TDBUpgrade23.upgradeExchangeCode(stream);
//                    if (set != null)
//                        stream.setMetaData(stream.isPolymorphic(), set);
//
//                    set = TDBUpgrade3.fixL2Messages(stream.getKey(), stream.getMetaData());
//                    if (set != null)
//                        stream.setMetaData(stream.isPolymorphic(), set);
//                }

                // TODO: @LEGACY
//                try {
//                    if (migrator.update(stream.getMetaData())) {
//                        if (isLocal) {
//                            if (!readOnly) {
//                                stream.saveToFile();
//                                LOGGER.log(Level.INFO, "Stream [" + stream.getKey() + "] schema migrated successfully.");
//                            }
//                        } else {
//                            // TODO: Implement upgrade for remote files
//                            LOGGER.log(Level.WARNING, "Skipped remote stream [" + stream.getKey() + "] schema update (NOT SUPPORTED)");
//                        }
//                    }
//                } catch (Introspector.IntrospectionException | ClassNotFoundException | StackOverflowError e) {
//                    LOGGER.log(Level.WARNING, "Failed to update stream [" + stream.getKey() + "] schema.", e);
//                } catch (Throwable e) {
//                    if (!isSafeMode())
//                        throw e;
//
//                    LOGGER.log(Level.WARNING, "Disabling broken stream [" + stream.getKey() + "] according to 'SAVE MODE'", e);
//                    continue;
//                }

                streams.put (stream.getKey (), stream);
                stream.addStateListener(this);

                // Build list of files used by this stream
                // TODO: Do we need that for remote streams?
                if (sf.isLocal()) {
                    for (File file : stream.listFiles()) {
                        actualFiles.put(file.getParentFile(), file.getParentFile());
                        actualFiles.put(file, file);
                    }

                    if (stream.isUnique())
                        actualFiles.put(stream.getCacheFile(), stream.getCacheFile());

                    actualFiles.put(stream.getVersionsFile(), stream.getVersionsFile());
                }
            }
            else if (tickStream instanceof FileStreamImpl) {
                if (sf.isRemote()) {
                    throw new IllegalStateException("Remote storage is not supported for FileStreamImpl");
                }
                FileStreamImpl fileStream = (FileStreamImpl)tickStream;
                fileStream.init (this, sf.getFile());
                fileStream.open (readOnly);
                streams.put (tickStream.getKey (), (FileStreamImpl) tickStream);
            }
        }

        // clear unused files only if we have db here 
        if (actualFiles.size() > 0) {
            for (File file : dataFiles.values()) {
                if (!actualFiles.containsKey(file) && file.exists() &&
                        !CATALOG_NAME.equals(file.getName()) ) {
                    LOGGER.info ("Deleting unreferenced file " + file + " ...");
                    file.delete();
                }
            }
        }

        isOpen = true;
        isReadOnly = false;

        // create system streams
        if (!streams.containsKey(EVENTS_STREAM_NAME)) {
            StreamOptions options = StreamOptions.fixedType(StreamScope.RUNTIME, EVENTS_STREAM_NAME, "logs", 0,
                    mkEventMessageDescriptor());
            (options.bufferOptions = new BufferOptions()).lossless = false;
            options.bufferOptions.initialBufferSize = 1024*1024;
            options.bufferOptions.maxBufferSize = 10*1024*1024;
            //for this stream set periodicity to IRREGULAR
            options.periodicity = Periodicity.parse(String.valueOf(Periodicity.Type.IRREGULAR));
            createStream (EVENTS_STREAM_NAME, options);
        }

        TickStreamImpl log = (TickStreamImpl) streams.get(EVENTS_STREAM_NAME);
        // locking stream to prevent deletion
        log.lock(LockType.READ);
        // create global loader
        LoadingOptions options = new LoadingOptions();
        options.channelQOS = ChannelQualityOfService.MIN_INIT_TIME;
        logLoader = log.createLoader(options);
        // set stream as 'final'
        log.setFinal(true);
        
        // now we can set read-only flag
        isReadOnly = readOnly;
        // restore stream background processes - now we have deleted all unused files and can run any process
        for (ServerStreamImpl tickStream : streams.values())
            tickStream.onDBOpen ();

        if (REMOTE_STREAMS) {
            remoteStreamSyncChecker.start();
        }
    }

    private void addRemoteStreams(Map<String, FileLocation> streamFiles) throws IOException {
        if (!REMOTE_STREAMS) {
            return;
        }

        String fsUrl = fs.url;
        if (fsUrl == null) {
            throw new IllegalArgumentException("TimeBase was configured to use remote streams but FSOptions.url is not set");
        }

        /*
        if (!Azure2FS.isAllRequiredPropertiesSet()) {
            throw new IllegalArgumentException("TimeBase was configured to use remote streams but AzureFS2 properties are not set");
        }
        */

        String remotePath = fsUrl + "/" + REMOTE_STREAM_PATH_PREFIX + uid; // TODO: Try to use locator
        AbstractFileSystem remoteFs = FSFactory.createNonCached(remotePath); // remote stream metadata root
        AbstractPath remoteRoot = remoteFs.createPath(remotePath); // remote stream metadata root
        AbstractPath remoteStreamRootPath = remoteRoot.append(".streams");
        if (remoteStreamRootPath.exists() && remoteStreamRootPath.isFolder()) {
            // This is valid folder: enable remote streams
            this.remoteStreamRootPath = remoteStreamRootPath;
            this.remoteStreamSyncChecker = new RemoteStreamSyncChecker(remoteStreamRootPath, contextContainer.getAffinityConfig(), new TickDBRemoteStreamChangeObserver(this, streams, streamsLock, remoteStreamRootPath));

            for (String childName : remoteStreamRootPath.listFolder()) {
                AbstractPath entry = remoteStreamRootPath.append(childName);
                if (entry.isFolder()) {
                    // TODO: Check for NEW_PREFIX/BACKUP_SUFFIX and perform locked rename if necessary
                    String fileName = childName + STREAM_EXTENSION;
                    AbstractPath streamFile = entry.append(fileName);
                    if (streamFile.exists() && streamFile.isFile()) {
                        if (streamFiles.containsKey(fileName)) {
                            throw new IllegalStateException("Remote stream has same key as local stream: " + childName);
                        }
                        streamFiles.put(childName, new FileLocation(streamFile));
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("TimeBase was configured to use remote streams but \"" + remoteStreamRootPath.getPathString() + "\" is not valid folder");
        }
    }

    private void                            setUp () {
        this.contextContainer.getQuickExecutor().reuseInstance();

        streamSaver = new Timer ("Stream Saver", true);
        
        saver = new ThrottlingExecutor("Timebase auto-save thread", 0.1);
        saver.setMaxSleepInterval (2000);
        saver.setDaemon (true);
        saver.start ();

        ramdisk.start();

        if (store != null)
            store.start();
    }

    public QuickExecutor                    getQuickExecutor() {
        return this.contextContainer.getQuickExecutor();
    }

    /**
     * @return first encountered but suppressed exception (if any)
     */
    @Nullable
    @CheckReturnValue
    private Throwable                            cleanup(boolean shutdown) {
        Throwable suppressedException = null;

        streamsLock.writeLock().lock();
        try {
            for (ServerStreamImpl stream : streams.values()) {
                stream.removeStateListener(this);
                try {
                    ((Disposable) stream).close();
                }catch (Throwable e) {
                    if (suppressedException == null) {
                        suppressedException = e;
                    }
                    LOG.error("Error during cleanup (shutdown phase): %s").with(e);
                }
            }
            streams.clear();
        } finally {
            streamsLock.writeLock().unlock();
        }

        synchronized (dataFiles) {
            dataFiles.clear ();
        }

        pqCache.clear();
        dirIdx = 0;
        mdFile = null;
        metaData = null;

        // TODO: Do not create instance just for shutdown
        // Warning: using getQuickExecutor().shutdownInstance(); results in error because this method is executed both on startup and shutdown
        if (shutdown) {
            this.contextContainer.getQuickExecutor().shutdownInstance();
        }
        //this.contextContainer.shutdownQuickExecutor();

        if (streamSaver != null) {
            streamSaver.cancel ();
            streamSaver = null;
        }

        if (saver != null) {
            saver.interrupt();
            saver = null;
        }

        return suppressedException;
    }

    @Override
    public void                close () {
        Throwable exception = closeInternal();
        if (exception != null) {
            ExceptionUtils.rethrow(exception);
        }
    }

    /**
     * This method will try it's best to finish even if there are exceptions.
     * It will suppress stream specific exceptions and will return the fist encountered exception as result.
     * The calling code is responsible to re-throw and propagate such exceptions.
     *
     * Please keep in mind that despite this code tries it's best to finish "shutdown" process properly
     * it's still possible that some new kind of exception can be encountered in the middle of process.
     *
     * @return first suppressed exception
     */
    @Nullable
    private Throwable closeInternal() {
        assert !Thread.holdsLock(this);

        Throwable firstException = null;

        synchronized (openCloseLock) {
            if (!isOpen())
                return firstException;

            boolean finished = false;
            try {

                if (REMOTE_STREAMS) {
                    remoteStreamSyncChecker.stop(true);
                }

                // close loader for events# stream
                logLoader = Util.close(logLoader);

                synchronized (this) {
                    ramdisk.startShutdown();
                }

                if (store != null) {
                    store.startShutdown();
                }

                // close all readers/writers, to push unsaved data into writer thread

                streamsLock.writeLock().lock();
                try {
                    for (ServerStreamImpl stream : streams.values()) {
                        if (stream instanceof PDStream) {
                            try {
                                ((PDStream) stream).cleanup();
                            } catch (Throwable e) {
                                if (firstException == null) {
                                    firstException = e;
                                }
                                LOG.error("Error during shutdown cleanup: %s").with(e);
                            }
                        }
                    }
                } finally {
                    streamsLock.writeLock().unlock();
                }

                if (store != null) {
                    //noinspection StatementWithEmptyBody
                    while (!store.waitUntilDataStored(1000)) ;
                    // just wait until all files are stored;

                    store.shutdown();
                    store.waitForShutdown(0);
                }

                Throwable cleanupException = cleanup(true);// should be NOT synchronized
                if (firstException == null) {
                    firstException = cleanupException;
                }

                synchronized (this) {
                    try {
                        ramdisk.shutdownAndWait();
                    } catch (InterruptedException e) {
                        LOGGER.log(Level.WARNING, "Ramdisk shutdown error.", e);
                    }
                    isOpen = false;
                }

                unlock(false);
                finished = true;
            } finally {
                if (!finished || firstException != null) {
                    criticalFailure = true;
                }
            }
        }
        return firstException;
    }

    /**
     * Starts Emergency shutdown.
     *
     * Emergency shutdown means:
     * 1) Critical TimeBase error has happened
     * 2) TimeBase will be closed and can't be re-opened anymore
     * 3) JVM will be terminated
     */
    private void startEmergencyShutdown() {
        synchronized (emergencyShutdownThread) {
            if (emergencyShutdownThread.getState() == Thread.State.NEW) {
                this.criticalFailure = true;
                LOG.warn("Triggering Emergency shutdown from thread \"%s\"").with(Thread.currentThread().getName());
                emergencyShutdownThread.start();
            }
        }
    }

    private void emergencyShutdown() {
        LOG.warn("Executing Emergency shutdown...");
        long startTime = System.currentTimeMillis();
        try {
            //noinspection ThrowableNotThrown
            closeInternal();
            LOG.warn("Emergency shutdown is complete in %s ms").with(System.currentTimeMillis() - startTime);
        } catch (Throwable e) {
            LOG.warn("Emergency shutdown is aborted by error (after %s ms): %s")
                    .with(System.currentTimeMillis() - startTime)
                    .with(e);
        } finally {
            LOG.error("Timebase will shutdown.");
            Shutdown.asyncTerminate();
        }
    }

    public void                             warmUp () {
        if (!isOpen ())
            throw new IllegalStateException ("Database is not open");

        LOGGER.info ("Database warmup begins...");

        for (ServerStreamImpl stream : streams ()) {
            if (stream.getHighAvailability())
                stream.warmUp();
        }

        LOGGER.info ("Database warmup has been completed.");

        GlobalStats     rds = ramdisk.getStats ();

        LOGGER.info (
            String.format (
                "RAM Disk Statistics: DB size: %,.3f GB, cached: %,d MB; used: %,d MB; #files: %,d.",
                (double) getSizeOnDisk() / (1024 * 1024 * 1024),
                rds.getCachedMemory() >> 20,
                rds.getUsedMemory() >> 20,
                rds.getNumOpenVirtualFiles()
            )
        );

        //LOGGER.info ("Memory Statistics");

        if (rds.getNumOpenVirtualFiles() * 4 > rds.getNumPages())
            LOGGER.warning("Insufficient RAMDisk size: less then 4 pages per data file. Consider increasing to " +
                    ((rds.getNumOpenVirtualFiles() * 4 * RAMDisk.PAGE_SIZE_WITH_OVERHEAD) >> 20) + "MB");
    }

    public void                             coolDown () {
        if (!isOpen ())
            throw new IllegalStateException ("Database is not open");

        for (DXTickStream stream : streams())
            ((ServerStreamImpl)stream).coolDown();        
    }

    public void                             trimToSize () {
        if (!isOpen ())
            throw new IllegalStateException ("Database is not open");

        if (isReadOnly ())
            throw new IllegalStateException ("Database is open in read-only mode");

        try {
            for (DXTickStream stream : streams())
                ((ServerStreamImpl)stream).trimToSize();
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        }
    }
    
//    public long                             getSizeOnDisk () {
//        if (!isOpen ())
//            throw new IllegalStateException ("Database is not open");
//
//        long            ret = 0;
//
//        synchronized (dataFiles) {
//            for (File f : dataFiles.values ())
//                ret += f.length ();
//        }
//
//        return (ret);
//    }

    public long                             getSizeOnDisk () {
        if (!isOpen ())
            throw new IllegalStateException ("Database is not open");

        long            ret = 0;

        // calculating actual using snapshot
        ServerStreamImpl[] dbStreams = streams();

        for (ServerStreamImpl stream : dbStreams)
            ret += stream.getSizeOnDisk();

        return (ret);
    }

    void                                    renameDataFile(String name, File file) {
        synchronized (dataFiles) {
            File    f = (dataFiles.get (name));

            if (f == null)
                throw new com.epam.deltix.util.io.UncheckedIOException("DB file not found: " + name);

            dataFiles.put(file.getName(), file);
            dataFiles.remove(name);
        }
    }

    File                                    getDataFile (String name) {
        synchronized (dataFiles) {
            File    f = (dataFiles.get (name));

            if (f == null)
                throw new com.epam.deltix.util.io.UncheckedIOException("DB file not found: " + name);

            return (f);
        }
    }

    boolean                                 hasDataFile (String name) {
        synchronized (dataFiles) {
            return dataFiles.get (name) != null;
        }
    }

    void                                    deleteDataFile (String name) {
        synchronized (dataFiles) {
            File    f = (dataFiles.get (name));

            if (f == null)
                throw new com.epam.deltix.util.io.UncheckedIOException("DB file not found: " + name);

            FileLocation loc = new FileLocation(f);

            try {
                loc.getPath().deleteIfExists();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            dataFiles.remove (name);
        }
    }

    /**
     *  Round-robin among database folders.
     */
    private File                            getNewDir () {
        synchronized (expandedDbDirs) {
            File    dir = expandedDbDirs [dirIdx++];

            if (dirIdx == expandedDbDirs.length)
                dirIdx = 0;

            return (dir);
        }
    }

    File                                    createDataFile (File parent, String name) {
         synchronized (dataFiles) {
            File    dataFile = dataFiles.get (name);

            if (dataFile == null) {
                dataFile = new File (parent, name);
                dataFiles.put (name, dataFile);
            }

            return (dataFile);
        }
    }

    private File                                    createDataFile (String name) {
        return createDataFile(getNewDir(), name);
    }

    public ServerStreamImpl                 createStream (
        String                                  key,
        String                                  name,
        String                                  description,
        int                                     distributionFactor
    )
    {
        return (
            createStream (
                key,
                new StreamOptions (StreamScope.DURABLE, name, description, distributionFactor)
            )
        );
    }

    public ServerStreamImpl                 createStream (
        String                                  key,
        StreamOptions                           options
    )
    {
        if (!isOpen ())
            throw new IllegalStateException ("Database is not open");

        if (isReadOnly ())
            throw new IllegalStateException ("Database is open in read-only mode");

        ServerStreamImpl       stream;

        String name = SimpleStringCodec.DEFAULT_INSTANCE.encode(key);

        // Remote stream-related
        boolean isRemoteStream = false;
        RemoteStreamLockImpl remoteStreamLock = null;

        streamsLock.writeLock().lock();
        try {
            if (streams.containsKey(key))
                throw new IllegalArgumentException("Duplicate stream key: " + key);

            if (options.name != null) {
                for (DXTickStream tickStream : streams.values()) {
                    if (StringUtils.equals(tickStream.getName(), options.name))
                        throw new IllegalArgumentException("Duplicate stream name: " + options.name);
                }
            }

            if (options.scope == StreamScope.DURABLE) {
                //if (isVersion5(options.version))
                {
                    options.location = locator != null ? locator.getPath(key, "data") : null;
                    isRemoteStream = REMOTE_STREAMS && isRemoteFsLocation(options.location);

                    stream = new PDStream(this, key, options);

                    if (isRemoteStream) {
                        remoteStreamLock = getRemoteStreamLock(key);
                    }
                }
//                else {
//                    stream = new DurableStreamImpl(this, key, options);
//                }
            } else {
                if (options.isFlagSet(TDBProtocol.AF_STUB_STREAM)) {
                    stream = new StubTimeStream(key, options.name, this);
                } else {
                    stream = new TransientStreamImpl(this, key, options);
                }
            }

//            stream =
//                options.scope == StreamScope.DURABLE ?
//                    new DurableStreamImpl (this, key, options) :
//                    new TransientStreamImpl (this, key, options);

            streams.put(key, stream);
            stream.addStateListener(this);


            boolean runtimeOnly = options.scope == StreamScope.RUNTIME;
            File folder = new File(getNewDir(), name);

            // may exists after rename
            int count = 0;
            while (folder.exists() && (getFolderState(folder) & FOLDER_HAS_STREAM_FILES) == FOLDER_HAS_STREAM_FILES) {
                folder = new File(getNewDir(), name + "-" + count);
                count++;
            }

            if (!runtimeOnly)
                folder.mkdirs();

            File f =
                    runtimeOnly ?
                            null :
                            new File(folder, name + STREAM_EXTENSION);

            if (stream instanceof TickStreamImpl) {
                TickStreamImpl tickStream = (TickStreamImpl) stream;

                if (isRemoteStream) {
                    tickStream.init(this, new FileLocation(remoteStreamLock.getStreamMetadataFile()));
                    tickStream.saveToFile(true);
                } else {
                    tickStream.init(this, f);
                }

                if (tickStream instanceof PDStream) {
                    if (isRemoteStream) {
                        ((PDStream) tickStream).formatWithoutLock();
                    } else {
                        tickStream.format();
                    }
                } else {
                    tickStream.open(false);
                }
            }

            fireCreated(stream);

            if (isRemoteStream) {
                remoteStreamLock.releaseSilent(); // TODO: Use non-silent release here
                remoteStreamLock = null;
                remoteStreamSyncChecker.writeChangeToLogStreamCreate(key);
            }

            return (stream);
        } finally {
            streamsLock.writeLock().unlock();
            if (remoteStreamLock != null) {
                remoteStreamLock.releaseSilent();
            }
        }
    }

    public static RecordClassDescriptor mkEventMessageDescriptor ()
    {
        final String            name = EventMessage.class.getName ();

        final DataField []      fields = {
                new NonStaticDataField ("eventType", "EventType",
                        new EnumDataType(true, new EnumClassDescriptor(EventMessageType.class))),
        };

        return new RecordClassDescriptor ( name, name, false, null, fields);
    }

    @Nonnull
    @CheckReturnValue
    RemoteStreamLockImpl getRemoteStreamLock(String key) {
        if (!REMOTE_STREAMS) {
            throw new IllegalStateException();
        }
        String name = SimpleStringCodec.DEFAULT_INSTANCE.encode(key);
        AbstractPath streamFolderRemotePath = remoteStreamRootPath.append(name);
        AbstractPath streamFileRemotePath = streamFolderRemotePath.append(name + STREAM_EXTENSION);
        FsLock remoteLock;
        try {
            remoteLock = AtomicFsLockManager.acquire(AtomicFsLockManager.getLockName(streamFolderRemotePath));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during attempt to get remote stream lock", e);
        }

        return new RemoteStreamLockImpl(key, streamFileRemotePath, remoteLock);
    }

    /**
     * Returns lock for remote stream.
     * Otherwise returns lock stub that does nothing.
     *
     * @param key stream key
     */
    @Nonnull
    @CheckReturnValue
    RemoteStreamLock getRemoteStreamLockOrStub(String key, boolean remote) {
        if (remote) {
            return getRemoteStreamLock(key);
        } else {
            return RemoteStreamLockStub.INSTANCE;
        }
    }

    private boolean isRemoteFsLocation(String location) {
        // TODO: Generalize for multiple remote FS types
        try {
            if (location != null) {
                AbstractFileSystem distributedFS = unwrapFs(FSFactory.getDistributedFS(location));
                if (distributedFS instanceof Azure2FS || distributedFS instanceof DistributedFS) {
                    return true;
                }
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    private AbstractFileSystem unwrapFs(AbstractFileSystem distributedFS) {
        while (distributedFS instanceof Wrapper) {
            Object nestedInstance = ((Wrapper) distributedFS).getNestedInstance();
            if (nestedInstance instanceof AbstractFileSystem) {
                distributedFS = (AbstractFileSystem) nestedInstance;
            } else {
                break;
            }
        }
        return distributedFS;
    }

    public DXTickStream                     createFileStream (
        String                                  key,
        String                                  dataFile)
    {
        if (!isOpen ())
            throw new IllegalStateException ("Database is not open");

        if (isReadOnly ())
            throw new IllegalStateException ("Database is open in read-only mode");

        FileStreamImpl       stream;

        streamsLock.writeLock().lock();
        try {
            if (streams.containsKey (key))
                throw new IllegalArgumentException ("Duplicate stream key: " + key);

            stream = new FileStreamImpl (dataFile, key);
            streams.put (key, stream);
            stream.addStateListener(this);

            String name = SimpleStringCodec.DEFAULT_INSTANCE.encode(key);
            File folder = new File(getNewDir(), name);
            folder.mkdirs();

            stream.init(this, new File(folder, name + STREAM_EXTENSION));
            stream.open(false);
            stream.saveChanges();

            fireCreated(stream);

        } finally {
            streamsLock.writeLock().unlock();
        }

        return (stream);
    }

    public TickStreamImpl                   createAnonymousStream (
        StreamOptions                           options
    )
    {
        options.scope = StreamScope.RUNTIME;
        TransientStreamImpl stream = new TransientStreamImpl(this, null, options);
        stream.open(false);
        return stream;
    }

    public DXTickStream                     getStream (
        String                                  key
    )
    {
        if (!isOpen ())
            throw new IllegalStateException ("Database is not open");

        streamsLock.readLock().lock();
        try {
            return (streams.get (key));
        } finally {
            streamsLock.readLock().unlock();
        }
    }

    private ServerStreamImpl []             streams () {
        streamsLock.readLock().lock();
        try {
            return (streams.values ().toArray (new ServerStreamImpl [streams.size ()]));
        } finally {
            streamsLock.readLock().unlock();
        }
    }

    public long                             getMetaDataVersion () {
        synchronized (metaData) {
            return (mdVersion);
        }
    }

    @Override
    public long                             getServerTime() {
        return System.currentTimeMillis();
    }

    public DXTickStream []                  listStreams () {
        if (!isOpen ())
            throw new IllegalStateException ("Database is not open");
        
        return (streams ());
    }

    public DXChannel[]                      listChannels () {
        return listStreams();
    }

    public TickCursor                       createCursor (
        SelectionOptions                        options,
        TickStream ...                          streams
    )
    {
        if (!isOpen ())
            throw new IllegalStateException ("Database is not open");

        if (streams.length == 1) 
            return streams [0].createCursor (options);
        else 
            return (new TickCursorImpl (this, options, streams));        
    }

    @Override
    public TickCursor                       select(
            long                                time,
            SelectionOptions                    options,
            String[]                            types,
            IdentityKey[]                       entities,
            TickStream ...                      streams)
    {
        if (!isOpen ())
            throw new IllegalStateException ("Database is not open");

        if (streams != null && streams.length == 1) {
            return streams[0].select(time, options, types, entities);
        } else {
            TickCursor cursor = new TickCursorImpl(this, options);
            cursor.reset (time);

            // cursor by default subscribed to all types, so change if types != null
            if (types != null)
                cursor.setTypes(types);

            if (entities != null)
                cursor.addEntities(entities, 0, entities.length);
            else
                cursor.subscribeToAllEntities();

            cursor.addStream(streams);

            return (cursor);
        }
    }

    @Override
    public TickCursor select(long time, SelectionOptions options, String[] types, CharSequence[] symbols, TickStream... streams) {
        if (!isOpen ())
            throw new IllegalStateException ("Database is not open");

        if (streams != null && streams.length == 1) {
            return streams[0].select(time, options, types, symbols);
        } else {
            TickCursor cursor = new TickCursorImpl(this, options);
            cursor.reset (time);

            // cursor by default subscribed to all types, so change if types != null
            if (types != null)
                cursor.setTypes(types);

            if (symbols != null)
                cursor.addSymbols(symbols);
            else
                cursor.subscribeToAllEntities();

            cursor.addStream(streams);

            return (cursor);
        }
    }

    @Override
    public TickCursor select(long time, SelectionOptions options, String[] types, TickStream... streams) {
        if (!isOpen ())
            throw new IllegalStateException ("Database is not open");

        if (streams != null && streams.length == 1) {
            return streams[0].select(time, options, types);
        } else {
            TickCursor cursor = new TickCursorImpl(this, options);
            cursor.reset (time);

            // cursor by default subscribed to all types, so change if types != null
            if (types != null)
                cursor.setTypes(types);

            cursor.subscribeToAllEntities();

            cursor.addStream(streams);

            return (cursor);
        }
    }

    @Override
    public TickCursor select(long time, SelectionOptions options, TickStream... streams) {
        if (!isOpen ())
            throw new IllegalStateException ("Database is not open");

        if (streams != null && streams.length == 1) {
            return streams[0].select(time, options);
        } else {
            TickCursor cursor = new TickCursorImpl(this, options);
            cursor.reset (time);

            cursor.subscribeToAllEntities();

            cursor.addStream(streams);

            return (cursor);
        }
    }

    public void                             setGrowthPolicy (GrowthPolicy policy) {
        growthPolicy = policy;
    }

    @Override
    public File[]                           getDbDirs () {
        return dbDirs;
    }
    
    public MetaData                         getMetaData () {
        return (metaData);
    }

    public InstrumentMessageSource          executeQuery (
        String                                  qql,
        Parameter ...                           params
    )
        throws CompilationException
    {
        return (executeQuery (qql, null, null, params));
    }

    public InstrumentMessageSource          executeQuery (
        String                                  qql,
        SelectionOptions                        options,
        Parameter ...                           params
    )
        throws CompilationException
    {
        return (executeQuery (qql, options, null, params));
    }

    public InstrumentMessageSource          executeQuery (
        String                                  qql,
        SelectionOptions                        options,
        CharSequence []                         ids,
        Parameter ...                           params
    )
        throws CompilationException
    {
        LOGGER.fine("Query: " + qql);
        return (executePreparedQuery (pqCache.prepareQuery (CompilerUtil.parse (qql), ParamSignature.signatureOf (params)),
                options, null, ids, true, 0, params));
    }

    public InstrumentMessageSource          executeQuery (
        String                                  qql,
        SelectionOptions                        options,
        TickStream []                           streams,
        CharSequence []                         ids,
        long                                    time,
        Parameter ...                           params
    )
        throws CompilationException
    {
        LOGGER.fine("Query: " + qql);
        return (executeQuery (CompilerUtil.parse (qql), options, streams, ids, time, params));
    }
    
    public InstrumentMessageSource          executeQuery (
        Element                                 qql,
        SelectionOptions                        options,
        TickStream []                           streams,
        CharSequence []                         ids,
        long                                    time,
        Parameter ...                           params
    )
        throws CompilationException
    {
        return (executePreparedQuery(pqCache.prepareQuery(qql, ParamSignature.signatureOf (params)),
                options, streams, ids, time == TimeConstants.TIMESTAMP_UNKNOWN, time, params));
    }

    @Override
    public InstrumentMessageSource          executePreparedQuery (
        PreparedQuery                           pq,
        SelectionOptions                        options,
        TickStream []                           streams,
        CharSequence []                         ids,
        boolean                                 fullScan,
        long                                    time,
        Parameter []                            params
    )
        throws CompilationException
    {
        InstrumentMessageSource     ims = 
            pq.executeQuery (options, Parameter.valuesOf (params));
        
        if (streams != null)
            ims.addStream (streams);
        
        if (ids == null)
            ims.subscribeToAllEntities ();
        else
            ims.addEntities (toArray(ids), 0, ids.length);
        
        if (fullScan) 
            ims.reset (pq.isReverse () ? Long.MAX_VALUE : Long.MIN_VALUE);
        else
            ims.reset (time);
        
        return (ims);
    }

    IdentityKey[] toArray(CharSequence[] symbols) {
        IdentityKey[] ids = new IdentityKey[symbols.length];
        for (int i = 0; i < symbols.length; i++)
            ids[i] = new ConstantIdentityKey(symbols[i]);

        return ids;
    }
    
    void                                    invalidateQueryCache(ServerStreamImpl stream) {
        // TODO: find related queries
        pqCache.clear();
    }

    public  void                            log(EventMessage msg) {
        msg.setTimeStampMs(TimeStampedMessage.TIMESTAMP_UNKNOWN);
        TickLoader loader = logLoader;
        if (loader != null)
            loader.send(msg);
    }

    private volatile StreamStateListener[]  snStateListeners = { };
    private final Set<StreamStateListener>  stateListeners = new HashSet<StreamStateListener>();

    @Override
    public void                 addStreamStateListener(StreamStateListener listener) {
        if (listener == null)
            return;

        synchronized (stateListeners) {
            stateListeners.add (listener);
            snStateListeners = stateListeners.toArray(snStateListeners);
        }
    }

    @Override
    public void                 removeStreamStateListener(StreamStateListener listener) {
        synchronized (stateListeners) {
            stateListeners.remove (listener);
            snStateListeners = stateListeners.toArray(snStateListeners);
        }
    }

    @Override
    public void                 changed(DXTickStream stream, int property) {
        firePropertyChanged(stream, property);
    }


    @Override
    public void                 writerCreated(DXTickStream stream, IdentityKey[] ids) {
        StreamStateListener[] changeListeners = snStateListeners;

        for (int i = 0; i < changeListeners.length; i++) {
            StreamStateListener listener = changeListeners[i];
            if (listener != null)
                listener.writerCreated(stream, ids);
        }
    }

    @Override
    public void writerClosed(DXTickStream stream, IdentityKey[] ids) {
        StreamStateListener[] changeListeners = snStateListeners;

        for (int i = 0; i < changeListeners.length; i++) {
            StreamStateListener listener = changeListeners[i];
            if (listener != null)
                listener.writerClosed(stream, ids);
        }
    }

    void                 fireRenamed(DXTickStream stream, String oldKey) {
        StreamStateListener[] changeListeners = snStateListeners;

        for (int i = 0; i < changeListeners.length; i++) {
            StreamStateListener listener = changeListeners[i];
            if (listener != null)
                listener.renamed(stream, oldKey);
        }
    }

    void                 fireCreated(DXTickStream stream) {
        StreamStateListener[] changeListeners = snStateListeners;

        for (int i = 0; i < changeListeners.length; i++) {
            StreamStateListener listener = changeListeners[i];
            if (listener != null)
                listener.created(stream);
        }
    }

    void                 fireDeleted(DXTickStream stream) {
        StreamStateListener[] changeListeners = snStateListeners;

        for (int i = 0; i < changeListeners.length; i++) {
            StreamStateListener listener = changeListeners[i];
            if (listener != null)
                listener.deleted(stream);
        }
    }

    public void         firePropertyChanged(DXTickStream stream, int property) {
        StreamStateListener[] changeListeners = snStateListeners;

        for (int i = 0, len = changeListeners.length; i < len; i++) {
            StreamStateListener listener = changeListeners[i];
            if (listener != null)
                listener.changed(stream, property);
        }
    }

    @Override
    public void         created(DXTickStream stream) {

    }

    @Override
    public void         deleted(DXTickStream stream) {

    }

    @Override
    public void                 renamed(DXTickStream stream, String key) {
    }

    public void                 addPropertyMonitor(String component, PropertyMonitor listener) {
        if ("RAMDisk".equals(component))
            ramdisk.addPropertyMonitor(listener);
    }

    @Override
    public TopicDB getTopicDB() {
        throw new UnsupportedOperationException("Topics are not supported on the internal TickDB level.");
    }

    @Override
    public boolean isTopicDBSupported() {
        return false;
    }

    void syncRemoteStreams() {
        remoteStreamSyncChecker.syncChangesNow();
    }

    void notifyRemoteStreamDeleted(String key) {
        remoteStreamSyncChecker.writeChangeToLogStreamDelete(key);
    }

    void notifyRemoteStreamRenamed(String oldKey, String newKey) {
        remoteStreamSyncChecker.writeChangeToLogStreamRename(oldKey, newKey);
    }

    void notifyRemoteStreamUpdated(String key, EnumSet<TickStreamPropertiesEnum> changes) {
        remoteStreamSyncChecker.writeChangeToLogStreamUpdate(key, changes);
    }

    interface RemoteStreamLock extends Closeable {
        void releaseSilent();

        @Override
        default void close() {
            releaseSilent();
        }
    }

    /**
     * Wrapper for remote stream lock.
     */
    static final class RemoteStreamLockImpl implements RemoteStreamLock {
        private final String streamKey;
        private final AbstractPath streamMetadataFile;
        private final FsLock lock;

        public RemoteStreamLockImpl(String streamKey, AbstractPath streamMetadataFile, FsLock lock) {
            this.streamKey = streamKey;
            this.streamMetadataFile = streamMetadataFile;
            this.lock = lock;
        }

        public String getStreamKey() {
            return streamKey;
        }

        public AbstractPath getStreamMetadataFile() {
            return streamMetadataFile;
        }

        public FsLock getLock() {
            return lock;
        }

        public void releaseSilent() {
            AtomicFsLockManager.releaseSilent(lock);
        }
    }

    /**
     * Stub of lock that can be used by local streams.
     */
    static final class RemoteStreamLockStub implements RemoteStreamLock {
        public static final RemoteStreamLockStub INSTANCE = new RemoteStreamLockStub();

        private RemoteStreamLockStub() {}

        @Override
        public void releaseSilent() {
            // Do nothing
        }
    }
}
