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
package com.epam.deltix.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.FixedBoundEncoder;
import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.JavaBeanGenerator;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.memory.MemoryDataOutput;
import org.junit.Test;

/**
 * Created by Alex Karpovich on 10/22/2018.
 */
public class Test_JavaBeanGenerator {

    @Test
    public void test() throws Throwable {
        final String testPackage = "gen.blah";

        Introspector ix = StreamConfigurationHelper.createMessageIntrospector ();

        JavaBeanGenerator jbg = new JavaBeanGenerator ();

        jbg.setDefaultPackage (testPackage);

        RecordClassDescriptor l2m_rcd = null;

        for (RecordClassDescriptor rcd : ix.getRecordClasses ()) {
            jbg.addClass (rcd);

            if (rcd.getName ().endsWith ("BarMessage"))
                l2m_rcd = rcd;
        }

        jbg.process ();

        for (JavaBeanGenerator.Bean bean : jbg.beans ()) {
            System.out.println ("#########################################\n" + bean.getSourceCode ());
        }

        final ClassLoader cl = jbg.compile ();

//        Class <?>           bua_class = cl.loadClass (testPackage + ".BookUpdateAction");
//        Object              bua_value = bua_class.getDeclaredField ("UPDATE").get (null);
        Class <?>           l2m_class = cl.loadClass (testPackage + ".BarMessage");
        Object              l2m = l2m_class.newInstance ();

        //l2m_class.getDeclaredField ("action").set (l2m, BookUpdateAction.UPDATE);

        FixedBoundEncoder enc =
                CodecFactory.INTERPRETED.createFixedBoundEncoder (
                        jbg.getTypeLoader (),
                        l2m_rcd
                );

        MemoryDataOutput mdo = new MemoryDataOutput ();

        enc.encode (l2m, mdo);

    }
}