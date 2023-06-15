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

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.lang.reflect.Constructor;
import java.util.HashMap;

import com.epam.deltix.util.ldap.config.Binding;
import com.epam.deltix.util.security.Entry;

final class EntryFactory {
    private final HashMap<Class<? extends Entry>, EntryMembersBinding> map = new HashMap<>();
    private final HashMap<String, Class<? extends Entry>> classes = new HashMap<>();

    public EntryFactory() {
    }

    public void addBinding(Class<? extends Entry> clazz, Binding binding) {
        for (String objectsClass : binding.objectsClasses)
            this.classes.put(objectsClass, clazz);

        EntryMembersBinding mapping = new EntryMembersBinding(clazz);

        for (com.epam.deltix.util.ldap.config.Attribute attribute : binding.attributes) {
            if (attribute.field != null)
                mapping.addField(attribute.name, attribute.field);
            else if (attribute.property != null)
                mapping.addProperty(attribute.name, attribute.property);
            else
                throw new IllegalArgumentException("Undefined attribute mapping " + attribute);
        }

        map.put(clazz, mapping);
    }

    public String[] getAttributes(Class<? extends Entry> clazz) {
        return map.get(clazz).getAttributes();
    }

    public Entry create(Attributes attrs) throws Exception {

        Attribute objectClass = attrs.get("objectClass");
        if (objectClass == null)
            throw new IllegalArgumentException("Cannot retrieve 'objectClass' attribute.");

        Class<? extends Entry> clazz = findClass(objectClass);
        if (clazz == null)
            throw new IllegalArgumentException("Cannot find mapping for the 'objectClass'=" + objectClass);

        EntryMembersBinding mapping = map.get(clazz);

        Constructor cs = clazz.getConstructor(java.lang.String.class);
        String id = mapping.getIdAttribute();
        if (id == null)
            throw new IllegalArgumentException("Mapping should contains 'id' attribute");

        Entry entry = (Entry) cs.newInstance(attrs.get(id).get());

        NamingEnumeration<String> ids = attrs.getIDs();
        while (ids.hasMoreElements()) {
            String name = ids.next();
            mapping.setValue(entry, name, attrs.get(name).getAll());
        }

        return entry;
    }

    private Class<? extends Entry> findClass(Attribute objectClass) throws NamingException {

        NamingEnumeration<?> values = objectClass.getAll();

        while (values.hasMore()) {
            Class<? extends Entry> clazz = classes.get(values.next().toString());
            if (clazz != null)
                return clazz;
        }

        return null;
    }

    public Entry create(Class<? extends Entry> clazz, Attributes attrs) throws Exception {

        EntryMembersBinding mapping = map.get(clazz);

        Constructor cs = clazz.getConstructor(java.lang.String.class);
        String id = mapping.getIdAttribute();
        if (id == null)
            throw new IllegalArgumentException("Mapping should contains 'id' attribute");

        Entry entry = (Entry) cs.newInstance(attrs.get(id).get());

        NamingEnumeration<String> ids = attrs.getIDs();
        while (ids.hasMoreElements()) {
            String name = ids.next();
            mapping.setValue(entry, name, attrs.get(name).getAll());
        }

        return entry;
    }
}