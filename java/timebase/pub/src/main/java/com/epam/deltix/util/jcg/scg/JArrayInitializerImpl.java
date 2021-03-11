package com.epam.deltix.util.jcg.scg;

import com.epam.deltix.util.jcg.*;

import java.io.IOException;
import java.util.ArrayList;

public class JArrayInitializerImpl extends JExprImplBase implements JArrayInitializer {
    //private final Class<?> javaType;
    private final ArrayList<JExpr> values = new ArrayList <> ();

    @Override
    public void print(int outerPriority, SourceCodePrinter out) throws IOException {
        // "new Type[]" must be added to initialize a local variable 
        out.print("{ ");
        final int size = values.size();
        for (int i = 0; i < size; i++) {
            out.print(values.get(i));
            if(i<size -1)
                out.print(", ");

        }
        out.print(" }");
    }

    public JArrayInitializerImpl (JContextImpl context, JType javaType) {
        super (context);
    }

    @Override
    public void add(JExpr value) {
        values.add(value);
    }
}
