package com.epam.deltix.qsrv.hf.codec.cg;

import java.util.*;

import com.epam.deltix.qsrv.hf.codec.ClassCodecFactory.Type;
import com.epam.deltix.qsrv.hf.codec.CompilationUnit;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.jcg.JClass;
import com.epam.deltix.util.lang.JavaCompilerHelper.SpecialClassLoader;

/**
 *
 */
public class CGContext {

    QVariableContainerLookup lookupContainer;
    final TypeLoader            typeLoader;
    final SpecialClassLoader    classLoader;

    ArrayList<CompilationUnit>        dependencies = new ArrayList<>();
    Map<CodecKey, CompilationUnit>    cache = new HashMap<>();

    public CGContext(TypeLoader typeLoader, SpecialClassLoader classLoader) {
        this.typeLoader = typeLoader;
        this.classLoader = classLoader;
    }

    public void addDependencies(CompilationUnit[] dependencies) {
        if (dependencies != null)
            this.dependencies.addAll(Arrays.asList(dependencies));
    }

    CompilationUnit lookup(RecordClassDescriptor cd, Type type) {
        return cache.get(new CodecKey(cd, type));
    }

    public void addCodec(CompilationUnit codec, RecordClassDescriptor cd, Type type) {
        dependencies.add(codec);
        cache.put(new CodecKey(cd, type), codec);
    }

    private static class CodecKey {
        private final RecordClassDescriptor     cd;
        private final Type                      type;

        private CodecKey(RecordClassDescriptor cd, Type type) {
            this.cd = cd;
            this.type = type;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass())
                return false;

            final CodecKey other = (CodecKey) obj;

            return (this.cd.equals(other.cd) && (this.type == other.type));
        }

        @Override
        public int hashCode() {
            return (cd.hashCode() + 59 * type.hashCode());
        }

        @Override
        public String toString() {
            return (cd.getName() + "#" + type);
        }
    }
}
