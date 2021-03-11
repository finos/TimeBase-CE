package com.epam.deltix.qsrv.hf.tickdb.impl;

/**
 *  Implements internal quick rejection of messages, mainly based on the symbol.
 */
public interface QuickMessageFilter {

    public boolean          acceptAllEntities ();

    public boolean          acceptEntity (CharSequence symbol);

    public void             setChangeListener(Runnable r);

    public boolean          isRestricted();
}
