package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;

/**
 *
 */
public interface AuthorizationContext {

    void                    checkCanCreateStreams();

    void                    checkReadable(DXTickStream stream);

    boolean                 canRead(DXTickStream stream);

    void                    checkWritable(DXTickStream stream);

    void                    checkPermission(String permission);

    void                    checkPermission(String permission, DXTickStream stream);

    boolean                 hasPermission(String permission);

    boolean                 hasPermission(String permission, DXTickStream stream);

    void                    checkCanImpersonate(String anotherUserId);

    boolean                 canImpersonate(String anotherUserId);
}
