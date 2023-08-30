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
package com.epam.deltix.qsrv.dtb.store.dataacc;

import java.util.Comparator;

/**
 *
 */
class LinkComparatorByTime implements Comparator <AccessorBlockLink> {
    static final Comparator <AccessorBlockLink>     ASCENDING = new LinkComparatorByTime (true);
    static final Comparator <AccessorBlockLink>     DESCENDING = new LinkComparatorByTime (false);
    
    private final boolean       ascending;

    private LinkComparatorByTime (boolean ascending) {
        this.ascending = ascending;
    }
            
    @Override
    public int  compare (AccessorBlockLink a, AccessorBlockLink b) {
        long        ta = a.getNextTimestamp ();
        long        tb = b.getNextTimestamp ();
        
        if (ta == tb)
            return (0);
        
        return (ta < tb == ascending ? -1 : 1);
    }
}