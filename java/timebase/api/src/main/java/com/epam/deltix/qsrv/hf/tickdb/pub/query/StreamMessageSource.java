package com.epam.deltix.qsrv.hf.tickdb.pub.query;

import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;

/**
 *  Supplies an InstrumentMessage as well as information about
 *  its source stream and RecordClassDescriptor.
 */
public interface StreamMessageSource<T> extends TypedMessageSource {
    /**
     *  Returns the current message.
     */
    public T getMessage ();

    /**
     *  Return the index of the stream that is the source of the current
     *  message.
     *
     *  @return The current message source stream's index.
     */
    public int                              getCurrentStreamIndex ();

    /**
     *  Return the key of the stream that is the source of the current
     *  message.
     *
     *  @return The source stream key.
     */
    public String                           getCurrentStreamKey ();

    /**
     *  Return the current stream instance, unless it has been removed,
     *  in which case null is returned.
     */
    public TickStream                       getCurrentStream ();
    
    /**
     *  Return a small number identifying the returned entity. This number
     *  is unique throughout the life of the message source. Removing
     *  entities from subscription does not create reusable holes in the
     *  "space" of entity indexes.
     * 
     *  @see EntitySubscriptionController
     */
    public int                              getCurrentEntityIndex ();   
}
