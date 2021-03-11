package com.epam.deltix.qsrv.hf.tickdb.pub;

public interface TickDBContext<T extends TickDB> {
    
    T getTickDB();
    
}
