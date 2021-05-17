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
package com.epam.deltix.qsrv.hf.tickdb.pub.task;

import com.epam.deltix.qsrv.hf.pub.util.SerializationUtils;
import com.epam.deltix.qsrv.hf.tickdb.pub.mapreduce.*;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.io.IOUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;

import java.io.*;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.zip.ZipOutputStream;

public class MapReduceTask extends SerializableTask {

    public static final String      MAP_CLASS_NAME = "mapreduce.job.map.class";
    public static final String      REDUCE_CLASS_NAME = "mapreduce.job.reduce.class";
    public static final String      COMBINE_CLASS_NAME = "mapreduce.job.combine.class";
    public static final String      PARTITIONER_CLASS_NAME = "mapreduce.job.partitioner.class";

    public static final String      OUTPUT_KEY_CLASS = "mapreduce.job.output.key.class";
    public static final String      OUTPUT_VALUE_CLASS = "mapreduce.job.output.value.class";

    public static final String      JOB_JAR_NAME = "job.jar";

    public String                   name;
    public final Configuration      config = new Configuration();

    public String                   mapper;
    public String                   reducer;
    public String                   combiner;
    public String                   partitioner;

    public String                   outputKeyClass;
    public String                   outputValueClass;
    public int                      numReduceTasks;

    public String                   tickdbUrl;
    public String                   outputStreamKey;

    public boolean                  isBackground;

    protected Map<String, byte[]>   sources = new HashMap<>();
    public DataFilter               filter;

    public MapReduceTask() {
        add3rdPartyJars();
    }

    public MapReduceTask(Class<? extends MessageMapper> mapper, Class<? extends MessageReducer> reducer) {
        this(mapper, reducer, null);
    }

    public MapReduceTask(Class<? extends MessageMapper> mapper, Class<? extends MessageReducer> reducer, Class<? extends DataReducer> combiner) {
        this.mapper = mapper.getName();
        this.reducer = reducer != null ? reducer.getName() : null;
        this.combiner = combiner != null ? combiner.getName() : null;

        this.name = mapper.getSimpleName();

        Type[] types = ((ParameterizedType) mapper.getGenericSuperclass()).getActualTypeArguments();
        assert types.length >= 2;
        outputKeyClass = ((Class) types[0]).getName();
        outputValueClass = ((Class) types[1]).getName();
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(name);
        out.writeUTF(mapper);
        SerializationUtils.writeNullableString(reducer, out);
        SerializationUtils.writeNullableString(combiner, out);
        out.writeUTF(outputKeyClass);
        out.writeUTF(outputValueClass);

        out.writeInt(numReduceTasks);

        SerializationUtils.writeNullableString(tickdbUrl, out);
        SerializationUtils.writeNullableString(outputStreamKey, out);

        out.writeBoolean(isBackground);

        out.writeInt(sources.size());
        for (Map.Entry<String, byte[]> src : sources.entrySet()) {
            out.writeUTF(src.getKey());
            out.writeInt(src.getValue().length);
            out.write(src.getValue());
        }

        config.write(out);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        name = in.readUTF();
        mapper = in.readUTF();
        reducer = SerializationUtils.readNullableString(in);
        combiner = SerializationUtils.readNullableString(in);

        outputKeyClass = in.readUTF();
        outputValueClass = in.readUTF();

        numReduceTasks = in.readInt();

        tickdbUrl = SerializationUtils.readNullableString(in);
        outputStreamKey = SerializationUtils.readNullableString(in);

        isBackground = in.readBoolean();

        int numSources = in.readInt();
        for (int i = 0; i < numSources; ++i) {
            String jarName = in.readUTF();
            byte src[] = new byte[in.readInt()];
            in.readFully(src);

            sources.put(jarName, src);
        }

        config.readFields(in);
    }

    public OutputReader getOutputReader() throws IOException {
        return new OutputReader(FileSystem.get(config), config, "/jobs/" + name + "/out/");
    }

    @Override
    public boolean isBackground() {
        return isBackground;
    }

    /*
        Set location (folder) of all Map/Reduce related classes.
     */
    public void         setClassesLocation(File location) throws IOException, InterruptedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ZipOutputStream zos =
            new ZipOutputStream (new BufferedOutputStream (out));

        zos.setLevel (9);

        File[] files = location.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".class");
            }
        });

        for (File file : files)
            IOUtil.addFileToZip (file, zos, file.getPath());

        zos.finish ();
        zos.close ();

        sources.put(JOB_JAR_NAME, out.toByteArray());
    }

    /*
        Set location of jar file, containing all Map/Reduce classes and related.
    */
    public void         setJobJar(File jar) throws IOException {
        sources.put(JOB_JAR_NAME, IOUtil.readBytes(jar));
    }

    public void         addJar(File jar) throws IOException {
        sources.put(jar.getName(), IOUtil.readBytes(jar));
    }

    public Set<String>  getJarNames() {
        return sources.keySet();
    }

    public void         writeJar(String name, OutputStream out) throws IOException {
        byte[] source = sources.get(name);
        if (source != null)
            out.write(source);
    }

    private void        add3rdPartyJars() {
        try {
            addJar(Home.getFile("lib", "gflog.jar"));
            addJar(Home.getFile("lib", "disruptor-3.3.6.jar"));
            addJar(Home.getFile("lib", "lz4-1.3.0.jar"));
            addJar(Home.getFile("lib", "snappy-java-1.1.0.jar"));
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}
