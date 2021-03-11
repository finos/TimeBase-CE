package com.epam.deltix.util.io.waitstrat;

import com.epam.deltix.util.lang.Changeable;
import com.epam.deltix.util.lang.Disposable;
import java.io.IOException;

/**
 *
 */
public interface WaitStrategy extends Disposable {
    public void                 waitSignal() throws InterruptedException;
    public void                 signal();
}
