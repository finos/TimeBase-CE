package com.epam.deltix.qsrv.hf.codec;

import com.epam.deltix.util.jcg.JClass;

/**
 *
 */
public class CompilationUnit {
    private final JClass jClass;
    private final Class<?>[] depClasses;
    private final CompilationUnit[] dependencies;

    public CompilationUnit(JClass jClass, Class<?>[] depClasses) {
        this(jClass, depClasses, null);
    }

    public CompilationUnit(JClass jClass, Class<?>[] depClasses, CompilationUnit[] dependencies) {
        this.jClass = jClass;
        this.depClasses = depClasses;
        this.dependencies = dependencies;
    }

    public CompilationUnit[] getDependencies() {
        return dependencies;
    }

    public JClass getJClass() {
        return jClass;
    }
}
