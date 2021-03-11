package com.epam.deltix.qsrv.hf.tickdb.http.stream;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Map;
import java.util.TreeMap;


@XmlRootElement(name = "changeSchema")
public class ChangeSchemaRequest extends StreamRequest {

    @XmlElement()
    public boolean              polymorphic;

    @XmlElement()
    public String               schema;

    /*
        Default values for the fields (if required), field name included fully qualified class name (ClassName:FieldName)
      */
    @XmlElement()
    @XmlJavaTypeAdapter(MapAdapter.class)
    public Map<String, String>  defaults;

    /*
        Mappings for the changed classes and fields
     */
    @XmlJavaTypeAdapter(MapAdapter.class)
    public Map<String, String>  mappings;

    @XmlElement()
    public boolean              background = false;
}


class MapElement {

    @XmlElement
    public String   name;
    @XmlElement
    public String   value;

    private MapElement() { } //Required by JAXB

    public MapElement(String name, String value) {
        this.name = name;
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
            map = new TreeMap<String, String>();
            for (MapElement e : elements)
                map.put(e.name, e.value);
        }
        return map;
    }
}