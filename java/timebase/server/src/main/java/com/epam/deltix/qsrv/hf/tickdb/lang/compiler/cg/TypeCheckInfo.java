package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.TypeCheck;

class TypeCheckInfo {
    TypeCheck                       typeCheck = null;
    QValue                   cache = null;

    public TypeCheckInfo (TypeCheck typeCheck) {
        this.typeCheck = typeCheck;
    }
}
