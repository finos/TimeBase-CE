package com.epam.deltix.util.security;

import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.containers.BinaryArray;

public class EntitlementIDComposer<T extends EntitlementIDComposer> implements EntitlementID {
    
    protected final int         entriesOffset;

    protected BinaryArray       content;
    protected boolean           isNull;

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

    public T appendEntry(byte[] content) {
        return appendEntry(content, 0, content.length);
    }
    
    @SuppressWarnings("unchecked")
    public T appendEntry(byte[] content, int offset, int length) {        
        setSafely(this.content, entriesOffset, (byte) (numberOfEntries() + 1));
        
        this.content.append((byte) length);
        this.content.append(content, offset, length);
        
        isNull = false;
        
        return (T) this; // unchecked
    }

    @SuppressWarnings("unchecked")
    public T appendAllEntries(T from) {
        setSafely(this.content, entriesOffset, (byte) (numberOfEntries() + from.numberOfEntries()));

        final int sizeToAdd = from.content.size() - from.entriesOffset - 1;
        content.append(from.content.toByteArray(), from.entriesOffset + 1, sizeToAdd);

        isNull = false;
        
        return (T) this; // unchecked
    }
    
    @Override
    public boolean isNull() {
        return isNull;
    }

    @Override
    public int numberOfEntries() {
        return content != null ? content.get(entriesOffset) & 0xFF : 0;
    }

    @Override
    public byte[] content() {
        return content != null ? content.toByteArray() : null;
    }

    @Override
    public int size() {
        return content != null ? content.size() : 0;
    }
    
    @Override
    public int entryOffset(int entryIndex) {
        if (entryIndex >= numberOfEntries()) {
            throw new IndexOutOfBoundsException();
        }
        return getEntryContentIndex(entryIndex) + 1;
    }

    @Override
    public int entryLength(int entryIndex) {
        if (entryIndex >= numberOfEntries()) {
            throw new IndexOutOfBoundsException();
        }        
        return content.get(getEntryContentIndex(entryIndex)) & 0xFF;
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder(getClass().getSimpleName()).append(':');
        if (isNull) {
            result.append("NULL");
        } else {
            result.append("entriesOffset=").append(Integer.toString(entriesOffset)).append(',');
            result.append("numberOfEntries=").append(Integer.toString(numberOfEntries()));
            result.append('[');
            result.append(StringUtils.toHex(content.toByteArray(), 0, entriesOffset));
            result.append('^');
            result.append(StringUtils.toHex(content.toByteArray(), entriesOffset, content.size() - entriesOffset));
            result.append(']');
        }
        return result.toString();
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
        // some unrolling
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
        int overhead = index - content.size();

        for (int i = 0; i <= overhead; i++)
            content.append((byte)0);

        content.set(index, value);

//        final int d = content.size() - index;
//        if (d <= 0) {
//            for (int i = 0; i <= -d - 1; i++) {
//                content.add(i, (byte) 0);
//            }
//            content.add(-d, value);
//            return;
//        }
//        content.set(index, value);
    }
}