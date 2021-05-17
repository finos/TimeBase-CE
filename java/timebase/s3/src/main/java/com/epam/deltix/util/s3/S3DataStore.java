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
package com.epam.deltix.util.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.util.collections.Visitor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * S3DataStore - used for uploading data files to S3 store
 */
public class S3DataStore {
    public static final String KEY_DELIMITER = "/";

    protected static final Log LOG = LogFactory.getLog(S3DataStore.class);

    private String bucket;
    private int maxBatchSize;
    private long maxBatchTime;

    private AmazonS3 s3Client;
    private TransferManager transferManager;

    public S3DataStore(S3StorageOptions options) {
        this.bucket = options.getBucket();
        this.maxBatchSize = options.getMaxBatchSize();
        this.maxBatchTime = options.getMaxBatchTime();
        this.s3Client = getS3Client(options);
        this.transferManager = TransferManagerBuilder.standard().withS3Client(s3Client).build();
    }

    public S3Writer<String> createWriter(String dataKey) {
        return new S3GzipWriter(this, dataKey, maxBatchSize, maxBatchTime);
    }

    public S3Reader<String> createReader(String dataKey) throws IOException {
        return new S3GzipReader(this, dataKey);
    }

    public void upload(String keyName, InputStream dataIn, Map<String,String> metadata) throws IOException, InterruptedException {
        long beforeTime = System.currentTimeMillis();
        int contentLength = dataIn.available();

        for (int attemptsCount = 0; attemptsCount < 2; attemptsCount++) {
            if (attemptsCount > 0) {
                LOG.warn("Retrying %s upload").with(keyName);
                dataIn.reset();
            }
            ObjectMetadata objMetadata = new ObjectMetadata();
            objMetadata.setContentLength(contentLength);
            if (metadata != null)
                objMetadata.setUserMetadata(metadata);

            Upload upload = transferManager.upload(bucket, keyName, dataIn, objMetadata);
            try {
                upload.waitForCompletion();
                break;
            }
            catch (AmazonClientException ex) {
                LOG.warn("Upload failed with error: %s").with(ex.getMessage());
                if (attemptsCount > 0) throw ex;
            }
        }

        long afterTime = System.currentTimeMillis();
        LOG.trace("Uploaded %s Mb in %s sec to %s").with(contentLength/1000/1000.0).with((afterTime-beforeTime)/1000.0).with(keyName);
    }

    // downloads object to output stream and returns its user metadata
    public Map<String,String> download(String keyName, OutputStream out) throws IOException {
        S3Object obj = s3Client.getObject(bucket, keyName);
        try (S3ObjectInputStream objStream = obj.getObjectContent()) {
            byte[] read_buf = new byte[1024];
            int read_len = 0;
            while ((read_len = objStream.read(read_buf)) > 0) {
                out.write(read_buf, 0, read_len);
            }
        }
        finally {
            out.close();
        }
        return obj.getObjectMetadata().getUserMetadata();
    }

    // creates or updates an empty object with value stored in its metadata
    public void setMetadata(String dataKey, String key, String value) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(0);
        metadata.addUserMetadata(key, value);
        InputStream emptyContent = new ByteArrayInputStream(new byte[0]);

        PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, dataKey, emptyContent, metadata);
        s3Client.putObject(putObjectRequest);

        /*
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.addUserMetadata(metadataKey, value);
        CopyObjectRequest request = new CopyObjectRequest(bucket, objectKey, bucket, objectKey);
        request.setNewObjectMetadata(metadata);
        s3Client.copyObject(request);
         */
    }

    public boolean objectExists(String dataKey) {
        return s3Client.doesObjectExist(bucket, dataKey);
    }

    // returns
    public Map<String,String> getMetadata(String dataKey) {
        if (!s3Client.doesObjectExist(bucket, dataKey))
            return null;

        ObjectMetadata metadata = s3Client.getObjectMetadata(bucket, dataKey);
        return metadata.getUserMetadata();
    }

    public List<String> getObjectKeys(String keyPrefix, String keySuffix) {
        ArrayList<String> keys = new ArrayList<>();
        ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucket).withMaxKeys(1000).withPrefix(keyPrefix);
        ListObjectsV2Result result;
        do {
            result = s3Client.listObjectsV2(req);
            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                String key = objectSummary.getKey();
                if (keySuffix != null && key.endsWith(keySuffix))
                    keys.add(objectSummary.getKey());
            }
            req.setContinuationToken(result.getNextContinuationToken());
        } while (result.isTruncated());

        return keys;
    }

    /**
     * @param dataKey S3 Key that corresponds to the folder containing partitions
     * @return List of keys that correspond to S3 top level partition folders
     */
    public Set<String> getPartitions(String dataKey, String extension) {
        assert !dataKey.endsWith(KEY_DELIMITER);
        String keyPrefix = dataKey + KEY_DELIMITER;

        Set<String> keys = new HashSet<>();
        ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucket).withMaxKeys(1000).withPrefix(keyPrefix);
        ListObjectsV2Result result;
        do {
            result = s3Client.listObjectsV2(req);
            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                String key = objectSummary.getKey();
                if (key.endsWith(extension)) {
                    int index = key.indexOf(KEY_DELIMITER, keyPrefix.length());
                    if (index > 0)
                        keys.add(key.substring(0, index));
                }
            }
            req.setContinuationToken(result.getNextContinuationToken());
        } while (result.isTruncated());

        return keys;
    }

    public void visitObjectKeys(String keyPrefix, Visitor<String> visitor) {
        ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucket).withMaxKeys(1000).withPrefix(keyPrefix);
        ListObjectsV2Result result;
        do {
            result = s3Client.listObjectsV2(req);

            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                String key = objectSummary.getKey();
                if (!visitor.visit(key))
                    return;
            }
            req.setContinuationToken(result.getNextContinuationToken());
        } while (result.isTruncated());
    }

    private static AmazonS3 getS3Client(S3StorageOptions options) {
        try {
            BasicAWSCredentials credentials = new BasicAWSCredentials(options.getAccessKeyId(), options.getAccessKey());
            return AmazonS3ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .withRegion(options.getRegion())
                    .build();
        }
        catch (AmazonServiceException ex) {
            throw new RuntimeException("Failed to login to AWS", ex);
        }
        catch (SdkClientException ex) {
            throw new RuntimeException("Failed to connect to AWS", ex);
        }
    }
}
