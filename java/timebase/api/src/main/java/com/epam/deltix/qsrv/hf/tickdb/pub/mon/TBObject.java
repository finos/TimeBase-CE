package com.epam.deltix.qsrv.hf.tickdb.pub.mon;

/**
 *
 */
public interface TBObject {

    long            getId ();

    void            setUser(String user);

    String          getUser();

    void            setApplication(String application);

    String          getApplication();

}
