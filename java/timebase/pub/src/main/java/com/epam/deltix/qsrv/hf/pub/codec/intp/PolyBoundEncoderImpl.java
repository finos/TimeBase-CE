package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.memory.MemoryDataOutput;

/**
 *
 */
public class PolyBoundEncoderImpl implements PolyBoundEncoder {
    private final FixedBoundEncoder []                  encoders;
    private final RecordTypeMap<Class>                  classToEncoderMap;
    private final RecordTypeMap<RecordClassDescriptor>  descriptorToEncoderMap;
    
    public PolyBoundEncoderImpl (FixedBoundEncoder [] encoders) {
        this.encoders = encoders; 
        
        int                     num = encoders.length;
        Class <?> []            classes = new Class <?> [num];
        RecordClassDescriptor [] rcds = new RecordClassDescriptor [num];
        
        for (int ii = 0; ii < num; ii++) {
            if (encoders [ii] == null)
                continue;

            classes [ii] = encoders [ii].getClassInfo().getTargetClass();
            rcds [ii] = encoders [ii].getClassInfo ().getDescriptor ();
        }
        
        classToEncoderMap = new RecordTypeMap<Class> (classes);
        descriptorToEncoderMap = new RecordTypeMap<RecordClassDescriptor> (rcds);
    }

    public void             encode (
        RecordClassDescriptor   rcd, 
        Object                  message, 
        MemoryDataOutput        out
    )
    {
        int                     code = descriptorToEncoderMap.getCode (rcd);
        
        out.writeUnsignedByte (code);
        
        encoders [code].encode (message, out);
    }
    
    public void             encode (Object message, MemoryDataOutput out) {
        int                     code = classToEncoderMap.getCode (message.getClass ());
        
        out.writeUnsignedByte (code);
        
        encoders [code].encode (message, out);
    }            
}
