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
package com.epam.deltix.util.jcg.scg;

import com.epam.deltix.util.jcg.JClass;

/**
 *
 */
abstract class JMemberImpl implements JMemberIntf {
    protected final JContextImpl    context;
    private final int               modifiers;
    private final String            name;
    private final JClass            container;
    
    JMemberImpl (
        int                 modifiers,
        String              name,
        ClassImpl           container
    )
    {
        this.context = container.context;
        this.modifiers = modifiers;
        this.name = name;
        this.container = container;
    }

    JMemberImpl (
        JContextImpl        context,
        int                 modifiers,
        String              name
    )
    {
        this.context = context;
        this.modifiers = modifiers;
        this.name = name;
        this.container = null;
    }

    @Override
    public final String     name () {
        return (name);
    }

    @Override
    public final int        modifiers () {
        return modifiers;
    }

    @Override
    public final JClass     containerClass () {
        return container;
    }          
}
