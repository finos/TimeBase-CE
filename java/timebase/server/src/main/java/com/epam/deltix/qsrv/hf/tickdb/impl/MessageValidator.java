package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.codec.RecordClassInfo;
import com.epam.deltix.qsrv.hf.pub.codec.validerrors.ValidationError;

/**
 *  Implements sequential access to unbound records.
 */
public interface MessageValidator {
    public RecordClassInfo getClassInfo ();
    
    public int                  validate (RawMessage msg);
    
    public int                  getNumErrors ();
    
    public ValidationError      getError (int idx);
}
