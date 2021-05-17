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
package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.tickdb.pub.query.*;
import java.util.*;

/**
 *  Common utility for filtering types. Requires external synchronization.
 */
public class HierarchicalTypeFilter implements TypeSubscriptionController {
    private Set <String>        allowedNames = null;
    private BitSet              allowedTypes = null;

    public boolean      accept (StreamMessageSource info) {
        throw new RuntimeException ();
    }

    public void         addTypes (String... names) {
        if (allowedNames == null)
            allowedNames = new HashSet <String> ();

        for (String s : names) {
            allowedNames.add (s);
        }
    }

    @Override
    public void setTypes(String... names) {
        if (allowedNames == null)
            allowedNames = new HashSet <String> ();
        else
            allowedNames.clear();

        allowedNames.addAll(Arrays.asList(names));
    }

    public void         removeTypes (String... names) {
        if (allowedNames == null)
            allowedNames = new HashSet <String> ();
        else {
            for (String s : names) {
                allowedNames.remove (s);
            }
        }
    }

    public void         subscribeToAllTypes () {
        allowedNames = null;
    }
}
