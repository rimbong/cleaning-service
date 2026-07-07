package com.boot.cleanhub.util.xml;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * <pre>
 *   XMLUtil 
 * 아래 3개의 라이브러리 포함 해야함
 * jackson-dataformat-xml-2.13.5.jar
 * woodstox-core-6.4.0.jar
 * stax2-api-4.2.1.jar
 * </pre>
 * @version 1.0
 */
public final class XMLUtil {

    private static class XMLUtilHolder {
        public static final XmlMapper xmlMapper;
        static {
            try {
                xmlMapper = new XmlMapper();
                xmlMapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true);
                xmlMapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, false);
            } catch (Throwable t) {
                throw new RuntimeException("Failed to initialize XmlMapper", t);
            }
        }
        
    }

    public static XmlMapper getXmlMapper() {
        return XMLUtilHolder.xmlMapper;
    }

    public static <T, V> Map<T, V> getMap(String xml) throws IOException, Exception {
        return toObject(xml, new TypeReference<Map<T, V>>() {});
    }

    public static <T, V> Map<T, V> getMapSafe(String xml) {
        try {
            return toObject(xml, new TypeReference<Map<T, V>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    public static <T, V> List<Map<T, V>> getListMap(String xml) throws IOException, Exception {
        return toObject(xml, new TypeReference<List<Map<T, V>>>() {});
    }

    public static <T, V> List<Map<T, V>> getListMapSafe(String xml) {
        try {
            return toObject(xml, new TypeReference<List<Map<T, V>>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static <T> Object getObject(String xml, Class<T> clazz) throws IOException, Exception {
        return toObject(xml, clazz);
    }

    public static <T> Object getObjectSafe(String xml, Class<T> clazz) {
        try {
            return toObject(xml, clazz);
        } catch (Exception e) {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e2) {
                return null;
            }
        }
    }

    public static <T> T getObject(String xml, TypeReference<T> typeRef) throws IOException, Exception {
        return toObject(xml, typeRef);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getObjectSafe(String xml, TypeReference<T> typeRef) {
        try {
            return toObject(xml, typeRef);
        } catch (Exception e) {
            // List 타입이면 빈 리스트 반환
            Class<?> rawType = typeRef.getType() instanceof ParameterizedType
                    ? (Class<?>) ((ParameterizedType) typeRef.getType()).getRawType()
                    : null;
            if (rawType != null && List.class.isAssignableFrom(rawType)) {
                return (T) new ArrayList<>();
            }
            return null;
        }
    }

    public static String getXMLStr(Object xmlObject, String rootTag) throws IOException, Exception {
        return parseXMLString(xmlObject,rootTag);
    }

    public static String getXMLStrSafe(Object xmlObject, String rootTag) {
        try {
            return parseXMLString(xmlObject,rootTag);
        } catch (Exception e) {
            return "<empty></empty>";
        }
    }

    public static String getXMLStr(Object xmlObject) throws IOException, Exception {
        return parseXMLString(xmlObject);
    }

    public static String getXMLStrSafe(Object xmlObject) {
        try {
            return parseXMLString(xmlObject);
        } catch (Exception e) {
            return "<empty></empty>";
        }
    }

    public static <T> T getObjectFromMap(Map<?, ?> map, Class<T> valueType) throws IllegalArgumentException, Exception {
        return convertMapToObject(map, valueType);
    }

    public static <T> T getObjectFromMapSafe(Map<?, ?> map, Class<T> valueType) {
        try {
            return convertMapToObject(map, valueType);
        } catch (Exception e) {
            try {
                return valueType.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public static <T> T getObjectFromMap(Map<?, ?> map, TypeReference<T> typeRef) throws IllegalArgumentException, Exception {
        return convertMapToObject(map, typeRef);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getObjectFromMapSafe(Map<?, ?> map, TypeReference<T> typeRef) {
        try {
            return convertMapToObject(map, typeRef);
        } catch (Exception e) {
            // List 타입이면 빈 리스트 반환
            Class<?> rawType = typeRef.getType() instanceof ParameterizedType
                    ? (Class<?>) ((ParameterizedType) typeRef.getType()).getRawType()
                    : null;
            if (rawType != null && List.class.isAssignableFrom(rawType)) {
                return (T) new ArrayList<>();
            }
            return null;
        }
    }

    public static <T> T getObjectFromMap(Map<?, ?> map, JavaType javaType) throws IllegalArgumentException, Exception {
        return convertMapToObject(map, javaType);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getObjectFromMapSafe(Map<?, ?> map, JavaType javaType) {
        try {
            return convertMapToObject(map, javaType);
        } catch (Exception e) {
            // JavaType의 raw 타입 확인
            Class<?> rawType = javaType.getRawClass();
            if (List.class.isAssignableFrom(rawType)) {
                return (T) new ArrayList<>();
            }
            try {
                return (T) rawType.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private static <T> T toObject(String xmlString, Class<T> valueType) throws IOException, Exception {
        return getXmlMapper().readValue(xmlString, valueType);
    }

    private static <T> T toObject(String xmlString, TypeReference<T> valueType) throws IOException, Exception {
        return getXmlMapper().readValue(xmlString, valueType);
    }

    private static String parseXMLString(Object obj) throws IOException, Exception {
        return parseXMLString(obj, "map");
    }

    private static String parseXMLString(Object obj,String rootTag) throws IOException, Exception {
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            if (map.isEmpty()) {
                return "<" + rootTag + "></" + rootTag + ">"; //
            }
            for (Object key : map.keySet()) {
                if (key == null || !(key instanceof String) || ((String) key).trim().isEmpty()) {
                    throw new IllegalArgumentException("Invalid map key for XML serialization: " + key);
                }
            }
            return getXmlMapper()
                    .writer()
                    .withRootName(rootTag)
                    .writeValueAsString(obj);
        }
        return getXmlMapper()
                .writerFor(new TypeReference<Object>(){})
                .withRootName(rootTag)
                .writeValueAsString(obj);
    }

    private static <T> T convertMapToObject(Map<?, ?> map, Class<T> valueType) throws IllegalArgumentException, Exception {
        return getXmlMapper().convertValue(map, valueType);
    }

    private static <T> T convertMapToObject(Map<?, ?> map, TypeReference<T> typeRef) throws IllegalArgumentException, Exception {
        return getXmlMapper().convertValue(map, typeRef);
    }

    private static <T> T convertMapToObject(Map<?, ?> map, JavaType javaType) throws IllegalArgumentException, Exception {
        return getXmlMapper().convertValue(map, javaType);
    }
}