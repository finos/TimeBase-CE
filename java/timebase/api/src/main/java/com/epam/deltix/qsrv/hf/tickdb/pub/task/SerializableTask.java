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