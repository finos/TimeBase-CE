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
package com.epam.deltix.qsrv.hf.topic.consumer;

import io.aeron.AvailableImageHandler;
import io.aeron.Image;
import io.aeron.LogBuffers;
import io.aeron.UnavailableImageHandler;
import io.aeron.logbuffer.LogBufferDescriptor;
import org.agrona.collections.ArrayUtil;
import org.agrona.concurrent.UnsafeBuffer;

import java.lang.reflect.Field;

import static io.aeron.logbuffer.LogBufferDescriptor.*;

/**
 * Tracks Aeron subscription {@link Image}s to provide information on fill percentage of incoming message buffer.
 *
 * Implementation detail: {@link #onAvailableImage(Image)} and {@link #onUnavailableImage(Image)}
 * are always called by client conductor thread.
 *
 * @author Alexei Osipov
 */
class IpcFilPercentageChecker implements AvailableImageHandler, UnavailableImageHandler {
    private static final ImageData[] EMPTY_ARRAY = new ImageData[0];

    private volatile ImageData[] imageData = EMPTY_ARRAY;

    @Override
    public void onAvailableImage(Image image) {
        imageData = ArrayUtil.add(imageData, new ImageData(image));
    }

    @Override
    public void onUnavailableImage(Image image) {
        ImageData[] oldArray = imageData;
        ImageData removedImageData = null;

        for (ImageData imageData : oldArray) {
            if (imageData.image == image) {
                removedImageData = imageData;
                break;
            }
        }

        if (null != removedImageData) {
            imageData = ArrayUtil.remove(oldArray, removedImageData);
        }
    }

    /**
     * Return fill percentage of incoming data buffer.
     *
     * If there are multiple buffers (as in case of multiple publications) then returns maximum value of all.
     *
     * @return value in range [0..100]
     */
    byte getBufferFillPercentage() {
        ImageData[] imageData = this.imageData;

        // We start as 0 (empty) for estimate and check all images for the publication.
        // We will return the maximum fill value we wind among all images.
        // If we don't have data for certain image then we skip/ignore it.
        byte maxFill = 0;
        for (ImageData data : imageData) {
            Image image = data.image;

            long publicationPosition = getPublisherPositionForImage(data.logBuffers, data.initialTermId);

            long currentPosition = image.position();
            if (currentPosition > publicationPosition) {
                // Invalid state: reader at a position beyond published data
                // Abort computations for this image
                // TODO: Log a warning here?
                continue;
            }

            int bufferLength = image.termBufferLength();
            long dataInBuffer = publicationPosition - currentPosition;
            assert dataInBuffer >= 0;
            if (dataInBuffer > bufferLength) {
                // Inconsistent state: the amount of unprocessed data is higher than buffer size
                // This state
                // TODO: Log a warning here?
                return 100;
            }

            // TODO: Division can be replaced by binary shift because divisor is always a power of two
            long imageBufferFillPercentage = dataInBuffer * 100 / bufferLength;
            assert 0 <= imageBufferFillPercentage && imageBufferFillPercentage <= 100;

            byte valueAsByte = (byte) imageBufferFillPercentage;
            if (valueAsByte > maxFill) {
                maxFill = valueAsByte;
            }
        }

        return maxFill;
    }

    /**
     * Releases acquired resources.
     * It's not permitted to use this object after the method call.
     */
    void releaseResources() {
        imageData = EMPTY_ARRAY;
    }

    private static final class ImageData {
        final Image image;
        final LogBuffers logBuffers;
        final int initialTermId;

        ImageData(Image image) {
            this.image = image;
            this.logBuffers = getLogBuffersUsingReflection(image);
            this.initialTermId = LogBufferDescriptor.initialTermId(logBuffers.metaDataBuffer());
        }
    }

    private static LogBuffers getLogBuffersUsingReflection(Image image) {
        Field f;
        try {
            f = Image.class.getDeclaredField("logBuffers");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        f.setAccessible(true);
        try {
            return (LogBuffers) f.get(image);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static long getPublisherPositionForImage(LogBuffers logBuffers, int initialTermId) {
        UnsafeBuffer logMetaDataBuffer = logBuffers.metaDataBuffer();
        int termLength = logBuffers.termLength();
        final long rawTail = rawTailVolatile(logMetaDataBuffer);
        final int termOffset = termOffset(rawTail, termLength);
        int positionBitsToShift = Integer.numberOfTrailingZeros(termLength);
        return computePosition(termId(rawTail), termOffset, positionBitsToShift, initialTermId);
    }
}