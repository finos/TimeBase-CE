package com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron;

import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry.IdGenerator;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

/**
 * @author Alexei Osipov
 */
public class FileBasedIdGeneratorTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void test() throws Exception {
        File file = folder.newFolder("tempQShome");
        QSHome.set(file.getPath());

        IdGenerator idGenerator = FileBasedIdGenerator.createFileBasedIdGenerator("200-1000");
        int val1 = idGenerator.nextId();
        int val2 = idGenerator.nextId();
        Assert.assertEquals(200, val1);
        Assert.assertEquals(201, val2);
    }
}