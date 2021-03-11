package com.epam.deltix.util.vsocket;

import com.epam.deltix.util.lang.Disposable;

/**
 *
 */
public interface VSocket extends Disposable {

    VSocketInputStream          getInputStream();

    VSocketOutputStream         getOutputStream();

    String                      getRemoteAddress();

    //void                        restore(VSocket from);

    void                        close();

    int                         getCode();

    void                        setCode(int code);

    /**
     * Serial number of this socket.
     * Should be used only for easier identification of the socket during debug.
     */
    int getSocketNumber();

    default String getSocketIdStr() {
        return '@' + Integer.toHexString(getCode()) + "#" + getSocketNumber();
    }
}
