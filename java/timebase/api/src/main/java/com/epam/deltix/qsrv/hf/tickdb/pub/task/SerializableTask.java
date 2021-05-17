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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class SerializableTask implements TransformationTask {

    public abstract void            write(DataOutputStream out) throws IOException;

    public abstract void            read(DataInputStream in) throws IOException;

    public final void               serialize(DataOutputStream out) throws IOException {
        out.writeUTF(getClass().getName());
        write(out);
    }

    public static SerializableTask  deserialize(DataInputStream in) throws Exception {
        String className = in.readUTF();

        SerializableTask task = (SerializableTask) Class.forName(className).newInstance();
        task.read(in);

        return task;
    }
}