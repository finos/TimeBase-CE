package com.epam.deltix.test.qsrv.hf.pub.md;

import com.epam.deltix.qsrv.hf.pub.md.EnumClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.EnumValue;
import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.util.JUnitCategories;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@Category(JUnitCategories.TickDBFast.class)
public class EnumIntrospectionTest {

    @Test
    public void testOld() throws Introspector.IntrospectionException {
        Introspector introspector = Introspector.createOldIntrospector();
        EnumClassDescriptor ecd = introspector.introspectEnumClass(CMEEventType.class);
        for (int i = 0; i < ecd.getValues().length; i++) {
            assertEquals(ecd.getValues()[i].value, i);
            assertNotEquals(ecd.getValues()[i].value, CMEEventType.valueOf(ecd.getValues()[i].symbol));
        }
    }

    @Test
    public void testNew() throws Introspector.IntrospectionException {
        Introspector introspector = Introspector.createEmptyMessageIntrospector();
        EnumClassDescriptor ecd = introspector.introspectEnumClass(CMEEventType.class);
        int i = 0;
        for (EnumValue value : ecd.getValues()) {
            assertEquals(value.value, CMEEventType.valueOf(value.symbol).getNumber());
            assertNotEquals(value.value, i++);
        }
    }

}
