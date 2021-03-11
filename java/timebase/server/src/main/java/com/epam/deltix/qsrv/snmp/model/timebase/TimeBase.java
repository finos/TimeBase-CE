package com.epam.deltix.qsrv.snmp.model.timebase;

import com.epam.deltix.snmp.pub.*;

/**
 *
 */
public interface    TimeBase {

    @Id (100)
    @Description ("Data Cache Information")
    public TBDataCache          getDataCache ();

    @Id (101)
    @Description ("Active Cursors")
    public Table <Cursor>       getCursors ();

    @Id (102)
    @Description ("Active Loaders")
    public Table <Loader>       getLoaders ();

    @Id (103)
    @Description ("License Information")
    public LicenseInfo          getLicense ();

}
