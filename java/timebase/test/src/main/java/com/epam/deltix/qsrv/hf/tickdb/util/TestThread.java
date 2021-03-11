package com.epam.deltix.qsrv.hf.tickdb.util;

import com.epam.deltix.util.concurrent.UncheckedInterruptedException;

public abstract class TestThread extends Thread {
    private Throwable           error = null;

    protected TestThread (String name) {
        super (name);
    }

    public Throwable            getError () {
        return error;
    }

    protected abstract void     doRun () throws Exception;

    @Override
    public final void           run () {
        try {
            doRun ();
        } catch (InterruptedException x) {
        } catch (UncheckedInterruptedException x) {
        } catch (Throwable x) {
            error = x;
        }
    }
}
