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
package com.epam.deltix.qsrv.dtb.fs.azure;

import com.microsoft.azure.management.datalake.store.models.*;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.gflog.api.LogLevel;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractFileSystem;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.qsrv.dtb.fs.pub.FSFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class AzurePathImpl implements AbstractPath {

    private static final Log LOG = LogFactory.getLog(AzurePathImpl.class);

    private static final String     AZURE_PROTOCOL_PREFIX = FSFactory.AZURE_PROTOCOL_ID + FSFactory.SCHEME_SEPARATOR;

    private final AzureFS           fileSystem;
    private final String            path;
    private final String            extractedPath;

    private FileStatusProperties    fileStatus = null;

    AzurePathImpl(String path, AzureFS fs) {
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
        if ((index = path.lastIndexOf(AzureFS.AZURE_PATH_SEPARATOR)) < 0)
            return path;

        return path.substring(index + 1);
    }

    @Override
    public String[]                     listFolder() throws IOException {
        try {
            List<FileStatusProperties> statuses =
                    fileSystem.adlsFSClient
                            .fileSystems()
                            .listFileStatus(fileSystem.accountName, extractedPath)
                            .getBody()
                            .fileStatuses()
                            .fileStatus();

            String[] items = new String[statuses.size()];
            for (int i = 0; i < statuses.size(); ++i)
                items[i] = statuses.get(i).pathSuffix();
            Arrays.sort(items);

            return items;
        } catch (AdlsErrorException e) {
            throw new IOException(e);
        }
    }

    @Override
    public AbstractPath                 append(String name) {
        return new AzurePathImpl(unitePath(path, name), fileSystem);
    }

    @Override
    public AbstractPath                 getParentPath() {
        int index;
        if ((index = extractedPath.lastIndexOf(AzureFS.AZURE_PATH_SEPARATOR)) <= 0) //not found '/', than return root
            return new AzurePathImpl(AZURE_PROTOCOL_PREFIX + AzureFS.AZURE_PATH_SEPARATOR, fileSystem);

        return new AzurePathImpl(AZURE_PROTOCOL_PREFIX + extractedPath.substring(0, index), fileSystem);
    }

    @Override
    public InputStream                  openInput(long offset) throws IOException {
        try {
            LOG.log(LogLevel.DEBUG).append("Opening file \"").append(extractedPath).append("\" at offset ").append(offset).commit();
            return fileSystem.adlsFSClient.fileSystems().open(fileSystem.accountName, extractedPath, null, offset).getBody();
        } catch (AdlsErrorException e) {
            throw new IOException(e);
        }
    }

    @Override
    public OutputStream                 openOutput(long size) throws IOException {
        deleteIfExists();
        createNewFile();

        return new OutputStream() {

            private static final int            CHUNK_SIZE = 5 * (1 << 20);

            @Override
            public void write(int b) throws IOException {
                throw new UnsupportedOperationException("Write byte method is not supported.");
            }

            @Override
            public void write(byte b[]) throws IOException {
                try {
                    fileSystem.adlsFSClient.fileSystems().append(fileSystem.accountName, extractedPath, b);
                } catch (AdlsErrorException e) {
                    throw new IOException(e);
                }
            }

            @Override
            public void write(byte b[], int off, int len) throws IOException {
                if (b == null) {
                    throw new NullPointerException();
                } else if ((off < 0) || (off > b.length) || (len < 0) ||
                        ((off + len) > b.length) || ((off + len) < 0)) {
                    throw new IndexOutOfBoundsException();
                } else if (len == 0) {
                    return;
                }

                int offset = off;
                int length = Math.min(len, CHUNK_SIZE);
                while (offset < len) {
                    write(Arrays.copyOfRange(b, offset, Math.min(off + len, offset + length)));
                    offset += length;
                }
            }
        };
    }

    @Override
    public long                         length() {
        try {
            if (fileStatus == null)
                fileStatus = getFileStatus();

            return length(fileStatus, extractedPath);
        } catch (AdlsErrorException | IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }
    }

    private long                        length(FileStatusProperties status, String path) throws IOException, AdlsErrorException {
        if (status.type() == FileType.DIRECTORY) {
            List<FileStatusProperties> statuses =
                    fileSystem.adlsFSClient
                            .fileSystems()
                            .listFileStatus(fileSystem.accountName, path)
                            .getBody()
                            .fileStatuses()
                            .fileStatus();

            long length = 0;
            for (FileStatusProperties curStatus : statuses)
                length += length(curStatus, unitePath(path, curStatus.pathSuffix()));

            return length;
        }

        return status.length();
    }

    @Override
    public boolean                      isFile() {
        try {
            if (fileStatus == null)
                fileStatus = getFileStatus();

            return fileStatus.type() == FileType.FILE;
        } catch (AdlsErrorException e) {
            if (e.getResponse().code() == 404)
                return false;

            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }
    }

    @Override
    public boolean                      isFolder() {
        try {
            if (fileStatus == null)
                fileStatus = getFileStatus();

            return fileStatus.type() == FileType.DIRECTORY;
        } catch (AdlsErrorException e) {
            if (e.getResponse().code() == 404)
                return false;

            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }
    }

    @Override
    public boolean                      exists() {
        try {
            fileStatus = getFileStatus();
            return true;
        } catch (AdlsErrorException | IOException e) {
            // TODO: Probably It's a mistake to consider any IO exception as missing file
            return false;
        }
    }

    @Override
    public void                         makeFolder() throws IOException {
        makeFolderRecursive();
    }

    @Override
    public void                         makeFolderRecursive() throws IOException {
        try {
            fileSystem.adlsFSClient.fileSystems().mkdirs(fileSystem.accountName, extractedPath);
        } catch (AdlsErrorException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void                         moveTo(AbstractPath newPath) throws IOException {
        try {
            fileStatus = null;

            FileOperationResult result = fileSystem.adlsFSClient
                    .fileSystems()
                    .rename(fileSystem.accountName, extractedPath, extractPath(newPath.getPathString()))
                    .getBody();

            if (!result.operationResult())
                throw new IOException("Failed to rename " + this + " --> " + newPath);
        } catch (AdlsErrorException e) {
            throw new IOException(e);
        }
    }

    @Override
    public AbstractPath                 renameTo(String newName) throws IOException {
        try {
            fileStatus = null;

            String newPath = unitePath(getParentPath().getPathString(), newName);
            FileOperationResult result = fileSystem.adlsFSClient
                    .fileSystems()
                    .rename(fileSystem.accountName, extractedPath, extractPath(newPath))
                    .getBody();

            if (!result.operationResult())
                throw new IOException("Failed to rename " + this + " --> " + newName);

            return new AzurePathImpl(newPath, fileSystem);
        } catch (AdlsErrorException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void                         deleteExisting() throws IOException {
        try {
            fileStatus = null;

            FileOperationResult result = fileSystem.adlsFSClient
                    .fileSystems()
                    .delete(fileSystem.accountName, extractedPath, true)
                    .getBody();

            if (!result.operationResult())
                throw new IOException("Failed to delete " + this);
        } catch (AdlsErrorException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void                         deleteIfExists() throws IOException {
        fileStatus = null;

        if (exists())
            deleteExisting();
    }

    private FileStatusProperties        getFileStatus() throws IOException, AdlsErrorException {
        return fileSystem.adlsFSClient
                .fileSystems()
                .getFileStatus(fileSystem.accountName, extractedPath)
                .getBody()
                .fileStatus();
    }

    private void                        createNewFile() throws IOException {
        try {
            fileSystem.adlsFSClient.fileSystems().create(fileSystem.accountName, extractedPath);
        } catch (AdlsErrorException e) {
            throw new IOException(e);
        }
    }

    private String                      extractPath(String uri) {
        if (uri.startsWith(AZURE_PROTOCOL_PREFIX))
            return uri.substring(AZURE_PROTOCOL_PREFIX.length());

        return uri;
    }

    private String                      unitePath(String parent, String child) {
        return parent + AzureFS.AZURE_PATH_SEPARATOR + child;
    }

    @Override
    public String                       toString() {
        return path;
    }
}
