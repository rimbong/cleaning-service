package com.boot.cleanhub.util.xml;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

public final class SimpleXMLUtil {

    // Map을 위한 래퍼 클래스
    @XmlRootElement(name = "map")
    static class MapWrapper {
        @XmlElement(name = "entry")
        private List<MapEntry> entries = new ArrayList<>();

        public MapWrapper() {}

        public MapWrapper(Map<?, ?> map) {
            map.forEach((key, value) -> entries.add(new MapEntry(key, value)));
        }

        public Map<Object, Object> toMap() {
            Map<Object, Object> result = new java.util.HashMap<>();
            entries.forEach(entry -> result.put(entry.getKey(), entry.getValue()));
            return result;
        }
    }

    static class MapEntry {
        @XmlElement(name = "key")
        private Object key;

        @XmlElement(name = "value")
        private Object value;

        public MapEntry() {}

        public MapEntry(Object key, Object value) {
            this.key = key;
            this.value = value;
        }

        public Object getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }
    }

    private static JAXBContext getJAXBContext(Class<?>... classes) throws JAXBException {
        return JAXBContext.newInstance(classes);
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    public static <T, V> Map<T, V> getMap(String xml,  Class<? extends Map> mapType) throws JAXBException {
        MapWrapper wrapper = toObject(xml, MapWrapper.class);
        return (Map<T, V>) wrapper.toMap();
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    public static <T, V> Map<T, V> getMapSafe(String xml, Class<? extends Map> mapType) {
        try {
            return getMap(xml, mapType);
        } catch (Exception e) {
            return (Map<T, V>) Collections.emptyMap();
        }
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    public static <T, V> List<Map<T, V>> getListMap(String xml, Class<? extends List> listType) throws JAXBException {
        return toObject(xml, listType);
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    public static <T, V> List<Map<T, V>> getListMapSafe(String xml, Class<? extends List> listType) {
        try {
            return toObject(xml, listType);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static <T> T getObject(String xml, Class<T> clazz) throws JAXBException {
        return toObject(xml, clazz);
    }

    public static <T> T getObjectSafe(String xml, Class<T> clazz) {
        try {
            return toObject(xml, clazz);
        } catch (Exception e) {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public static String getXMLStr(Object xmlObject) throws JAXBException {
        if (xmlObject instanceof Map) {
            return parseXMLString(new MapWrapper((Map<?, ?>) xmlObject));
        }
        return parseXMLString(xmlObject);
    }

    public static String getXMLStrSafe(Object xmlObject) {
        try {
            return getXMLStr(xmlObject);
        } catch (Exception e) {
            return "<empty></empty>";
        }
    }

    public static <T> T getMapToObject(Map<?, ?> map, Class<T> valueType) throws JAXBException {
        String xml = getXMLStr(map);
        return toObject(xml, valueType);
    }

    public static <T> T getMapToObjectSafe(Map<?, ?> map, Class<T> valueType) {
        try {
            return getMapToObject(map, valueType);
        } catch (Exception e) {
            try {
                return valueType.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                return null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getMapToObjectSafe(Map<?, ?> map, Class<T> valueType, Class<?> rawType) {
        try {
            return getMapToObject(map, valueType);
        } catch (Exception e) {
            if (rawType != null && List.class.isAssignableFrom(rawType)) {
                return (T) new ArrayList<>();
            }
            try {
                return valueType.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private static <T> T toObject(String xmlString, Class<T> valueType) throws JAXBException {
        JAXBContext context = getJAXBContext(valueType);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StringReader reader = new StringReader(xmlString);
        return valueType.cast(unmarshaller.unmarshal(reader));
    }

    private static String parseXMLString(Object obj) throws JAXBException {
        JAXBContext context = getJAXBContext(obj.getClass());
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        StringWriter writer = new StringWriter();
        marshaller.marshal(obj, writer);
        return writer.toString();
    }
}