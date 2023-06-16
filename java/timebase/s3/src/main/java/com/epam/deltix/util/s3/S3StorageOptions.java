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

public class S3StorageOptions {
    private String bucket;
    private String region;
    private String accessKeyId;
    private String accessKey;

    private int maxBatchSize = 0;
    private long maxBatchTime = 0;

    public S3StorageOptions(String bucket, String region, String accessKeyId, String accessKey) {
        assert bucket != null && region != null;
        this.bucket = bucket;
        this.region = region;
        this.accessKeyId = accessKeyId;
        this.accessKey = accessKey;
    }

    public String getBucket() {
        return bucket;
    }

    public String getRegion() {
        return region;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    public void setMaxBatchSize(int maxBatchSize) {
        if (maxBatchSize < 0)
            throw new IllegalArgumentException("Negative Max Batch Size value");
        this.maxBatchSize = maxBatchSize;
    }

    public long getMaxBatchTime() {
        return maxBatchTime;
    }

    public void setMaxBatchTime(long maxBatchTime) {
        if (maxBatchTime < 0)
            throw new IllegalArgumentException("Negative Max Batch Time value");
        this.maxBatchTime = maxBatchTime;
    }
}