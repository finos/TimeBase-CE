package com.epam.deltix.qsrv.hf.tickdb.lang.runtime;

import com.epam.deltix.qsrv.hf.blocks.InstrumentIndex;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.*;
import com.epam.deltix.util.collections.IndexedArrayList;
import java.util.Arrays;
import java.util.Enumeration;

/**
 *
 */
public abstract class FilterIMSImpl
    extends DelegatingInstrumentMessageSource
{
    protected static final int                  REJECT = 0;
    protected static final int                  ACCEPT = 1;
    protected static final int                  ABORT = 2;
    
    private boolean                             atEnd = false;
    protected final RecordClassDescriptor []    inputTypes;
    protected final RecordClassDescriptor []    outputTypes;
    protected final ReadableValue []            params;
    private final int []                        inputTypeIndexMap;
    private final InstrumentIndex               instrumentIndex =
        new InstrumentIndex ();

    private final IndexedArrayList <String>     streamKeyIndex =
        new IndexedArrayList <String> ();

    private final IndexedArrayList <RecordClassDescriptor> typeIndex =
        new IndexedArrayList <RecordClassDescriptor> ();

    protected RawMessage                        outMsg;
    private Enumeration <FilterState>           aggregateEnum = null;
    
    protected FilterIMSImpl (
        InstrumentMessageSource             source,
        RecordClassDescriptor []            inputTypes,
        RecordClassDescriptor []            outputTypes,
        ReadableValue []                    params
    )
    {
        super (source);
        
        this.outputTypes = outputTypes;
        this.inputTypes = inputTypes;
        this.params = params;

        this.inputTypeIndexMap = new int [inputTypes.length];
        Arrays.fill (inputTypeIndexMap, -1);
    }

    protected abstract FilterState          newState ();

    protected abstract FilterState          getState (RawMessage msg);
    
    @Override
    public int                              getCurrentStreamIndex () {
        if (source.getCurrentStream() != null)
            return (streamKeyIndex.getIndexOrAdd (getCurrentStreamKey ()));

        return -1;
    }

    @Override
    public int                              getCurrentEntityIndex () {
        return (instrumentIndex.getOrAdd (getMessage ()));
    }

    @Override
    public RawMessage                       getMessage () {
        return (outMsg);
    }

    @Override
    public RecordClassDescriptor            getCurrentType () {
        return (getMessage ().type);
    }

    @Override
    public int                              getCurrentTypeIndex () {
        return (typeIndex.getIndexOrAdd (getCurrentType ()));
    }

    /**
     *  Efficiently returns an index of the current message type in
     *  the array of output types returned by {@link #getMessageTypes}.
     */
    protected int                          getInputTypeIndex () {
        int     ctix = source.getCurrentTypeIndex ();
        int     out = inputTypeIndexMap [ctix];

        if (out == -1) {
            RecordClassDescriptor   curType = source.getCurrentType ();

            for (int ii = 0; ii < inputTypes.length; ii++) {
                if (curType.equals (inputTypes [ii])) {
                    out = ii;
                    break;
                }
            }

            if (out == -1)
                throw new IllegalStateException (
                    "Type " + curType + 
                    " is not found in the preset list of input types"
                );

            inputTypeIndexMap [ctix] = out;
        }

        return (out);
    }
    
    @Override
    public boolean                      isAtEnd () {
        return (atEnd);
    }

    protected int                       accept (RawMessage inMsg, FilterState state) {
        outMsg = inMsg;
        return (ACCEPT);
    }

    protected abstract Enumeration <FilterState>  getStates ();
   
    /**
     *  Aggregate queries override next() to call this method.
     */
    protected boolean                   nextAggregated () {
        if (aggregateEnum == null) {
            while (source.next ())                 
                getStateAndProcess (); 
            
            aggregateEnum = getStates ();
        }
        
        for (;;) {
            if (!aggregateEnum.hasMoreElements ())
                return (false);

            FilterState     state = aggregateEnum.nextElement ();
            
            if (state.accepted) {                
                outMsg = state.getLastMessage ();
                return (true);
            }
        }
    }
    
    @Override
    public boolean                      next () {
        for (;;) {
            if (!source.next ()) {
                atEnd = true;
                return (false);
            }

            int s = getStateAndProcess ();
            
            switch (s) {
                case ABORT:     return (false);
                case ACCEPT:    return (true);
            }
        }
    }
    
    private int                         getStateAndProcess () {
        final RawMessage      inMsg = (RawMessage) source.getMessage ();
        final FilterState     state = getState (inMsg);
        final int             status = accept (inMsg, state);
        
        if (status == ACCEPT) 
            state.accepted = true;
                    
        return (status);
    }   
    
    public RecordClassDescriptor []     getMessageTypes () {
        return (outputTypes);
    }
}
