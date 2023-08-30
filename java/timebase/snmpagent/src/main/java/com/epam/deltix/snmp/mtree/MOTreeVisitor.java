/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.snmp.mtree;

/**
 *
 */
public class MOTreeVisitor {
    static class NodeInfo {
        final NodeInfo      parent;
        final MONode        node;
        final Integer []    childIds;
        int                 pos = -1;
        final int           numChildren;
        
        NodeInfo (NodeInfo parent, int position) {
            this (
                parent, 
                position, 
                ((MOContainer) (parent.node)).getDirectChildById (parent.childIds [position])                
            );
        }
        
        NodeInfo (MONode node) {
            this (null, -1, node);
        }
        
        private NodeInfo (NodeInfo parent, int position, MONode node) {
            this.node = node;
            this.parent = parent;
            this.pos = position;
            
            if (node instanceof MOContainer) {
                childIds = ((MOContainer) node).getDirectChildIds ();
                numChildren = childIds.length;
            }
            else {
                childIds = null;
                numChildren = 0;
            }
        }
        
        private boolean     hasChildren () {
            return (numChildren > 0);
        }

        private NodeInfo    getFirstChild () {
            return (new NodeInfo (this, 0));
        }

        private boolean     hasNext () {
            return (parent != null && parent.numChildren > (pos + 1));
        }

        private NodeInfo    getNextSibling () {
            return (new NodeInfo (parent, pos + 1));
        }

        private NodeInfo    getParent () {
            return (parent);
        }
    }
    
    private NodeInfo            current;
    private int                 depthChange;
    private int                 depth;
    
    public MOTreeVisitor (MONode node) {
        current = new NodeInfo (node);
        depth = 0;
    }
    
    public boolean              next () {
        depthChange = 0;
        
        if (current.hasChildren ()) {
            current = current.getFirstChild ();
            depthChange++;
            depth++;
            return (true);
        }
        
        for (;;) {
            if (current.hasNext ()) {
                current = current.getNextSibling ();
                return (true);
            }
            
            current = current.getParent ();
            depthChange--;  
            depth--;
            
            if (current == null)
                return (false);
        }              
    }
    
    public MONode               getNode () {
        return (current.node);
    }
    
    public int                  getDepthChange () {
        return (depthChange);
    }
    
    public int                  getDepth () {
        return (depth);
    }
}