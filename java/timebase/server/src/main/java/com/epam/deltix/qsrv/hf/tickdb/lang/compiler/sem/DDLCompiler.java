/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.CompiledConstant;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.activities.StreamCreator;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.activities.StreamKiller;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.activities.StreamModifier;
import com.epam.deltix.qsrv.hf.tickdb.pub.BufferOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.PreparedQuery;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.StreamChangeTask;

import java.util.*;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.QQLCompiler.lookUpType;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.QQLCompiler.lookUpVariable;

/**
 *
 */
public class DDLCompiler {
    private final DXTickDB              db;
    private final Environment           env;
    private final QQLExpressionCompiler xc;
    
    public DDLCompiler (DXTickDB db, Environment env) {
        this.db = db;
        this.env = env;
        this.xc = new QQLExpressionCompiler (env);
    }

    public PreparedQuery     compileStatement (Statement s) {
        if (s instanceof CreateStreamStatement)
            return (compileCreateStreamStatement ((CreateStreamStatement) s));
        
        if (s instanceof ModifyStreamStatement)
            return (compileModifyStreamStatement ((ModifyStreamStatement) s));
        
        if (s instanceof DropStreamStatement)
            return (compileDropStreamStatement ((DropStreamStatement) s));
        
        throw new UnsupportedOperationException (s.toString ());
    }

    private PreparedQuery    compileDropStreamStatement (
        DropStreamStatement       s
    )
    {
        Identifier  sid = s.id;
        Object      obj = lookUpVariable (env, sid);
        
        if (!(obj instanceof DXTickStream))
            throw new IllegalObjectException (sid, obj);
        
        return (new StreamKiller (db, ((DXTickStream) obj).getKey ()));
    }
    
    private PreparedQuery    compileModifyStreamStatement (
        ModifyStreamStatement       s
    )
    {
        Identifier                          sid = s.id;
        Object                              obj = lookUpVariable (env, sid);
        
        if (!(obj instanceof DXTickStream))
            throw new IllegalObjectException (sid, obj);
        
        DXTickStream                        stream = (DXTickStream) obj;  
        Map <DataField, ModifyFieldData>    defaults = new HashMap <> ();
        StreamOptions                       optionsBean = new StreamOptions ();
        
        compileClassDefs (optionsBean, s.members, defaults);
                
        optionsBean.description = s.comment;
        optionsBean.name = s.title;
        optionsBean.scope = stream.getScope ();
        
        compileStreamOptions (s.options, optionsBean);

        StreamChangeTask change = StreamChangeProcessor.process(stream, optionsBean, defaults, s.confirm);
        
        return (new StreamModifier (db, stream.getKey (), change));
    }
    
    private PreparedQuery    compileCreateStreamStatement (
        CreateStreamStatement       s
    )
    {
        StreamOptions                       optionsBean = new StreamOptions ();
        
        compileClassDefs (optionsBean, s.members, null);
                
        optionsBean.description = s.comment;
        optionsBean.name = s.title;
        optionsBean.scope = s.scope;
        
        compileStreamOptions (s.options, optionsBean);
        
        return (new StreamCreator (db, s.id.id, optionsBean));
    }
            
    private void                    compileStreamOptions (
        OptionElement []                options,
        StreamOptions                   optionsBean
    )
    {
        OptionProcessor []  ops;
        
        switch (optionsBean.scope) {
            case DURABLE:   
                ops = StreamOptionsProcessors.DURABLE_STREAM_OPS; 
                break;
                
            case TRANSIENT: 
                optionsBean.bufferOptions = new BufferOptions ();
                ops = StreamOptionsProcessors.TRANSIENT_STREAM_OPS; 
                break;
                
            default: throw new UnsupportedOperationException (optionsBean.scope.name ());
        }
        
        xc.processOptions (ops, options, optionsBean);
    }

    private void                    compileClassDefs (
        StreamOptions                       optionsBean,
        ClassDef []                         members,
        Map <DataField, ModifyFieldData>    outDefaults
    )
    {
        RecordClassSet                      rcs = optionsBean.getMetaData ();                
        EnvironmentFrame                    senv = new EnvironmentFrame (env);
            
        for (ClassDef cdef : members) {
            ClassDescriptor     cd = compileClassDef (senv, cdef, outDefaults);                                    
            
            rcs.addClasses (cd);
            
            if (cd instanceof RecordClassDescriptor) {
                RecordClassDescriptor   rcd = (RecordClassDescriptor) cd;
                
                if (!rcd.isAbstract ())
                    rcs.addContentClasses (rcd);
                
                senv.bindNoDup (cdef.id, new ClassDataType (true, rcd));
            }        
            else if (cd instanceof EnumClassDescriptor) {
                EnumClassDescriptor     ecd = (EnumClassDescriptor) cd;
                
                senv.bindNoDup (cdef.id, new EnumDataType (true, ecd));
            }
            else
                throw new RuntimeException (cd.toString ());
        }
    }
    
    private ClassDescriptor compileClassDef (
        EnvironmentFrame                    senv, 
        ClassDef                            cdef,
        Map <DataField, ModifyFieldData>    outDefaults
    )
    {
        if (cdef instanceof RecordClassDef)
            return (compileRecordClassDef (senv, (RecordClassDef) cdef, outDefaults));
        
        if (cdef instanceof EnumClassDef)
            return (compileEnumClassDef ((EnumClassDef) cdef));
        
        throw new UnsupportedOperationException (cdef.toString ());
    }
    
    private RecordClassDescriptor compileRecordClassDef (
        EnvironmentFrame                    senv, 
        RecordClassDef                      cdef,
        Map <DataField, ModifyFieldData>    outDefaults
    )
    {
        boolean                 isAbstract = !cdef.instantiable;
        int                     numFields = cdef.attributes.length;
        DataField []            fields = new DataField [numFields];    
        TypeIdentifier          parentId = cdef.parent;
        RecordClassDescriptor   parent;
        
        if (parentId == null)
            parent = null;
        else {
            Object              type = lookUpType (senv, parentId);
            
            if (!(type instanceof ClassDataType))
                throw new IllegalSupertypeException (parentId);
            
            ClassDataType       cdt = (ClassDataType) type;
            
            if (!cdt.isFixed ())
                throw new IllegalSupertypeException (parentId);
            
            parent = cdt.getFixedDescriptor ();
        }
        
        for (int ii = 0; ii < numFields; ii++)
            fields [ii] = compileAttributeDef (senv, cdef.attributes [ii], outDefaults);
            
        RecordClassDescriptor   rcd =
            new RecordClassDescriptor (
                cdef.id.typeName, 
                cdef.title,
                isAbstract, 
                parent,
                fields
            );
        
        rcd.setDescription (cdef.comment);
        return (rcd);
    }
    
    private EnumClassDescriptor compileEnumClassDef (EnumClassDef cdef) {
        EnumValueDef []     valueDefs = cdef.values;
        int                 n = valueDefs.length;
        EnumValue []        values = new EnumValue [n];
        boolean             flags = cdef.isFlags;
        Set <String>        keys = new HashSet <String> ();
        
        for (int ii = 0; ii < n; ii++) {            
            EnumValueDef    evdef = valueDefs [ii];
            String          key = evdef.id.id;
            
            if (!keys.add (key))
                throw new DuplicateIdentifierException (evdef.id);
            
            long            nv;
            
            if (evdef.value == null)
                nv = flags ? 1 << ii : ii;
            else 
                nv = xc.computeStaticInt (evdef.value);
                        
            values [ii] = new EnumValue (key, nv);
        }
        
        EnumClassDescriptor     ecd =
            new EnumClassDescriptor (
                cdef.id.typeName, 
                cdef.title, 
                flags, 
                values
            );
        
        ecd.setDescription (cdef.comment);
        return (ecd);
    }
    
    private DataField                   compileAttributeDef (
        Environment                         senv, 
        AttributeDef                        adef,
        Map <DataField, ModifyFieldData>    outDefaults
    )
    {
        if (adef instanceof StaticAttributeDef)
            return (compileStaticAttributeDef (senv, (StaticAttributeDef) adef));
        
        if (adef instanceof NonStaticAttributeDef)
            return (compileNonStaticAttributeDef (senv, (NonStaticAttributeDef) adef, outDefaults));
        
        throw new UnsupportedOperationException (adef.toString ());
    }

    private DataType        lookUpDataType (
        Environment             senv, 
        TypeIdentifier          typeId
    )
    {
        Object              obj = lookUpType (senv, typeId);
        
        if (!(obj instanceof DataType))
            throw new IllegalObjectException (typeId, obj);
           
        return ((DataType) obj); 
    }
    
    private DataField       compileStaticAttributeDef (
        Environment                         senv, 
        StaticAttributeDef                  adef
    ) 
    {        
        DataType            type = compileDataTypeSpec (senv, adef.type);
        CompiledConstant    c = xc.computeStatic (adef.value, type);
        StaticDataField     sdf =
            new StaticDataField (adef.id, adef.title, type, type.toString (c.getValue()));
        
        sdf.setDescription (adef.comment);
        sdf.setAttributes(adef.tags);
        return (sdf);
    }

    private DataField       compileNonStaticAttributeDef (
        Environment                         senv, 
        NonStaticAttributeDef               adef,
        Map <DataField, ModifyFieldData>    outDefaults
    )
    {
        DataType            type = compileDataTypeSpec (senv, adef.type);
        
        Identifier          relativeId = adef.relativeId;
        NonStaticDataField  nsdf =
            new NonStaticDataField (
                adef.id, adef.title, 
                type, 
                false,
                relativeId == null ? null : relativeId.id
            );

        nsdf.setAttributes(adef.tags);
        nsdf.setDescription (adef.comment);
        
        if (outDefaults != null) {
            outDefaults.put (
                nsdf, 
                new ModifyFieldData (
                    adef.defval == null ? null : xc.computeStatic (adef.defval, type),
                    adef.location
                )
            );
        }
        else if (adef.defval != null) 
            throw new IllegalDefaultValueException (adef.defval);
            
        return (nsdf);
    }

    public DataType         compileTopDataTypeSpec (DataTypeSpec dts) {
        return (compileDataTypeSpec (env, dts));
    }

    private DataType        compileDataTypeSpec (
            Environment                         senv,
            DataTypeSpec                        dts
    )
    {
        if (dts instanceof SimpleDataTypeSpec)
            return (compileSimpleDataTypeSpec (senv, (SimpleDataTypeSpec) dts));

        if (dts instanceof ArrayDataTypeSpec)
            return (compileArrayDataTypeSpec (senv, (ArrayDataTypeSpec) dts));

        if (dts instanceof ClassDataTypeSpec)
            return (compileClassDataTypeSpec(senv, (ClassDataTypeSpec) dts));

        throw new UnsupportedOperationException (dts.toString ());
    }

    private DataType        compileArrayDataTypeSpec (Environment senv, ArrayDataTypeSpec dts) {
        DataType elementType;
        if (isDataTypeFixed(dts))
            elementType = compileDataTypeCheckAbstract(senv, dts.elementsTypeSpec[0]);
        else
            elementType = compilePolymorphicDataTypeSpec(senv, dts);

        return (new ArrayDataType(dts.nullable, elementType));
    }

    private DataType        compileClassDataTypeSpec(Environment senv, ClassDataTypeSpec dts) {
        if (isDataTypeFixed(dts))
            return compileFixedClassDataTypeSpec(senv, dts);
        else
            return compilePolymorphicDataTypeSpec(senv, dts);
    }

    private DataType        compileFixedClassDataTypeSpec(Environment senv, ClassDataTypeSpec dts) {
        DataTypeSpec elementDts = dts.elementsTypeSpec[0];
        DataType elementType = compileDataTypeCheckAbstract(senv, elementDts);

        if (!(elementType instanceof EnumDataType) && !(elementType instanceof ClassDataType))
            throw new IllegalTypeException(
                    elementDts, elementType.getClass(),
                    ClassDataType.class, EnumDataType.class);

        return elementType.nullableInstance(dts.nullable);
    }

    private DataType        compilePolymorphicDataTypeSpec(Environment senv, PolymorphicDataTypeSpec dts) {
        ArrayList<RecordClassDescriptor> descriptorsList = new ArrayList<>();

        for (int i = 0; i < dts.elementsTypeSpec.length; ++i) {
            DataTypeSpec spec = dts.elementsTypeSpec[i];
            DataType type = compileDataTypeCheckAbstract(senv, spec);

            if (type instanceof ClassDataType)
                descriptorsList.addAll(Arrays.asList(((ClassDataType) type).getDescriptors()));
            else
                throw new IllegalTypeException(spec, type.getClass(), ClassDataType.class);
        }

        return new ClassDataType(
                dts.nullable,
                descriptorsList.toArray(new RecordClassDescriptor[descriptorsList.size()]));
    }

    private boolean         isDataTypeFixed(PolymorphicDataTypeSpec dts) {
        return dts.elementsTypeSpec.length == 1;
    }

    private DataType        compileDataTypeCheckAbstract(Environment senv, DataTypeSpec dts) {
        DataType elementType = compileDataTypeSpec(senv, dts);
        checkDataTypeIsNotAbstract(dts, elementType);

        return elementType;
    }

    private void            checkDataTypeIsNotAbstract(DataTypeSpec dts, DataType type) {
        if (type instanceof ClassDataType) {
            for (RecordClassDescriptor descriptor : ((ClassDataType) type).getDescriptors()) {
                if (descriptor.isAbstract())
                    throw new IllegalAbstractDataType(dts, descriptor);
            }
        }
    }

    private DataType        compileSimpleDataTypeSpec (
        Environment                         senv, 
        SimpleDataTypeSpec                  dts
    )
    {
        DataType            type = lookUpDataType (senv, dts.typeId);        
        
        if (type instanceof IntegerDataType) 
            return (
                DataTypeCompiler.compileInteger (
                    dts, 
                    xc.computeStaticIntOrStar (dts.min), 
                    xc.computeStaticIntOrStar (dts.max)
                )
            );

        if (type instanceof FloatDataType) 
            return (
                DataTypeCompiler.compileFloat (
                    dts,
                    xc.computeStaticFloatOrStar (dts.min), 
                    xc.computeStaticFloatOrStar (dts.max)
                )
            );

        if (type instanceof VarcharDataType) 
            return (
                DataTypeCompiler.compileVarchar (dts)
            );

        if (dts.encoding != null)
            throw new IllegalEncodingException (dts);                        
        
        return (type.nullableInstance (dts.nullable));
    }        
}