package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import com.epam.deltix.qsrv.hf.pub.md.DataType;

/*
   Represents logical connective expression (disjunction or conjunction)
 */
public class ConnectiveExpression extends CompiledComplexExpression {

    private final boolean               conjunction; //

    public ConnectiveExpression(boolean conjunction, DataType type, CompiledExpression... args) {
        super(type, args);
        this.conjunction = conjunction;
    }

    public CompiledExpression getArgument() {
        return args[0];
    }

    public boolean          isConjunction() {
        return conjunction;
    }

    @Override
    protected void          print(StringBuilder out) {
        String code = conjunction ? " AND " : " OR ";

        out.append (" (");
        for (int i = 0; i < args.length; i++) {
            out.append (i > 0 ? code : "");
            args [i].print(out);
        }
        out.append (")");
    }
}
