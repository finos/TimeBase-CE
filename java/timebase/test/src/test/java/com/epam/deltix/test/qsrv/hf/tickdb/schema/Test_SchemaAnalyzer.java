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
package com.epam.deltix.test.qsrv.hf.tickdb.schema;

import com.epam.deltix.qsrv.hf.tickdb.schema.*;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.SchemaChangeTask;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.TransformationContext;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.StringWriter;
import java.io.StringReader;
import java.util.*;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

@Category(TickDBFast.class)
public class Test_SchemaAnalyzer {

    public RecordClassSet getLeft4test1() {
        RecordClassSet s1 = new RecordClassSet();
        RecordClassDescriptor marketMsgDescriptor =
            StreamConfigurationHelper.mkMarketMessageDescriptor (null);

        RecordClassDescriptor rd1 = StreamConfigurationHelper.mkBarMessageDescriptor(marketMsgDescriptor,
                null, null, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO);
        RecordClassDescriptor rd2 = StreamConfigurationHelper.mkBBOMessageDescriptor(marketMsgDescriptor,
	            true, null, null, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO);
        s1.addContentClasses(rd1, rd2);
        
        return s1;
    }

    public RecordClassSet getRight4test1() {
       RecordClassSet s2 = new RecordClassSet();
        RecordClassDescriptor d2 = StreamConfigurationHelper.mkBarMessageDescriptor(null,
            "", 999, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO);
        s2.addContentClasses(d2);

        return s2;
    }
    
    @Test
    public void test1() {
        RecordClassSet left = getLeft4test1();
        RecordClassSet right = getRight4test1();

        MetaDataChange metaDataChange = SchemaAnalyzer.DEFAULT.getChanges(
                left, MetaDataChange.ContentType.Polymorphic,
                right, MetaDataChange.ContentType.Fixed);
        
        assertEquals(metaDataChange.changes.size(), 2);
        assertEquals(metaDataChange.changes.get(0).getChanges().length, 2);
        assertEquals(metaDataChange.changes.get(0).getChanges()[0].getClass(), FieldModifierChange.class);
        assertEquals(metaDataChange.changes.get(0).getChanges()[1].getClass(), FieldModifierChange.class);
    }

    public RecordClassSet getLeft4test2() {

        RecordClassSet set = new RecordClassSet();

        String priceEncoding = FloatDataType.ENCODING_SCALE_AUTO;

        final DataField[]      fields = {            
            new NonStaticDataField ("close", "Close", new FloatDataType (priceEncoding, true), "open"),
            new NonStaticDataField ("open", "Open", new FloatDataType (priceEncoding, true)),
            new NonStaticDataField ("high", "High", new FloatDataType (priceEncoding, true), "open"),
            new NonStaticDataField ("low", "Low", new FloatDataType (priceEncoding, true), "open"),
            new NonStaticDataField ("volume", "Volume", new FloatDataType (priceEncoding, true))
        };

        String name = BarMessage.class.getName();

        set.addContentClasses(new RecordClassDescriptor(name, name, false,null, fields));

        return set;
    }

    public RecordClassSet getRight4test2() {

        RecordClassSet s2 = new RecordClassSet();
        RecordClassDescriptor d2 = StreamConfigurationHelper.mkBarMessageDescriptor(null,
                "", 999, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO);
        s2.addContentClasses(d2);

        return s2;
    }

    @Test
    public void test2() {
        RecordClassSet left = getLeft4test2();
        RecordClassSet right = getRight4test2();

        MetaDataChange metaDataChange = SchemaAnalyzer.DEFAULT.getChanges(
                left, MetaDataChange.ContentType.Fixed,
                right, MetaDataChange.ContentType.Fixed);
    }

     public RecordClassSet getLeft4test3() {
        RecordClassSet s2 = new RecordClassSet();
        RecordClassDescriptor d2 = StreamConfigurationHelper.mkBarMessageDescriptor(null,
                "", 840, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO);
        s2.addContentClasses(d2);

        return s2;
    }

    public RecordClassSet getRight4test3() {
        RecordClassSet s2 = new RecordClassSet();
        RecordClassDescriptor d2 = StreamConfigurationHelper.mkBarMessageDescriptor(null,
                "", null, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO);
        s2.addContentClasses(d2);

        return s2;
    }

    @Test
    public void test3() {
        RecordClassSet left = getLeft4test3();
        RecordClassSet right = getRight4test3();

        MetaDataChange metaDataChange = SchemaAnalyzer.DEFAULT.getChanges(
                left, MetaDataChange.ContentType.Fixed,
                right, MetaDataChange.ContentType.Fixed);

        assertEquals(metaDataChange.changes.size(), 1);
        assertEquals(metaDataChange.changes.get(0).getChanges().length, 1);
        assertEquals(metaDataChange.changes.get(0).getChanges()[0].getClass(), FieldModifierChange.class);        
    }

    @Test
    public void test4() {
        RecordClassSet s1 = new RecordClassSet();

        String priceEncoding = FloatDataType.ENCODING_SCALE_AUTO;

         final DataField[]      fields1 = {
            new NonStaticDataField ("close", "Close", new FloatDataType (priceEncoding, true)),
            new NonStaticDataField ("open", "Open", new FloatDataType (priceEncoding, true))
         };
        String name = "myClass";

        s1.addContentClasses(new RecordClassDescriptor(name, name, false, null, fields1));

        RecordClassSet s2 = new RecordClassSet();        

         final DataField[]      fields2 = {            
            new NonStaticDataField ("close1", "Close1", new FloatDataType (priceEncoding, true)),
            new NonStaticDataField ("open", "Open", new FloatDataType (priceEncoding, true))
         };        

        s2.addContentClasses(new RecordClassDescriptor(name, name, false, null, fields2));

        SchemaMapping mapping = new SchemaMapping();
        mapping.fields.put(fields1[0], fields2[0]);
        SchemaAnalyzer analyzer = new SchemaAnalyzer(mapping);

        StreamMetaDataChange change = analyzer.getChanges(s1, MetaDataChange.ContentType.Fixed,
                s2, MetaDataChange.ContentType.Fixed);
        assertEquals(1, change.changes.size());
        assertEquals(SchemaChange.Impact.None, change.getChangeImpact());
    }

    @Test
    public void test41() {
        RecordClassSet s1 = new RecordClassSet();

        String priceEncoding = FloatDataType.ENCODING_SCALE_AUTO;

        final DataField[]      fields1 = {
                new NonStaticDataField ("close", "Close", new FloatDataType (priceEncoding, true)),
                new NonStaticDataField ("open", "Open", new FloatDataType (priceEncoding, true))
        };
        String name = "myClass";

        s1.addContentClasses(new RecordClassDescriptor(name, name, false, null, fields1));

        RecordClassSet s2 = new RecordClassSet();

        final DataField[]      fields2 = {
                new NonStaticDataField ("open", "Open", new FloatDataType (priceEncoding, true)),
                new NonStaticDataField ("close1", "Close1", new FloatDataType (priceEncoding, true))
        };

        s2.addContentClasses(new RecordClassDescriptor(name, name, false, null, fields2));

        SchemaMapping mapping = new SchemaMapping();
        mapping.fields.put(fields1[0], fields2[0]);
        SchemaAnalyzer analyzer = new SchemaAnalyzer(mapping);

        StreamMetaDataChange change = analyzer.getChanges(s1, MetaDataChange.ContentType.Fixed,
                s2, MetaDataChange.ContentType.Fixed);
        assertEquals(1, change.changes.size());
        assertEquals(SchemaChange.Impact.DataConvert, change.getChangeImpact());
    }

    @Test
    public void test5() {
        RecordClassSet s1 = new RecordClassSet();

        String priceEncoding = FloatDataType.ENCODING_SCALE_AUTO;

         final DataField[]      fields1 = {
            new NonStaticDataField ("close", "Close", new FloatDataType (priceEncoding, true)),
            new NonStaticDataField ("open", "Open", new FloatDataType (priceEncoding, true))
         };
        String name = "myClass";

        RecordClassDescriptor parent = new RecordClassDescriptor(name, name, false, null, fields1);
        s1.addContentClasses(parent);

        RecordClassSet s2 = new RecordClassSet();

         final DataField[]      fields2 = {
            new NonStaticDataField ("close1", "Close1", new FloatDataType (priceEncoding, true)),
         };

        RecordClassDescriptor child = new RecordClassDescriptor(name, name, false, parent, fields2);
        s2.addContentClasses(child);

        SchemaMapping mapping = new SchemaMapping();
        mapping.descriptors.put(parent.getGuid(), child.getGuid());
        SchemaAnalyzer analyzer = new SchemaAnalyzer(mapping);

        StreamMetaDataChange change = analyzer.getChanges(s1, MetaDataChange.ContentType.Fixed,
                s2, MetaDataChange.ContentType.Fixed);
        assertEquals(1, change.changes.size());
        assertEquals(SchemaChange.Impact.None, change.getChangeImpact());
    }

    @Test
    public void Test_Serialize()  throws Exception {

        RecordClassSet left = new RecordClassSet();
        List<RecordClassDescriptor> descriptors =
                new ArrayList<>(Arrays.asList(StreamConfigurationHelper.mkUniversalMarketDescriptors()));
        left.addContentClasses(descriptors.toArray(new RecordClassDescriptor[descriptors.size()]));
        
        RecordClassSet right = getRight4test3();

        SchemaChangeTask task = new SchemaChangeTask(SchemaAnalyzer.DEFAULT.getChanges(
            left, MetaDataChange.ContentType.Fixed,
            right, MetaDataChange.ContentType.Fixed));

        task.change.mapping.descriptors.put("aaa", "bbb");
        task.change.mapping.fields.put(
                new NonStaticDataField ("any", "any", new FloatDataType (FloatDataType.ENCODING_SCALE_AUTO, true)),
                new NonStaticDataField ("any1", "any1", new FloatDataType (FloatDataType.ENCODING_SCALE_AUTO, true)));

        Marshaller m = TransformationContext.createMarshaller(task);
        StringWriter s = new StringWriter ();
        m.marshal (task, s);

        Unmarshaller u = TransformationContext.createUnmarshaller(task);
        SchemaChangeTask result = (SchemaChangeTask) u.unmarshal(new StringReader(s.toString()));

        assertEquals(result.change.mapping.fields.size(), 1);
        assertEquals(result.change.mapping.descriptors.size(), 1);
    }
}