package com.epam.deltix.snmp.mibc;

import com.epam.deltix.util.collections.*;
import java.util.*;
import java.io.IOException;

/**
 *
 */
public class OIDTreeNode {
    public static final int                 ROOT_ID = -1;
    
    private final int                       id;
    private CompiledObject                  object = null;
    private ArrayList <OIDTreeNode>         children = null;
        
    OIDTreeNode (int id) {
        this.id = id;
    }

    public int                  getId () {
        return id;
    }
    
    public CompiledObject       getObject () {
        return object;
    }

    public int                  getNumChildren () {
        return (children == null ? 0 : children.size ());
    }
    
    public OIDTreeNode          getChild (int idx) {
        return (children.get (idx));
    }
    
    void                        setObject (CompiledObject object) {
        this.object = object;
    }
        
    OIDTreeNode                 getOrCreateChild (int id) {
        int         pos;
        
        if (children == null) {
            pos = -1;
            children = new ArrayList <OIDTreeNode> ();
        }
        else
            pos = 
                BinarySearch2.binarySearch (
                    children, 
                    id, 
                    new Comparator2 <OIDTreeNode, Integer> () {
                        @Override
                        public int  compare (OIDTreeNode o1, Integer o2) {
                            return (o1.id - o2);
                        }                        
                    }
                );
        
        if (pos >= 0)
            return (children.get (pos));
        
        pos = -pos - 1;
        
        OIDTreeNode     node = new OIDTreeNode (id);

        children.add (pos, node);

        return (node);        
    }
    
    public void                 dump (String indent, Appendable out) 
        throws IOException         
    {
        if (id != ROOT_ID) {
            out.append (indent);
            out.append (object == null ? "?" : object.getId ());
            out.append (" [");
            out.append (String.valueOf (id));
            out.append ("]\n");
        }
        
        if (children != null) {
            String          sub = id == ROOT_ID ? indent : indent + " ";
            
            for (OIDTreeNode node : children)
                node.dump (sub, out);
        }            
    }

    @Override
    public String               toString () {
        StringBuilder   sb = new StringBuilder ();
        
        try {
            dump ("", sb);
        } catch (IOException iox) {
            throw new RuntimeException (iox);
        }
        
        return (sb.toString ());
    }        
}
