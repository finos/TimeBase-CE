package com.epam.deltix.qsrv.dtb.fs.azure2;

import com.microsoft.azure.datalake.store.*;
import com.epam.deltix.gflog.LogLevel;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractFileSystem;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.qsrv.dtb.fs.pub.FSFactory;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.epam.deltix.qsrv.dtb.fs.azure2.Azure2FS.LOG;

/**
 *
 */
public class Azure2PathImpl implements AbstractPath {

    private static final String     AZURE_PROTOCOL_PREFIX = FSFactory.AZURE_PROTOCOL_ID + FSFactory.SCHEME_SEPARATOR;

    private final Azure2FS fileSystem;
    private final String path;
    private final String extractedPath; // Path without prefix in the format expected by Azure.

    private Boolean exists = null; // Null value mean we don't know file/folder status
    private DirectoryEntry directoryEntry = null; // Null value mean we don't know file/folder status OR file just doesn't exist

    private String sessionId = null; // SessionID for Azure DL requests.

    private boolean canCacheMetadata = true;

    Azure2PathImpl(String path, Azure2FS fs) {
        this.path = path;
        this.fileSystem = fs;
        this.extractedPath = extractPath(path);
    }

    @Override
    public AbstractFileSystem           getFileSystem() {
        return fileSystem;
    }

    @Override
    public String                       getPathString() {
        return path;
    }

    @Override
    public String                       getName() {
        int index;
        if ((index = path.lastIndexOf(Azure2FS.AZURE_PATH_SEPARATOR)) < 0)
            return path;

        return path.substring(index + 1);
    }

    @Override
    public String[]                     listFolder() throws IOException {
        try {
            List<DirectoryEntry> entries = fileSystem.adlsFSClient.enumerateDirectory(extractedPath);

            String[] items = new String[entries.size()];
            for (int i = 0; i < entries.size(); ++i)
                items[i] = entries.get(i).name;
            Arrays.sort(items);

            return items;
        } catch (ADLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public AbstractPath                 append(String name) {
        return new Azure2PathImpl(unitePath(path, name), fileSystem);
    }

    @Override
    public AbstractPath                 getParentPath() {
        int index;
        if ((index = extractedPath.lastIndexOf(Azure2FS.AZURE_PATH_SEPARATOR)) <= 0) //not found '/', than return root
            return new Azure2PathImpl(AZURE_PROTOCOL_PREFIX + Azure2FS.AZURE_PATH_SEPARATOR, fileSystem);

        return new Azure2PathImpl(AZURE_PROTOCOL_PREFIX + extractedPath.substring(0, index), fileSystem);
    }

    @Override
    public InputStream                  openInput(long offset) throws IOException {
        if (LOG.isEnabled(LogLevel.DEBUG)) {
            LOG.log(LogLevel.DEBUG).append("Opening file \"").append(extractedPath).append("\" at offset ").append(offset).append(" from ").append(Thread.currentThread().getName()).commit();
        }
        AzureInputStreamWrapper streamWrapper = new AzureInputStreamWrapper(extractedPath, offset, -1, getSessionId(), fileSystem.adlsFSClient);
        streamWrapper.init();
        InputStream is = streamWrapper.getWrappedStream();
        if (is == null) {
            // Try to find error reason
            invalidateStatus();
            if (!exists()) {
                throw new FileNotFoundException("Not exist");
            } else if (!isFile()) {
                throw new IOException("Not a file");
            } else {
                throw new IOException("Failed to open stream");
            }
        }
        return streamWrapper;
    }

    @Override
    public InputStream openInput(long offset, long length) throws IOException {
        if (LOG.isEnabled(LogLevel.DEBUG)) {
            LOG.log(LogLevel.DEBUG).
                    append("Opening file \"").append(extractedPath).append("\" at offset ").append(offset).
                    append(" with length limit ").
                    append(length).append(" from ").
                    append(Thread.currentThread().getName()).commit();
        }
        AzureInputStreamWrapper streamWrapper = new AzureInputStreamWrapper(extractedPath, offset, length, getSessionId(), fileSystem.adlsFSClient);
        streamWrapper.init();
        InputStream is = streamWrapper.getWrappedStream();
        if (is == null) {
            // Try to find error reason
            invalidateStatus();
            if (!exists()) {
                throw new FileNotFoundException("Not exist");
            } else if (!isFile()) {
                throw new IOException("Not a file");
            } else {
                throw new IOException("Failed to open stream");
            }
        }
        return streamWrapper;
    }

    @Override
    public OutputStream                 openOutput(long size) throws IOException {
        invalidateStatus();

        if (LOG.isEnabled(LogLevel.DEBUG)) {
            LOG.log(LogLevel.DEBUG).append("Opening output file \"")
                    .append(extractedPath).append("\"  ").append(" from ")
                    .append(Thread.currentThread().getName()).commit();
        }

        return fileSystem.adlsFSClient.createFile(extractedPath, IfExists.OVERWRITE);
    }

    @Override
    public OutputStream openOutputForAppend() throws IOException {
        invalidateStatus();

        try {
            return fileSystem.adlsFSClient.getAppendStream(extractedPath);
        } catch (ADLException ex) {
            throw getExceptionFromAzureError(ex);
        }
    }

    @Override
    public long                         length() {
        try {
            DirectoryEntry info = getInfo();
            if (info == null) {
                throw new IOException("File not found");
            }
            return getSizeRecursive(info);
        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }
    }

    /**
     * @return size of file for files and total size of contained files for folders
     */
    private long getSizeRecursive(DirectoryEntry status) throws IOException {
        if (status.type == DirectoryEntryType.DIRECTORY) {
            List<DirectoryEntry> childEntries = fileSystem.adlsFSClient.enumerateDirectory(status.fullName);

            long length = 0;
            for (DirectoryEntry childEntry : childEntries) {
                length += getSizeRecursive(childEntry);
            }

            return length;
        } else {
            // This is file
            return status.length;
        }
    }

    @Override
    public boolean                      isFile() {
        try {
            DirectoryEntry info = getInfo();
            return info != null && info.type == DirectoryEntryType.FILE;
        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }
    }

    @Override
    public boolean                      isFolder() {
        try {
            DirectoryEntry info = getInfo();
            return info != null && info.type == DirectoryEntryType.DIRECTORY;
        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }
    }

    @Override
    public boolean                      exists() {
        try {
            return getInfo() != null;
        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }
    }

    /**
     * Same as {@link #exists()} but ignored saved cached value.
     */
    public boolean                      existsIgnoreCached() {
        try {
            return getInfo(false) != null;
        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }
    }

    @Override
    public void                         makeFolder() throws IOException {
        makeFolderRecursive();
    }

    @Override
    public void                         makeFolderRecursive() throws IOException {
        try {
            fileSystem.adlsFSClient.createDirectory(extractedPath);
        } catch (ADLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void                         moveTo(AbstractPath newPath) throws IOException {
        try {
            invalidateStatus();

            String extractedNewPath = extractPath(newPath.getPathString());
            if (!fileSystem.adlsFSClient.rename(extractedPath, extractedNewPath)) {
                throw new IOException("Failed to rename " + this + " --> " + newPath);
            }

            if (LOG.isEnabled(LogLevel.DEBUG)) {
                LOG.debug("Renamed file %s to %s from %s")
                        .with(extractedPath)
                        .with(extractedNewPath)
                        .with(Thread.currentThread().getName());
            }
        } catch (ADLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public AbstractPath                 renameTo(String newName) throws IOException {
        try {
            invalidateStatus();

            String newPath = unitePath(getParentPath().getPathString(), newName);

            String extractedNewPath = extractPath(newPath);
            if (!fileSystem.adlsFSClient.rename(extractedPath, extractedNewPath)) {
                throw new IOException("Failed to rename " + this + " --> " + newName);
            }
            if (LOG.isEnabled(LogLevel.DEBUG)) {
                LOG.debug("Renamed file %s to %s from %s")
                        .with(extractedPath)
                        .with(extractedNewPath)
                        .with(Thread.currentThread().getName());
            }
            return new Azure2PathImpl(newPath, fileSystem);
        } catch (ADLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void                         deleteExisting() throws IOException {
        try {
            invalidateStatus();

            if (!fileSystem.adlsFSClient.deleteRecursive(extractedPath)) {
                throw new IOException("Failed to delete " + this);
            }
            if (LOG.isEnabled(LogLevel.DEBUG)) {
                LOG.debug("Deleted file %s from %s")
                        .with(extractedPath)
                        .with(Thread.currentThread().getName());
            }
        } catch (ADLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void                         deleteIfExists() throws IOException {
        if (exists()) {
            deleteExisting();
        }
    }

    @Override
    public long getModificationTime() throws IOException {
        DirectoryEntry info = getInfo(false);
        if (info == null) {
            throw new FileNotFoundException();
        }
        return info.lastModifiedTime.getTime();
    }

    @Override
    public void setModificationTime(long timestamp) throws IOException {
        try {
            fileSystem.adlsFSClient.setTimes(extractedPath, null, new Date(timestamp));
        } catch (ADLException ex) {
            throw getExceptionFromAzureError(ex);
        }
    }

    /**
     * Expires file after specified amount of time (relative to this call).
     */
    public void setExpireAfter(long expiryTimeMilliseconds) throws IOException {
        try {
            fileSystem.adlsFSClient.setExpiryTime(extractedPath, ExpiryOption.RelativeToNow, expiryTimeMilliseconds);
        } catch (ADLException ex) {
            throw getExceptionFromAzureError(ex);
        }
    }

    private IOException getExceptionFromAzureError(ADLException ex) {
        if (isNotFoundException(ex)) {
            return new FileNotFoundException();
        } else {
            return ex;
        }
    }

    private String                      extractPath(String uri) {
        if (uri.startsWith(AZURE_PROTOCOL_PREFIX))
            return uri.substring(AZURE_PROTOCOL_PREFIX.length());

        return uri;
    }

    private String                      unitePath(String parent, String child) {
        return parent + Azure2FS.AZURE_PATH_SEPARATOR + child;
    }

    @Override
    public String                       toString() {
        return path;
    }

    @Override
    public void setCacheMetadata(boolean cache) {
        this.canCacheMetadata = cache;
    }

    /**
     * @return file or folder info if it exists or {@code null} otherwise
     */
    @Nullable
    private DirectoryEntry getInfo() throws IOException {
        return getInfo(true);
    }

    private DirectoryEntry getInfo(boolean useCached) throws IOException {
        if (exists == null || !useCached || !canCacheMetadata) {
            try {
                directoryEntry = fileSystem.adlsFSClient.getDirectoryEntry(extractedPath);
                exists = true;
            } catch (ADLException ex) {
                if (isNotFoundException(ex)) {
                    // Not exist
                    exists = false;
                    directoryEntry = null;
                } else {
                    throw ex;
                }
            }
        }
        return directoryEntry;
    }

    private boolean isNotFoundException(ADLException ex) {
        return ex.httpResponseCode == 404;
    }

    public void invalidateStatus() {
        directoryEntry = null;
        exists = null;
    }

    private String getSessionId() {
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
        }
        return sessionId;
    }
}
