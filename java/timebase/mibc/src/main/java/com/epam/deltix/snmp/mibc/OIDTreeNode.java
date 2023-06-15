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