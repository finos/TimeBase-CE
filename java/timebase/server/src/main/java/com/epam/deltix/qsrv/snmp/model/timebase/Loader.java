package com.epam.deltix.qsrv.snmp.model.timebase;

import com.epam.deltix.snmp.pub.*;

/**
 *
 */
@Description ("Information about an open loader")
public interface Loader {

    @Id(1) @Index()
    @Description ("Loader identifier")
    public int                  getLoaderId();

    @Id(2)
    @Description ("Loader source stream")
    public String               getSource();

    @Id(3)
    @Description ("The timestamp of the last message written by this loader")
    public String               getLoaderLastMessageTime();
}
