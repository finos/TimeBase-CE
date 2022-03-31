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
package com.epam.deltix.qsrv.hf.pub.md;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Attribute definition for the DataFields.
 */
public class Attributes {

    @XmlElement(name = "item")
    public ArrayList<MapElement> items = new ArrayList<>();

    public Attributes(ArrayList<MapElement> items) {
        this.items = items;
    }

    public Attributes() { // JAXB
    }

    public void clear() {
        items = new ArrayList<>();
    }
}

class MapElement {

    @XmlElement()
    public String   name;

    @XmlElement()
    public String   value;

    private MapElement() { } //Required by JAXB

    public MapElement(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

class MapAdapter extends XmlAdapter<MapElement[], Map<String, String>> {
    public MapAdapter() {
    }

    public MapElement[] marshal(Map<String, String> map) throws Exception {
        MapElement[] mapElements = null;

        if (map != null) {
            mapElements = new MapElement[map.size()];
            int i = 0;
            for (Map.Entry<String, String> entry : map.entrySet())
                mapElements[i++] = new MapElement(entry.getKey(), entry.getValue());
        }

        return mapElements;
    }

    public Map<String, String> unmarshal(MapElement[] elements) throws Exception {
        Map<String, String> map = null;

        if (elements != null) {
            map = new HashMap<>();
            for (MapElement e : elements)
                map.put(e.name, e.value);
        }

        return map;
    }
}
