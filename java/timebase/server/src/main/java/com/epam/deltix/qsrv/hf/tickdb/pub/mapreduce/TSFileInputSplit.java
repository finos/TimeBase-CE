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
package com.epam.deltix.qsrv.hf.tickdb.pub.mapreduce;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.InputSplit;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TSFileInputSplit extends InputSplit implements Writable {
    private List<Path>                    files = new ArrayList<>();
    private long                          length;
    private String[]                      hosts;

    public TSFileInputSplit() {
    }

    public void             addFile(Path file, long length) throws IOException, InterruptedException {
        this.files.add(file);
        this.length += length;
    }

    public void             setHosts(String[] hosts) {
        this.hosts = hosts;
    }

    public Path[]           getFiles() {
        return files.toArray(new Path[files.size()]);
    }

    @Override
    public long             getLength() throws IOException, InterruptedException {
        return length;
    }

    @Override
    public String[]         getLocations() throws IOException, InterruptedException {
        return hosts;
    }

    @Override
    public void             write(DataOutput out) throws IOException {
        out.writeInt(files.size());
        for (Path path : files)
            Text.writeString(out, path.toString());

        out.writeLong(length);

        out.writeInt(hosts.length);
        for (String host : hosts)
            Text.writeString(out, host);
    }

    @Override
    public void             readFields(DataInput in) throws IOException {
        int num = in.readInt();
        files.clear();
        for (int i = 0; i < num; ++i)
            files.add(new Path(Text.readString(in)));

        length = in.readLong();

        num = in.readInt();
        hosts = new String[num];
        for (int i = 0; i < num; ++i)
            hosts[i] = Text.readString(in);
    }

}
