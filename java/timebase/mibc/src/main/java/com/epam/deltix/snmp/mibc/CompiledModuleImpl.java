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
package com.epam.deltix.snmp.mibc;

import com.epam.deltix.snmp.mibc.errors.CircularReferenceException;
import com.epam.deltix.snmp.parser.*;
import com.epam.deltix.snmp.parser.Module;
import com.epam.deltix.snmp.smi.*;

import com.epam.deltix.util.collections.generated.IntegerArrayList;
import java.util.Collection;

/**
 *
 */
class CompiledModuleImpl implements CompiledModule {
    private Env <CompiledEntity>        env = 
        new Env <CompiledEntity> ();    
    
    private final ModuleRegistry                reg;
    private final String                        moduleId;
    
    public CompiledModuleImpl (ModuleRegistry reg, Module pmod) {
        env.register (new StandardObjectIdBean ("iso", 1));
        
        this.reg = reg;
         
        moduleId = pmod.id;
        
        if (pmod.imports != null)
            for (Import imp : pmod.imports)
                processImport (imp);
        
        for (Definition pd : pmod.definitions) 
            registerDefinition (pd);
                
        for (CompiledEntity e : env.values ()) {
            if (e instanceof CompiledObjectBean) {
                CompiledObjectBean  cobj = (CompiledObjectBean) e;
                
                computeValue (cobj);
                
                if (cobj.def instanceof ObjectTypeDefinition) 
                    processObjectType (cobj);                
            }
            else if (e instanceof CompiledTypeBean)
                computeType ((CompiledTypeBean) e);
        }                
    }

    @Override
    public String               getId () {
        return (moduleId);
    }
        
    private void                registerDefinition (Definition pd) {
        CompiledEntityBean          bean;
        
        if (pd instanceof ObjectDefinition) 
            bean = new CompiledObjectBean ((ObjectDefinition) pd);        
        else if (pd instanceof TypeDefinition) 
            bean = new CompiledTypeBean ((TypeDefinition) pd);  
        else if (pd instanceof MacroDefinition)
            bean = new CompiledMacroBean ((MacroDefinition) pd);
        else 
            throw new UnsupportedOperationException (pd.getClass ().getName ());
        
        env.register (pd.location, bean);
    }

    private CompiledObject      resolveObject (long refLocation, String objName) {
        if (!Character.isLowerCase (objName.charAt (0)))
            throw new IllegalArgumentException (objName);
        
        CompiledEntity  e = resolve (refLocation, objName);
        // should not mix up types because ids are categorized by first letter
        return ((CompiledObject) e);
    }
    
    private CompiledType        resolveType (long refLocation, String objName) {
        if (!Character.isUpperCase (objName.charAt (0)))
            throw new IllegalArgumentException (objName);
        
        CompiledEntity  e = resolve (refLocation, objName);
        // should not mix up types because ids are categorized by first letter
        return ((CompiledType) e);
    }
    
    private SMIType             resolveAndCompileType (long refLocation, String objName) {
        if (objName.equals ("OCTET STRING"))
            return (SMIOctetStringType.INSTANCE);
        
        if (objName.equals ("INTEGER"))
            return (SMIIntegerType.INSTANCE);
        
        if (objName.equals ("OBJECT IDENTIFIER"))
            return (SMIObjectIdentifierType.INSTANCE);
        
        CompiledType        refType = resolveType (refLocation, objName);
               
        if (refType instanceof CompiledTypeBean)
            computeType ((CompiledTypeBean) refType);

        return (refType.getType ());
    }
    
    @Override
    public CompiledEntity       resolve (long refLocation, String objName) {
        return (env.resolve (refLocation, objName));
    }
    
    @Override
    public Collection <CompiledEntity>      entities () {
        return (env.values ());
    }
    
    private void                computeType (CompiledTypeBean ctype) {
        switch (ctype.state) {
            case INIT:      
                ctype.state = CompiledEntityBean.RefState.RESOLVING;
                break;
                
            case RESOLVING: 
                throw new CircularReferenceException (ctype.def);
                
            case RESOLVED:  
                return;
        }
        
        TypeDefinition      tdef = ctype.def;
        SMIType             smiType;
        
        if (tdef instanceof TypeAssignment) {
            TypeAssignment  ta = (TypeAssignment) tdef;            
            Type            ptype = ta.value;
            
            smiType = compileType (ptype);                
        }
        else
            throw new UnsupportedOperationException (tdef.toString ());
        
        ctype.type = new SMINamedType (smiType, tdef.id);
        ctype.state = CompiledEntityBean.RefState.RESOLVED;
    }
    
    private SMINameNumberPair []            compilePairs (NameNumberPair [] p) {
        int                     n = p.length;
        SMINameNumberPair []    ret = new SMINameNumberPair [n];
        
        for (int ii = 0; ii < n; ii++) {
            NameNumberPair      pnn = p [ii];
            
            //TODO: ERROR CHECKING
            
            ret [ii] = new SMINameNumberPair (pnn.name, pnn.numericId);
        }
        
        return (ret);
    }
    
    private SMIField []                     compileFields (Field [] p) {
        int                     n = p.length;
        SMIField []             ret = new SMIField [n];
        
        for (int ii = 0; ii < n; ii++) {
            Field               pf = p [ii];
            
            //TODO: ERROR CHECKING
            
            ret [ii] = new SMIField (pf.name, compileType (pf.type));
        }
        
        return (ret);
    }
    
    private SMIConstraint       compileConstraint (Constraint cons) {
        if (cons instanceof ValueListConstraint) {
            ValueListConstraint     vlc = (ValueListConstraint) cons;
            
            return (new SMISetConstraint (vlc.values));
        }
        
        if (cons instanceof SizeConstraint) {
            SizeConstraint          sc = (SizeConstraint) cons;
            
            return (new SMISizeConstraint (compileConstraint (sc.constraint)));
        }
            
        if (cons instanceof RangeConstraint) {
            RangeConstraint         rc = (RangeConstraint) cons;
            
            return (new SMIRangeConstraint (rc.from, rc.to));
        }
        
        throw new UnsupportedOperationException (cons.toString ());
    }
    
    private SMIType             compileType (Type ptype) {
        if (ptype instanceof BitsType) {
            BitsType            bt = (BitsType) ptype;
            
            return (new SMIBitsType (compilePairs (bt.components)));
        }
        
        if (ptype instanceof ChoiceType) {
            ChoiceType          st = (ChoiceType) ptype;
            
            return (new SMIChoiceType (compileFields (st.fields)));
        }
        
        if (ptype instanceof ConstrainedType) {
            ConstrainedType     ct = (ConstrainedType) ptype;            
            SMIType             t = compileType (ct.baseType);
            
            return (new SMIConstrainedType (t, compileConstraint (ct.constraint)));
        }
        
        if (ptype instanceof NamedType) {
            NamedType           nt = (NamedType) ptype;           
            
            return (resolveAndCompileType (nt.location, nt.typeId));
        }
        
        if (ptype instanceof EnumeratedType) {
            EnumeratedType      et = (EnumeratedType) ptype;
            
            return (new SMIEnumeratedType (compilePairs (et.components)));
        }
        
        if (ptype instanceof TextualConvention) {
            TextualConvention   tc = (TextualConvention) ptype;
            
            return (
                new SMITextualConvention (
                    compileType (tc.syntax), 
                    tc.hint, 
                    tc.status, 
                    tc.description, 
                    tc.reference
                )
            );
        }
        
        if (ptype instanceof SequenceOfType) {
            SequenceOfType      st = (SequenceOfType) ptype;
            SMIType             elemType = 
                resolveAndCompileType (st.location, st.elementTypeId);
            
            while (elemType instanceof SMINamedType)
                elemType = ((SMINamedType) elemType).getBase ();
            
            return (new SMITableType ((SMIStructureType) elemType));
        }
        
        if (ptype instanceof StructureType) {
            StructureType       st = (StructureType) ptype;
            
            return (new SMIStructureType (compileFields (st.fields)));
        }
         
        throw new UnsupportedOperationException (ptype.toString ());
    }
    
    private void                computeValue (CompiledObjectBean cobj) {
        switch (cobj.state) {
            case INIT:      
                cobj.state = CompiledEntityBean.RefState.RESOLVING;
                break;
                
            case RESOLVING: 
                throw new CircularReferenceException (cobj.def);
                
            case RESOLVED:  
                return;
        }
                                   
        // allocate new, because the buffer escapes!
        IntegerArrayList    ids = new IntegerArrayList (); 

        for (NameNumberPair nnp : cobj.def.value.components) {
            if (nnp.numericId != null)
                ids.add (nnp.numericId.intValue ());
            else {
                CompiledObject      refObj = resolveObject (nnp.location, nnp.name);
                
                if (refObj instanceof CompiledObjectBean)
                    computeValue ((CompiledObjectBean) refObj);
                
                SMIOID              refVal = refObj.getOid ();

                for (int ii = 0; ii < refVal.getLength (); ii++)
                    ids.add (refVal.getId (ii));
            }
        }

        cobj.oid = new SMIOID (ids.getInternalBuffer (), 0, ids.size ());    
        cobj.state = CompiledEntityBean.RefState.RESOLVED;
    }

    private void                processObjectType (CompiledObjectBean cobj) {        
        ObjectTypeDefinition        def = (ObjectTypeDefinition) cobj.def;
        IndexInfo                   iinfo = def.index;
        
        if (iinfo != null) {
            if (iinfo instanceof PrimaryIndexInfo) {
                PrimaryIndexInfo    pii = (PrimaryIndexInfo) iinfo;
                int                 num = pii.columnIds.length;
                CompiledObject []   columns = new CompiledObject [num];
                
                for (int ii = 0; ii < num; ii++)
                    columns [ii] = 
                        resolveObject (pii.location, pii.columnIds [ii]);
                
                cobj.indexInfo = 
                    new CompiledPrimaryIndexBean (pii.lastIsImplied, columns);                             
            }
            else if (iinfo instanceof AugmentedIndexInfo) {
                AugmentedIndexInfo  aii = (AugmentedIndexInfo) iinfo;
                
                cobj.indexInfo = 
                    new CompiledAugmentedIndexBean (
                        resolveObject (aii.location, aii.refId)
                    );
            }
            else
                throw new UnsupportedOperationException (iinfo.toString ());
        }
        
        cobj.type = compileType (def.syntax);
    }

    private void                processImport (Import imp) {
        CompiledModule      cmod = reg.getModule (imp.location, imp.moduleId);
        
        for (String id : imp.symbols) {
            CompiledEntity  e = cmod.resolve (imp.location, id);
            
            env.register (imp.location, e);
        }            
    }

}