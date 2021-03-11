package com.epam.deltix.qsrv.hf.pub.codec;

import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;

/**
 * Implements encoding and decoding of strings composed from characters of the limited set.
 * <p>
 * The character set includes all Latin capital letters [A-Z], digits [0-9] as well as
 * the following special characters !"#$%&amp;'()*+,-./:;&lt;=&gt;?@[\]^_. Its range is [0x20, 0x5F].
 * </p>
 */
public final class AlphanumericCodec {
    private static final double LOG2 = Math.log(2);
    private static final char CHAR_MIN = 0x20;
    private static final char CHAR_MAX = 0x5F;
    
    private final int n;
    private final StringBuilder sb;

    private final int sizeBits;
    private final int numBytes;
    private byte[] bytes;

    public static int   getNumSizeBits (int n) {
        return ((int) Math.ceil(Math.log(n + 2) / LOG2));
    }
    
    public AlphanumericCodec(int n) {
        this.n = n;
        sizeBits = getNumSizeBits (n);
        final int numBits = sizeBits + 6 * n;
        numBytes = numBits / 8 + (numBits % 8 != 0 ? 1 : 0);
        bytes = new byte[numBytes];
        sb = new StringBuilder(n);
    }

    public final CharSequence readCharSequence (MemoryDataInput in) {
        final int start = in.getStart();
        final int begin = in.getCurrentOffset ();
        final int end = in.getLength() + start;
        final CharSequence result = decode(in.getBytes(), begin, end);
        in.seek(byteIdx - start);
        return result;
    }

    public final int readInt (MemoryDataInput in) {
        final int start = in.getCurrentOffset ();
        final byte[] bytes = in.getBytes();
        final int len = decodeLength(bytes, start);
        final int size;
        int result;
        if (len == n + 1) {
            result = IntegerDataType.INT32_NULL;
            size = calculateSize(0);

        } else {
            size = calculateSize(len);
            final int end;
            assert start + len < (end = start + in.getLength()) : "start=" + start + " size=" + size + " end=" + end;

            result = 0;
            for (int i = start; i < start + size; i++)
                result = (result << 8) | (bytes[i] & 0xff);

            result <<= (32 - 8 * size);
        }

        in.seek(in.getPosition() + size);
        return result;
    }

    public final long readLong (MemoryDataInput in) {
        final int start = in.getCurrentOffset ();
        final byte[] bytes = in.getBytes();
        final int len = decodeLength(bytes, start);
        final int size;
        long result;
        if (len == n + 1) {
            result = IntegerDataType.INT64_NULL;
            size = calculateSize(0);

        } else {
            size = calculateSize(len);
            final int end;
            assert start + len < (end = start + in.getLength()) : "start=" + start + " size=" + size + " end=" + end;

            result = 0;
            for (int i = start; i < start + size; i++)
                result = (result << 8) | (bytes[i] & 0xff);

            result <<= (64 - 8 * size);
        }

        in.seek(in.getPosition() + size);
        return result;
    }

    public final void writeCharSequence (CharSequence v, MemoryDataOutput out) {
        final int size = calculateSize(v != null ? v.length() : 0);
        final int start = out.getPosition();
        out.makeRoom(size);
        encode(v, out.getBuffer(), start);
        assert start + size == byteIdx;
        out.seek(byteIdx);
    }

    public final void writeInt (int v, MemoryDataOutput out) {
        if (v == IntegerDataType.INT32_NULL)
            writeCharSequence(null, out);
        else {
            final int len = decodeLength(v);
            final int size = calculateSize(len);
            out.makeRoom(size);

            for (int i = 0; i < size; i++)
                out.writeByte((v >>> (24 - 8 * i)) & 0xff);
        }
    }

    public final void writeLong(long v, MemoryDataOutput out) {
        if (v == IntegerDataType.INT64_NULL)
            writeCharSequence(null, out);
        else {
            final int len = decodeLength(v);
            final int size = calculateSize(len);
            out.makeRoom(size);

            for (int i = 0; i < size; i++)
                out.writeByte((v >>> (56 - 8 * i)) & 0xff);
        }
    }

    public long encodeToLong(CharSequence s) {
        if (numBytes > 8)
            throw new IllegalArgumentException(s + " is longer then " + n);
        if (s == null)
            throw new UnsupportedOperationException("null is not supported");

        encode(s, bytes, 0);
        long result = 0;
        for (int i = 0; i < byteIdx; i++)
            result = (result << 8) | (bytes[i] & 0xff);

        return result << (64 - byteIdx * 8);
    }

    public CharSequence decodeFromLong(long v) {
        final int len = decodeLength(v);
        final int size = calculateSize(len);
        for (int i = 0; i < size; i++)
            bytes[i] = (byte) ((v >>> (56 - 8 * i)) & 0xff);

        return decode(bytes, 0, 8);
    }

    private byte[] encode(CharSequence s, byte[] bytes, int start) {
        final int len;
        if (s != null) {
            len = s.length();
            if (len > n)
                throw new IllegalArgumentException(String.format("'%s' is longer then %d", s, n));
        } else
            len = n + 1;

        byteIdx = start;
        int bitIdx;
        byte v;
        if (sizeBits < 8) {
            v = (byte) (len << (8 - sizeBits));
            bitIdx = sizeBits;
        } else {
            bytes[byteIdx++] = (byte) (len >> (sizeBits - 8));
            v = (byte) ((len & 0xff) << (16 - sizeBits));
            bitIdx = sizeBits - 8;
        }
        if (s == null || len == 0) {
            bytes[byteIdx++] = v;
            return bytes;
        }

        byte v1;
        int charIdx = 0;
        while (charIdx < len) {
            final char ch = s.charAt(charIdx);
            if (ch < CHAR_MIN || ch > CHAR_MAX)
                throw new IllegalArgumentException(String.format("Character '%c' (0x%H) in '%s' is out of range", ch, ch, s ));

            final int chVal = ch - CHAR_MIN;
            int sizeValue = 6;
            if (8 - bitIdx < sizeValue) {
                // split
                // size  - step (8 - bitIdx)
                v |= (byte) (chVal >>> (sizeValue - (8 - bitIdx))); // shift left to extract upper bits
                // 8 - (size  - step)
                v1 = (byte) ((chVal << (16 - sizeValue - bitIdx)) & 0xff); // shift right to extract lower bits
            }
            else {
                // append
                v |= (byte) (chVal << (8 - sizeValue - bitIdx));
                v1 = 0;
            }
            bitIdx += sizeValue;
            charIdx++;


            if (bitIdx >= 8) {
                bytes[byteIdx++] = v;
                bitIdx -= 8;
                v = v1;
            }

            if(charIdx == len && v != 0) {
                bytes[byteIdx++] = v;
                bitIdx = 0;
            }
        }
        if (bitIdx > 0)
            bytes[byteIdx++] = v;
        
        return bytes;
    }

    private int decodeLength(byte[] bytes, int start) {
        return (sizeBits < 8) ?
            (bytes[start] & 0xff) >>> (8 - sizeBits) :
            (bytes[start] & 0xff) << (sizeBits - 8) | (bytes[start + 1] & 0xff) >>> (16 - sizeBits);
    }

    private int decodeLength(int v) {
        return v >>> (32 - sizeBits);
    }

    private int decodeLength(long v) {
        return (int) (v >>> (64 - sizeBits));
    }

    private int calculateSize(int len) {
        final int numBits = sizeBits + 6 * len;
        return numBits / 8 + (numBits % 8 != 0 ? 1 : 0);
    }

    private int byteIdx;

    private CharSequence decode(byte[] bytes, int start, int end) {
        final int arrayEnd = start + Math.min(numBytes, Math.min(end - start, bytes.length - start));
        final int len;

        byteIdx = start;
        int bitIdx;
        if (sizeBits < 8) {
            len = (bytes[byteIdx] & 0xff) >>> (8 - sizeBits);
            bitIdx = sizeBits;
        } else {
            len = (bytes[byteIdx] & 0xff) << (sizeBits - 8) | (bytes[++byteIdx] & 0xff) >>> (16 - sizeBits);
            bitIdx = sizeBits - 8;
        }
        if (len == n + 1) {
            byteIdx++;
            return null;
        }

        sb.setLength(0);
        int charIdx = 0;
        int bitIdx2 = 0;

        int v1 = 0;
        int sizeValue = 6;
        while (byteIdx < arrayEnd && charIdx < len) {
            int chVal;

            if (6 - bitIdx2 <= 8 - bitIdx) {
                // have all bits necessary to compose the value
                int step = 6 - bitIdx2;
                chVal = v1 | (((bytes[byteIdx] & 0xff) << (bitIdx)) & 0xff) >>> (8 - sizeValue + bitIdx2);
                sb.append((char) (chVal + CHAR_MIN));
                charIdx++;

                v1 = 0;
                bitIdx2 = 0;
                bitIdx += step;
            } else {
                // have only part of the value
                int step = 8 - bitIdx;
                // clean up upper bits and make a shift
                v1 = (((bytes[byteIdx] & 0xff) << (bitIdx)) & 0xff) >>> (8 - sizeValue);
                bitIdx2 += step;
                bitIdx += step;
            }

            if (bitIdx >= 8) {
                byteIdx++;
                bitIdx -= 8;
            }
        }
        if (bitIdx > 0)
            byteIdx++;

        return sb;
    }

    public static void  skip (MemoryDataInput in, int n) {
        skip(in, getNumSizeBits(n), n);
    }

    public static void  skip (MemoryDataInput in, int numSizeBits, int n) {
        int     numChars = 0;
        int     totalBitsRead = 0;
        
        while (totalBitsRead < numSizeBits) {
            numChars = (numChars << 8) | in.readUnsignedByte ();
            totalBitsRead += 8;
        }
        
        int     contentBitsRead = totalBitsRead - numSizeBits;
        
        numChars = numChars >> contentBitsRead;
        
        int     numBitsLeft = 
            numChars == n + 1 ?
                0 :     // null
                numChars * 6 - contentBitsRead;
            
        in.skipBytes ((numBitsLeft + 7) / 8);        
    }

    public static void          staticWrite (
        CharSequence                s,
        int                         numSizeBits, 
        int                         n, 
        MemoryDataOutput            out
    )
    {
        if (s == null) {
            writeNull (out, numSizeBits, n);
            return;
        }
        
        final int                   slen = s.length ();
        
        if (slen > n)
            throw new IllegalArgumentException (
                "Alphanumeric value longer than " + n + ": " + s.toString ()
            );
        
        int     x = slen;
        int     xbits = numSizeBits;        
                
        for (int ii = 0; ii < slen; ) {
            final int     remain = xbits - 8;
            //
            //  Flush out 8 upper bits
            //
            if (remain >= 0) {                
                out.writeUnsignedByte (x >>> remain);
                xbits -= 8;
                continue;
            }
            //
            //  Encode the next character.
            //
            char    ch = s.charAt (ii);
            int     charCode = ch - CHAR_MIN;
            
            if (charCode < 0 || charCode >= 0x40)
                throw new IllegalArgumentException (
                    "Out-of-range character '" + ch + "' at position " + ii + 
                    " in  " + s
                );
            
            x = (x << 6) | charCode;
            xbits += 6;
            ii++;
        }
        //
        //  We can have up to 7 + 6 = 13 unflushed bits. 
        //  Inline the flushing for speed.
        //
        assert xbits <= 13;
        
        if (xbits > 0) {           
            final int     remain = xbits - 8;

            if (remain < 0) {
                out.writeUnsignedByte (x << (-remain));
                xbits = 0;
            }
            else {
                out.writeUnsignedByte (x >>> remain);
                xbits -= 8;    
            }            
        }
        //
        //  After the first flush we cannot have more than 5 bits left,
        //  so we can skip the >=8 check.
        //
        assert xbits <=5;
        
        if (xbits > 0)         
            out.writeUnsignedByte (x << (8 - xbits));        
    }
    
    public static StringBuilder staticRead (
        MemoryDataInput             in, 
        int                         numSizeBits, 
        int                         n, 
        StringBuilder               sb
    )
    {
        int     x = 0;
        int     xbits = 0;
        
        while (xbits < numSizeBits) {
            x = (x << 8) | in.readUnsignedByte ();
            xbits += 8;
        }
                
        xbits -= numSizeBits;
        
        assert xbits < 8;
        
        final int   numChars = x >>> xbits;
        
        if (numChars == n + 1)
            return (null);                        
        
        if (sb == null)
            sb = new StringBuilder ();
        
        sb.setLength (numChars);
        
        for (int ii = 0; ii < numChars; ii++) {
            if (xbits < 6) {
                x = (x << 8) | in.readUnsignedByte ();
                xbits += 8;
            }
            
            xbits -= 6;
            
            final int     charCode = (x >>> xbits) & 0x3F;
            
            sb.setCharAt (ii, (char) (CHAR_MIN + charCode));            
        }        
        
        return (sb);
    }

    public static void  writeNull (MemoryDataOutput out, int n) {
        writeNull(out, getNumSizeBits(n), n);
    }

    public static void  writeNull (MemoryDataOutput out, int numSizeBits, int n) {
        int     headerBits = (numSizeBits + 7) & 0xFFFFFFF8;
        int     extraBits = headerBits - numSizeBits;
        int     header = (n + 1) << extraBits;
        int     b = headerBits - 8;
        
        while (b >= 0) {
            out.writeUnsignedByte (header >> b);
            b -= 8;
        }
    }

    public static void validate(int n, CharSequence value) {
        if (value != null) {
            if (value.length() > n)
                throw new IllegalArgumentException(String.format("'%s' is longer then %d (%d)", value, n, value.length()));

            for (int i = 0; i < value.length(); i++) {
                final char ch = value.charAt(i);
                if (ch < CHAR_MIN || ch > CHAR_MAX)
                    throw new IllegalArgumentException(String.format("Character '%c' (0x%H) in '%s' is out of range", ch, ch, value));
            }
        }
    }

    public final void writeObject(Object v, MemoryDataOutput out) {
        if (v == null || v instanceof CharSequence)
            writeCharSequence((CharSequence) v, out);
        else
            writeCharSequence((String) v, out);
    }


    public void skip(MemoryDataInput in) {
        skip(in, sizeBits, n);
    }
}
