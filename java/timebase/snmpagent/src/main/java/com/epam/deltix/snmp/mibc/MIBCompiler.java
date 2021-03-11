package com.epam.deltix.snmp.mibc;

import com.epam.deltix.snmp.parser.*;
import com.epam.deltix.snmp.parser.Module;
import com.epam.deltix.snmp.smi.*;
import java.io.*;

/**
 *
 */
public class MIBCompiler {
    private final ModuleRegistry        registry;
    private final OIDTreeNode           oidTree = 
        new OIDTreeNode (OIDTreeNode.ROOT_ID);
    
    public MIBCompiler (ModuleRegistry registry) {
        this.registry = registry;
    }
    
    public MIBCompiler () {
        this (ModuleRegistry.create ());
    }
    
    public void                         updateStandardOidTree () {
        updateOIDTree (StandardModules.SNMPv2_SMI);
    }
    
    public CompiledModule               load (File f) throws IOException {
        return (load (MIBParser.parse (f)));
    }
    
    public CompiledModule               load (Reader rd) throws IOException {
        return (load (MIBParser.parse (rd)));
    }
    
    public CompiledModule               load (Module pmod) {
        CompiledModuleImpl  cmod = new CompiledModuleImpl (registry, pmod);
        
        registry.register (cmod);
        
        updateOIDTree (cmod);
        
        return (cmod);
    }

    public OIDTreeNode                  getOidTree () {
        return oidTree;
    }

    public ModuleRegistry               getRegistry () {
        return registry;
    }
        
    private void                        updateOIDTree (CompiledModule cmod) {
        for (CompiledEntity e : cmod.entities ()) {
            if (!(e instanceof CompiledObject))
                continue;
            
            CompiledObject  cobj = (CompiledObject) e;            
            SMIOID          oid = cobj.getOid ();
            int             len = oid.getLength ();
            OIDTreeNode     node = oidTree;
            
            for (int ii = 0; ii < len; ii++) {
                int         id = oid.getId (ii);
                
                node = node.getOrCreateChild (id);
            }

            if (node.getObject () == null)
                node.setObject (cobj);
        }        
    }
    
    private void                        exportObjectTree (
        SMIComplexNode                      smiNode, 
        OIDTreeNode                         otn,
        CompiledObject []                   primaryIndexes
    )
    {
        int                         nc = otn.getNumChildren ();        
            
        for (int ii = 0; ii < nc; ii++) {
            OIDTreeNode             otc = otn.getChild (ii);            
            int                     indexDepth = -1;
            
            if (primaryIndexes != null) {
                for (int jj = 0; jj < primaryIndexes.length; jj++) {
                    if (primaryIndexes [jj].getOid ().getLast () == otc.getId ()) {
                        indexDepth = jj;
                        break;
                    }
                }
            }
            
            exportChild (smiNode, otc, indexDepth);
        }
    }
    
    private void                        exportTableEntry (
        SMITable                            table, 
        OIDTreeNode                         otn
    )
    {
        int         nc = otn.getNumChildren ();        
                   
        if (nc != 1)
            throw new IllegalArgumentException (
                "Table must have exactly one child: " + otn
            );

        OIDTreeNode     enode = otn.getChild (0);
        int             id = enode.getId ();
        
        if (id != 1)
            throw new IllegalArgumentException (
                "Table entry must have the id of 1; found " + id
            );
        
        CompiledObject      eco = enode.getObject ();
        CompiledIndexInfo   cii = eco.getIndexInfo ();
        String              name = eco.getId ();
        String              description = eco.getDescription ();        
        SMIRow              row;
        CompiledObject []   primaryIndexes = null;
        
        if (cii == null)
            throw new IllegalStateException ("No indexes in conceptual row " + enode);
        
        if (cii instanceof CompiledPrimaryIndexInfo) {
            CompiledPrimaryIndexInfo    cpi = (CompiledPrimaryIndexInfo) cii;
            
            primaryIndexes = cpi.getIndexedChildren ();
            
            row = 
                table.addIndexedRow (
                    id, 
                    name, 
                    description, 
                    primaryIndexes.length, 
                    cpi.isLastImplied ()
                );
        }
        else if (cii instanceof CompiledAugmentedIndexInfo) {

           CompiledAugmentedIndexInfo    cpi = (CompiledAugmentedIndexInfo) cii;

           row =
                table.addAugmentingRow (id, name, description, new SMIRowImpl (
                        (SMITableImpl) table,
                        new SMIOID (cpi.getAugmentedObject().getOid(), id),
                        name,
                        description,
                        ((CompiledPrimaryIndexBean)cpi.getAugmentedObject().getIndexInfo()).getIndexedChildren().length,
                        ((CompiledPrimaryIndexBean)cpi.getAugmentedObject().getIndexInfo()).isLastImplied()
                ));
        }
        else
            throw new UnsupportedOperationException (cii.toString ());
        
        exportObjectTree (row, enode, primaryIndexes);
    }
    
    private void                        exportChild (
        SMIComplexNode                      parent, 
        OIDTreeNode                         onode,
        int                                 indexDepth
    )
    {                
        CompiledObject      co = onode.getObject ();         
        int                 id = onode.getId ();
        String              name = co == null ? null : co.getId ();
        SMIType             type = co == null ? null : co.getType ();

        if (type == null) {
            SMICategory     childCat = ((SMICategory) parent).addObjectIdentifier (id, name);

            exportObjectTree (childCat, onode, null);
        }
        else if (type instanceof SMITableType) {
            SMITable        childTable = 
                ((SMICategory) parent).addTable (id, name, co.getDescription ());
            
            exportTableEntry (childTable, onode);
        }
        else {
            int             nc = onode.getNumChildren ();
            
            if (nc != 0)
                throw new IllegalArgumentException (
                    "Primitive node may not have children: " + onode
                );
            
            if (parent instanceof SMIRow)
                ((SMIRow) parent).addObjectType (
                    id, 
                    name, 
                    type, 
                    co.getAccess (), 
                    co.getDescription (),
                    indexDepth
                );
            else if (parent instanceof SMICategory)                
                ((SMICategory) parent).addObjectType (
                    id, 
                    name, 
                    type, 
                    co.getAccess (), 
                    co.getDescription ()
                );
            else
                throw new UnsupportedOperationException (parent.toString ());
        }
        
    }
    
    public SMICategory                 exportObjectTree () {
        SMICategory     root = SMISchema.createRoot ();
        
        exportObjectTree (root, oidTree, null);
        
        return (root);
    }
}
