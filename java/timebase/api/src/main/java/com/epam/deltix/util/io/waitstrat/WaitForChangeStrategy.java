package com.epam.deltix.util.io.waitstrat;

import com.epam.deltix.util.lang.Changeable;
import com.epam.deltix.util.lang.Disposable;

/**
 *
 */
public interface WaitForChangeStrategy extends Disposable {
    public void                 waitFor(Changeable value) throws InterruptedException;
}
