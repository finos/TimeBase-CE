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
package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.qsrv.hf.codec.CompilationUnit;
import com.epam.deltix.util.jcg.JClass;


public class CodecCacheValue {

    private JClass jClass;
    private CompilationUnit[] dependencies;

    public JClass getjClass() {
        return jClass;
    }

    public void setjClass(JClass jClass) {
        this.jClass = jClass;
    }

    public CompilationUnit[] getDependencies() {
        return dependencies;
    }

    public void setDependencies(CompilationUnit[] dependencies) {
        this.dependencies = dependencies;
    }

    public CodecCacheValue(JClass jClass, CompilationUnit[] dependencies) {
        this.jClass = jClass;
        this.dependencies = dependencies;
    }
}
