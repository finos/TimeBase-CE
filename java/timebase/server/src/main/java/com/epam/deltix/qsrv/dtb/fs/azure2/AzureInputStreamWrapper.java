package com.epam.deltix.qsrv.dtb.fs.azure2;

import com.microsoft.azure.datalake.store.ADLStoreClient;
import com.microsoft.azure.datalake.store.Core;
import com.microsoft.azure.datalake.store.OperationResponse;
import com.microsoft.azure.datalake.store.RequestOptions;
import com.microsoft.azure.datalake.store.retrypolicies.ExponentialBackoffPolicy;
import com.epam.deltix.util.concurrent.UncheckedInterruptedException;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wraps {@link InputStream} provided by Azure to re-open it in case of any network issues.
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
class AzureInputStreamWrapper extends InputStream {
    private static final int MAX_RETRIES = Integer.getInteger(Azure2FS.AZURE_MAX_READ_RETRY_PROP, 5);
    private static final int TIME_TO_WAIT_AFTER_EXCEPTION = Integer.getInteger(Azure2FS.AZURE_TIME_TO_WAIT_AFTER_IOE_PROP, 500);
    private final String path;
    private final String sessionId;
    private final ADLStoreClient adlsFSClient;

    private long offset;
    private long limit;
    private int failureCount = 0;

    private InputStream wrapped = null;

    /**
     * @param offset start position
     * @param bytesToRead Number of bytes to read. Use -1, if not known.
     */
    public AzureInputStreamWrapper(String path, long offset, long bytesToRead, String sessionId, ADLStoreClient adlsFSClient) {
        this.path = path;
        this.offset = offset;
        this.sessionId = sessionId;
        this.adlsFSClient = adlsFSClient;
        if (bytesToRead > 0) {
            this.limit = offset + bytesToRead;
        } else {
            this.limit = -1;
        }
    }

    public void init() {
        if (wrapped != null) {
            throw new IllegalStateException();
        }
        crateAzureStream();
    }

    /**
     * Re-creates azure stream from current offset.
     */
    private void crateAzureStream() {
        RequestOptions opts = new RequestOptions();
        opts.retryPolicy = new ExponentialBackoffPolicy();
        opts.timeout = Azure2FS.CONNECT_AND_READ_TIMEOUT;
        OperationResponse resp = new OperationResponse();
        long length;
        if (limit >= 0) {
            length = limit - offset;
        } else {
            length = 0;
        }
        // Result may be null if path does not exist
        wrapped = Core.open(path, offset, length, sessionId, adlsFSClient, opts, resp);
    }

    @Nullable
    public InputStream getWrappedStream() {
        return wrapped;
    }

    @Override
    public int read() throws IOException {
        while (true){
            try {
                int value = wrapped.read();
                if (value >= 0) {
                    offset += 1;
                }
                return value;
            } catch (IOException e) {
                handleIOException(e);
            }
        }
    }

    @Override
    public int read(@NotNull byte[] b, int off, int len) throws IOException {
        while (true) {
            try {
                int bytesRead = wrapped.read(b, off, len);
                if (bytesRead > 0) {
                    offset += bytesRead;
                }
                return bytesRead;
            } catch (IOException e) {
                handleIOException(e);
            }
        }
    }

    @Override
    public long skip(long n) throws IOException {
        // TODO: In case of IOException inside .skip() we might jump right to the desired position. Not implemented yet.
        while (true) {
            try {
                long bytesSkipped = wrapped.skip(n);
                if (bytesSkipped > 0) {
                    offset += bytesSkipped;
                }
                return bytesSkipped;
            } catch (IOException e) {
                handleIOException(e);
            }
        }
    }

    private void handleIOException(IOException e) throws IOException {
        failureCount += 1;
        if (failureCount < MAX_RETRIES) {
            try {
                Thread.sleep(TIME_TO_WAIT_AFTER_EXCEPTION);
            } catch (InterruptedException ie) {
                throw new UncheckedInterruptedException(ie);
            }
            crateAzureStream();
        } else {
            throw e;
        }
    }

    @Override
    public int available() throws IOException {
        return wrapped.available();
    }

    @Override
    public void close() throws IOException {
        wrapped.close();
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void mark(int readlimit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new UnsupportedOperationException();
    }
}
