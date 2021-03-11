package com.epam.deltix.qsrv.hf.tickdb.pub.lock;


public interface LockHandler {
    
    void    addEventListener(LockEventListener listener);

    void    removeEventListener(LockEventListener listener);
}
