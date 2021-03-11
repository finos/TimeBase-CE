package com.epam.deltix.qsrv.dtb.store.codecs;

import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;

/**
 *
 */
public class SymmetricSizeCodec {
    public static final int     LIMIT_7_BITS =  1 << 7;
    public static final int     LIMIT_12_BITS = 1 << 12;
    public static final int     LIMIT_18_BITS = 1 << 18;
    public static final int     LIMIT_26_BITS = 1 << 26;
    
    public static int           requiredFieldSize (int s) {
        if (s < 0)
            throw new IllegalArgumentException (s + " is negative");
        
        if (s < LIMIT_7_BITS)
            return (1);
        
        if (s < LIMIT_12_BITS)
            return (2);
        
        if (s < LIMIT_18_BITS)
            return (3);
        
        if (s < LIMIT_26_BITS) 
            return (4);
                
        throw new IllegalArgumentException (s + " is too large"); 
    }
    
    public static void          write (int s, MemoryDataOutput out) {
        if (s < 0)
            throw new IllegalArgumentException (s + " is negative");
        else if (s < LIMIT_7_BITS)
            out.writeUnsignedByte (s);
        else if (s < LIMIT_12_BITS) {   // 10+6 bits | 10+6 bits
            out.writeUnsignedByte (0x80 | s & 0x3F);
            out.writeUnsignedByte (0x80 | s >> 6);
        } 
        else if (s < LIMIT_18_BITS) { // 110 + 5 bits | 8 bits | 110 + 5 bits
            out.writeUnsignedByte (0xC0 | s & 0x1F);
            out.writeUnsignedByte (s >> 5);
            out.writeUnsignedByte (0xC0 | s >> 13);
        } 
        else if (s < LIMIT_26_BITS) { // 111 + 5 bits | 8 bits | 8 bits | 110 + 5 bits
            out.writeUnsignedByte (0xE0 | s & 0x1F);
            out.writeUnsignedByte (s >> 5);
            out.writeUnsignedByte (s >> 13);
            out.writeUnsignedByte (0xE0 | s >> 21);
        } 
        else
            throw new IllegalArgumentException (s + " is too large"); 
    }
    
    public static int           endByteToFieldSize (int endByte) {
        switch ((endByte >>> 5) & 0x7) {
            case 7:
                return (4);
                
            case 6:
                return (3);
                
            case 5:
            case 4:
                return (2);
                
            default:    
                return (1);                            
        }
    }
    
    public static void          skipForward (MemoryDataInput mdi) {
        int         sz = endByteToFieldSize (mdi.readUnsignedByte ());
        
        mdi.skipBytes (sz - 1);              
    }
    
    
    public static int           readForward (MemoryDataInput mdi) {
        byte []     b = mdi.getBytes ();
        int         offset = mdi.getCurrentOffset ();
        int         sz = endByteToFieldSize (b [offset]);
        
        mdi.skipBytes (sz);
        
        return (read (b, offset));
    }

    public static int           readBackward (MemoryDataInput mdi, int offset) {
        final byte []         bytes = mdi.getBytes ();
        final int             szfsz = endByteToFieldSize (bytes [offset - 1]);
        final int             sz =
                SymmetricSizeCodec.read (bytes, offset - szfsz);

        assert sz > 0;

        return szfsz + sz;
    }
    
    public static int           read (
        byte []                     bytes, 
        int                         firstOffset
    )
    {
        byte        head = bytes [firstOffset];

        if ((head & 0x80) == 0)
            return (head);
        
        if ((head & 0x40) == 0) {
            int     tail = bytes [firstOffset + 1];
            
            if ((tail & 0xC0) != 0x80)
                throw new IllegalArgumentException (
                    "Head-Tail mismatch: " + head + ":" + tail
                );
            
            return (
                head & 0x3F | 
                ((tail & 0x3F) << 6)
            );
        }
        
        if ((head & 0x20) == 0) {
            int     tail = bytes [firstOffset + 2];
            
            if ((tail & 0xE0) != 0xC0)
                throw new IllegalArgumentException (
                    "Head-Tail mismatch: " + head + ":" + tail
                );
            
            return (
                head & 0x1F | 
                ((bytes [firstOffset + 1] & 0xFF) << 5) |
                ((tail & 0x1F) << 13)
            );
        }
        
        int         tail = bytes [firstOffset + 3];
        
        if ((tail & 0xE0) != 0xE0)
            throw new IllegalArgumentException (
                "Head-Tail mismatch: " + head + ":" + tail
            );
        
        return (
            head & 0x1F | 
            ((bytes [firstOffset + 1] & 0xFF) << 5) |
            ((bytes [firstOffset + 2] & 0xFF) << 13) |
            ((tail & 0x1F) << 21)
        );                
    }           
}
