package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

/**
 *
 */
public class InputParameterEnvironment extends EnvironmentFrame {
    public InputParameterEnvironment (Environment parent) {
        super (parent);
    }

    public InputParameterEnvironment () {
    }

    public void             addParameter (ParamSignature psig, int idx) {
        bind (NamedObjectType.VARIABLE, psig.name, new ParamRef (psig, idx));
    }
}
