package com.epam.deltix.qsrv.snmp.model.timebase;

import com.epam.deltix.snmp.pub.Description;
import com.epam.deltix.snmp.pub.Id;
import com.epam.deltix.snmp.pub.Index;

/**
 *
 */
@Description ("Information about failure")
public interface Failure {

    @Id(1) @Index
    @Description("Index")
    public int                      getIndex();

    @Id(2)
    @Description("Error Message")
    public String                  getMessage();
}
