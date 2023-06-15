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
package com.epam.deltix.util.ldap.security;

import com.epam.deltix.util.collections.HCMultiMap;
import com.epam.deltix.util.collections.MultiMap;
import com.epam.deltix.util.security.Entry;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

final class EntryMembersBinding {
    private Class<? extends Entry> clazz;

    private final MultiMap<String, Field> fields = new HCMultiMap<>();
    private final HashMap<String, Method> properties = new HashMap<>();
    private String idAttributeName;

    EntryMembersBinding(Class<? extends Entry> clazz) {
        this.clazz = clazz;
    }

    public String[] getAttributes() {
        ArrayList<String> attrs = new ArrayList<>();
        attrs.addAll(fields.keySet());
        attrs.addAll(properties.keySet());
        attrs.add("objectClass");

        return attrs.toArray(new String[attrs.size()]);
    }

    public String getIdAttribute() {
        return idAttributeName;
    }

    public void addField(String attribute, String member) {
        try {
            if ("id".equals(member))
                idAttributeName = attribute;

            fields.put(attribute, clazz.getField(member));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            // TODO: log warning
        }
    }

    public void addProperty(String attribute, String member) {
        try {
            properties.put(attribute, findMethod(clazz, member));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    private Method findMethod(Class<?> clazz, String name) throws NoSuchMethodException {

        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (name.equals(method.getName()))
                return method;
        }

        throw new NoSuchMethodException(name);
    }

    public boolean setValue(Entry entry, String attribute, NamingEnumeration<?> values)
            throws NamingException, InvocationTargetException, IllegalAccessException {

        if (!values.hasMore())
            return false;

        Object value = null;
        Collection<Field> fs = fields.get(attribute);
        if (fs != null) {
            value = values.next();

            for (Field field : fs) {
                if (field.getDeclaringClass().isArray())
                    throw new UnsupportedOperationException();

                try {
                    field.set(entry, getValue(field.getType(), value));
                } catch (IllegalAccessException e) {
                    // do nothing
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }

        Method method = properties.get(attribute);
        if (method != null) {
            Class<?> type = method.getParameterTypes()[0];
            if (type.isArray()) {
                method.invoke(entry, getArray(type.getComponentType(), values));
            } else {
                method.invoke(entry, getValue(type, value != null ? value : values.next()));
            }

            return true;
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private static Object getValue(Class<?> type, Object value) {
        if (type.isEnum())
            return Enum.valueOf((Class<Enum>) type, value.toString());
        return value;
    }

    private Object getArray(Class type, NamingEnumeration<?> values) throws NamingException {

        ArrayList<Object> list = new ArrayList<Object>();
        while (values.hasMore())
            list.add(values.next());

        Object array = Array.newInstance(type, list.size());
        for (int i = 0; i < list.size(); i++)
            Array.set(array, i, list.get(i));

        return array;
    }

}