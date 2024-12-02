/*
 * Copyright 2024 EPAM Systems, Inc
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
package com.epam.deltix.util.security;

import com.epam.deltix.containers.BinaryArray;
import com.epam.deltix.containers.interfaces.BinaryArrayReadOnly;
import com.epam.deltix.util.lang.StringUtils;

public class EntitlementIDComposer<T extends EntitlementIDComposer> implements EntitlementID {
    protected final int     entriesOffset;

    protected BinaryArray   content;
    protected boolean       isNull;

    public EntitlementIDComposer() {
        this(0);
    }

    public EntitlementIDComposer(int entriesOffset) {
        this.entriesOffset = entriesOffset;
        reset();
    }

    @SuppressWarnings("unchecked")
    public T reset() {
        resetContent();
        setSafely(content, entriesOffset, (byte) 0);
        
        isNull = true;
        
        return (T) this; // unchecked
    }

    public T appendEntry(byte[] entry) {
        return appendEntry(entry, 0, entry.length);
    }

    @SuppressWarnings("unchecked")
    public T appendEntry(byte[] entry, int offset, int length) {
        setSafely(content, entriesOffset, (byte)(numberOfEntries() + 1));

        content.append((byte)length);
        content.append(entry, offset, length);

        isNull = false;

        return (T) this; // Unchecked
    }

    @SuppressWarnings("unchecked")
    public T appendAllEntries(T from) {
        setSafely(content, entriesOffset, (byte)(numberOfEntries() + from.numberOfEntries()));

        final int sizeToAdd = from.content.size() - from.entriesOffset - 1;

        for (int i = 1; i <= sizeToAdd; i++) {
            content.append(from.content.get(from.entriesOffset + i));
        }

        isNull = false;

        return (T) this; // Unchecked
    }

    @Override
    public boolean isNull() {
        return isNull;
    }

    @Override
    public BinaryArrayReadOnly content() {
        return content;
    }

    @Override
    public int numberOfEntries() {
        return content != null ? content.get(entriesOffset) & 0xFF : 0;
    }

    @Override
    public int entryOffset(int entryIndex) {
        if (entryIndex >= numberOfEntries())
            throw new IndexOutOfBoundsException();

        return getEntryContentIndex(entryIndex) + 1;
    }

    @Override
    public int entryLength(int entryIndex) {
        if (entryIndex >= numberOfEntries())
            throw new IndexOutOfBoundsException();

        return content.get(getEntryContentIndex(entryIndex)) & 0xFF;
    }

    protected void resetContent() {
        if (content != null) {
            content.clear();
        } else {
            content = new BinaryArray();
        }
    }

    private int getEntryContentIndex(int entryIndex) {
        int result = entriesOffset + 1;

        // Some unrolling
        switch (entryIndex) {
            case 0:
                break;
            case 1:
                result = result + (content.get(result) & 0xFF) + 1;
                break;
            case 2:
                result = result + (content.get(result) & 0xFF) + 1;
                result = result + (content.get(result) & 0xFF) + 1;
                break;
            case 3:
                result = result + (content.get(result) & 0xFF) + 1;
                result = result + (content.get(result) & 0xFF) + 1;
                result = result + (content.get(result) & 0xFF) + 1;
                break;
            case 4:
                result = result + (content.get(result) & 0xFF) + 1;
                result = result + (content.get(result) & 0xFF) + 1;
                result = result + (content.get(result) & 0xFF) + 1;
                result = result + (content.get(result) & 0xFF) + 1;
                break;
            case 5:
                result = result + (content.get(result) & 0xFF) + 1;
                result = result + (content.get(result) & 0xFF) + 1;
                result = result + (content.get(result) & 0xFF) + 1;
                result = result + (content.get(result) & 0xFF) + 1;
                result = result + (content.get(result) & 0xFF) + 1;
                break;
            default:
                result = result + (content.get(result) & 0xFF) + 1;
                result = result + (content.get(result) & 0xFF) + 1;
                result = result + (content.get(result) & 0xFF) + 1;
                result = result + (content.get(result) & 0xFF) + 1;
                result = result + (content.get(result) & 0xFF) + 1;
                result = result + (content.get(result) & 0xFF) + 1;
                for (int entry = 7; entry <= entryIndex; entry++) {
                    result = result + (content.get(result) & 0xFF) + 1;
                }
                break;
        }
        
        return result;        
    }

    protected static void setSafely(BinaryArray content, int index, byte value) {
        final int deficiency = index - content.size();

        for (int i = 0; i <= deficiency; i++) {
            content.append((byte)0);
        }

        content.set(index, value);
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder(getClass().getSimpleName()).append(": ");

        if (isNull) {
            result.append("NULL");
        } else {
            final byte[] contentBytes = content.toByteArray();

            result.append("entriesOffset=").append(entriesOffset).append(",");
            result.append("numberOfEntries=").append(numberOfEntries()).append(" ");
            result.append("[");
            result.append(StringUtils.toHex(content.toByteArray(), 0, entriesOffset));
            result.append("^");
            result.append(StringUtils.toHex(contentBytes, entriesOffset, content.size() - entriesOffset));
            result.append("]");
        }

        return result.toString();
    }
}