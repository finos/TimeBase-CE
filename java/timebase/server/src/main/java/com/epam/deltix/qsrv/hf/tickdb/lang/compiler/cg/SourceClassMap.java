package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.DataFieldRef;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.*;
import java.util.*;

/**
 *
 */
class SourceClassMap {
    public final RecordClassDescriptor []           concreteTypes;

    private final Map <RecordClassDescriptor, ClassSelectorInfo>  map =
        new HashMap <RecordClassDescriptor, ClassSelectorInfo> ();

    private final Set <TypeCheckInfo>               typeChecks =
        new HashSet <TypeCheckInfo> ();

    private ClassSelectorInfo                        getOrCreate (
        RecordClassDescriptor                           type
    )
    {
        ClassSelectorInfo           csi = map.get (type);

        if (csi == null) {
            RecordClassDescriptor   parentRCD = type.getParent ();

            ClassSelectorInfo       parentCSI =
                parentRCD == null ?
                    null :
                    getOrCreate (parentRCD);

            csi = new ClassSelectorInfo (parentCSI, type);

            map.put (type, csi);
        }
        
        return (csi);
    }

    public SourceClassMap (RecordClassDescriptor [] concreteTypes) {
        this.concreteTypes = concreteTypes;

        int                         n = concreteTypes.length;

        for (int ii = 0; ii < n; ii++) {
            RecordClassDescriptor   rcd = concreteTypes [ii];
            ClassSelectorInfo       csi = getOrCreate (rcd);
            csi.ordinal = ii;
        }
    }

    public Collection <ClassSelectorInfo>       allClassInfo () {
        return (map.values ());        
    }

    public Collection <TypeCheckInfo>           allTypeChecks () {
        return (typeChecks);
    }

    public ClassSelectorInfo                    getSelectorInfo (
        RecordClassDescriptor                       rcd
    )
    {
        return (map.get (rcd));
    }

    public void                                 discoverFieldSelectors (
        CompiledExpression                          e
    )
    {
        if (e instanceof FieldSelector)
            register ((FieldSelector) e);
        else if (e instanceof TypeCheck)
            register ((TypeCheck) e);

        if (e instanceof CompiledComplexExpression) {
            CompiledComplexExpression   ccx = (CompiledComplexExpression) e;

            for (CompiledExpression arg : ccx.args)
                discoverFieldSelectors (arg);
        }
    }

    private void                                register (
        TypeCheck                                   typeCheck
    )
    {
        typeChecks.add (new TypeCheckInfo (typeCheck));
    }

    private void                                register (
        FieldSelector                               fieldSelector
    )
    {
        DataFieldRef            fieldRef = fieldSelector.fieldRef;
        DataField               df = fieldRef.field;

        if (!(df instanceof NonStaticDataField))
            return;
        
        for (ClassSelectorInfo csi : map.values ())
            csi.nonStaticFieldUsedFrom (fieldSelector);
    }
}
