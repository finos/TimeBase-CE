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
package com.epam.deltix.qsrv.dtb.store.codecs;

import com.epam.deltix.qsrv.dtb.fs.alloc.ByteArrayHeap;
import com.epam.deltix.qsrv.dtb.fs.alloc.HeapManager;
import com.epam.deltix.util.collections.generated.ByteArrayList;

import java.util.StringTokenizer;

/**
 *
 */
public class BlockCompressorFactory {

    public static byte              getCode(Algorithm algorithm) {
        switch (algorithm) {
            case LZ4:
                return 2;
            case SNAPPY:
                return 1;
            case ZLIB:
                return 0;

            default:
                throw new IllegalArgumentException("Unknown compression algorithm");
        }
    }

    public static Algorithm              getAlgorithm(byte code) {
        switch (code) {
            case 2:
                return Algorithm.LZ4;
            case 1:
                return Algorithm.SNAPPY;
            case 0:
                return Algorithm.ZLIB;

            default:
                throw new IllegalArgumentException("Unknown compression algorithm: " + code);
        }
    }

    public static BlockCompressor createCompressor(String compression, ByteArrayList buffer) {
        if (compression == null)
            throw new NullPointerException("Compression can't be null.");

        StringTokenizer tokenizer = new StringTokenizer(compression, "()");
        Algorithm algorithm = getCompressionType(tokenizer);

        switch (algorithm) {
            case LZ4:
                return new LZ4BlockCompressor(getCompressionLevel(tokenizer), buffer);
            case ZLIB:
                return new DeflateCompressor(getCompressionLevel(tokenizer), buffer);
            case SNAPPY:
                return new SnappyCompressor(buffer);

            default:
                throw new IllegalArgumentException("Unknown compression algorithm: " + algorithm);
        }
    }

    public static BlockDecompressor createDecompressor(byte code) {

        Algorithm algorithm = getAlgorithm(code);

        switch (algorithm) {
            case LZ4:
                return new LZ4BlockDecompressor();
            case ZLIB:
                return new DeflateDecompressor();
            case SNAPPY:
                return new SnappyDecompressor();

            default:
                throw new IllegalArgumentException("Unknown decompressor code: " + code);
        }
    }

    private static Algorithm getCompressionType(StringTokenizer tokenizer) {
        try {
            if (tokenizer.hasMoreTokens())
                return Algorithm.valueOf(tokenizer.nextToken().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }

        return null;
    }

    private static int getCompressionLevel(StringTokenizer tokenizer) {
        if (tokenizer.hasMoreTokens()) {
            String level = tokenizer.nextToken().trim();
            try {
                return Integer.parseInt(level);
            } catch(NumberFormatException e) {
                return 1;
            } catch(NullPointerException e) {
                return 1;
            }
        }

        return 1;
    }
}
