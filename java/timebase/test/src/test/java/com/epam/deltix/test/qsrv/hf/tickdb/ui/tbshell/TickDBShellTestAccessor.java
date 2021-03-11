package com.epam.deltix.test.qsrv.hf.tickdb.ui.tbshell;

import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.TickDBShell;

/**
 * Provides public access to some internal methods of TickDBShell for testing purpose.
 *
 * @author Alexei Osipov
 */
public class TickDBShellTestAccessor extends TickDBShell {
    @Override
    public boolean doCommand(String key, String args) throws Exception {
        return super.doCommand(key, args);
    }

    @Override
    public boolean doSet(String option, String value) throws Exception {
        return super.doSet(option, value);
    }

    @Override
    public void doSet() {
        super.doSet();
    }
}