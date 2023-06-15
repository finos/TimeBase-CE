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
package com.epam.deltix.snmp.smi;

import java.util.Collection;

/**
 *
 */
public interface SMIPrimitiveContainer extends SMIComplexNode {
    /**
     *  Specified oid hits an out-of-schema id along the way.
     */
    public static final int         MATCH_ERROR_UNKNOWN_ID = -1;
    
    /**
     *  Specified oid leads to a category, not a primitive node, and stops there.
     */
    public static final int         MATCH_ERROR_NON_PRIMITIVE_NODE = -2;
    
    /**
     *  Specified oid hits a table along the way.
     */
    public static final int         MATCH_ERROR_IS_A_TABLE = -3;        

    public Collection <SMINode>     children ();
    
    /**
     * Follow the path specified by <code>oid</code>, starting at <code>start</code>,
     * until it hits a Primitive node, returning the number of "extra" elements.
     * 
     * @param   oid     The oid.
     * 
     * @param   start   The start index, at which a child of this node should 
     *                  be looked up.
     * 
     * @return  The number of remaining entries in <code>oid</code> after the
     *          portion that leads to a Primitive node, or one of the 
     *          MATCH_ERROR constants. 
     */
    public int                      getIndexLength (SMIOID oid, int start);
}