package com.epam.deltix.qsrv.dtb.store.pub;

/**
 *  A reusable object for accessing TimeBase messages.
 */
public interface DataWriter extends DataAccessor {
    /**
     *  Resets this accessor to focus on
     *  the first message at or after the specified timestamp.
     * 
     *  @param nstime    The timestamp (in nanoseconds) to seek.
     * 
     */
    public void                 open (
        long                        nstime,
        EntityFilter                filter
    ); 
    
    /**
     *  Inserts a message at the current location.
     */
    public void                 insertMessage (
        int                         entity,
        long                        nstime,
        int                         typeCode,
        TSMessageProducer           producer
    );

    /**
     *  Write a message at the current location.
     */
    public void                 appendMessage (
            int                         entity,
            long                        nstime,
            int                         typeCode,
            TSMessageProducer           producer,
            boolean                     truncate
    );


    //public void             truncate(long nstime, int[] entities);

    public void             truncate(long nstime, int entity);

//    public void             cut (long[] range, int[] entities);
//
//    public void             cut (long startTime, long endTime);

}
