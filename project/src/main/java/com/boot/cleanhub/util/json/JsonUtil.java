package com.boot.cleanhub.util.json;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtil {

    private static class JsonUtilHolder {
        public static final ObjectMapper om = new ObjectMapper();
    }

    /* 
     * custom Mapper 사용의 경우 Jackson을 2.15 이상으로 업그레이드하여 사용해야함
     */
    // public static class MapperConfigBuilder {
    //     private int maxStringLength = StreamReadConstraints.DEFAULT_MAX_STRING_LEN; 
    //     private int maxNumberLength = StreamReadConstraints.DEFAULT_MAX_NUM_LEN; 

    //     public MapperConfigBuilder maxStringLength(int maxStringLength) {
    //         if (maxStringLength <= 0) {
    //             throw new IllegalArgumentException("maxStringLength must be positive");
    //         }
    //         this.maxStringLength = maxStringLength;
    //         return this;
    //     }

    //     public MapperConfigBuilder maxNumberLength(int maxNumberLength) {
    //         if (maxNumberLength <= 0) {
    //             throw new IllegalArgumentException("maxNumberLength must be positive");
    //         }
    //         this.maxNumberLength = maxNumberLength;
    //         return this;
    //     }

    //     public ObjectMapper build() {
    //         ObjectMapper mapper = new ObjectMapper();
    //         mapper.getFactory().setStreamReadConstraints(
    //             StreamReadConstraints.builder()
    //                 .maxStringLength(maxStringLength)
    //                 .maxNumberLength(maxNumberLength)
    //                 .build()
    //         );
    //         return mapper;
    //     }
	
	// }

    // public static MapperConfigBuilder getCustomObjMapper() {
    //     return new MapperConfigBuilder();
    // }

    public static ObjectMapper getObjMapper() {
        return JsonUtilHolder.om;
    }

	public static <T,V> Map<T, V> getMap(String json) throws JsonParseException, JsonMappingException, Exception{
		return toObject( json, new TypeReference<Map<T, V>>(){} );
	}
    
    public static <T, V> Map<T, V> getMapSafe(String json) {
        try {
            return toObject(json, new TypeReference<Map<T, V>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
	
    public static <T,V> List<Map<T, V>> getListMap(String json) throws JsonParseException, JsonMappingException, Exception{
        return toObject( json, new TypeReference< List<Map<T, V>>>(){} );
	}

    public static <T, V> List<Map<T, V>> getListMapSafe(String json) {
        try {
            return toObject(json, new TypeReference<List<Map<T, V>>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static <T> Object getObject(InputStream is, Class<T> clazz) throws JsonParseException, JsonMappingException, Exception{
		return toObject(is, clazz);
	}

	public static <T> Object getObjectSafe(InputStream is, Class<T> clazz) {
		try {
            return toObject(is, clazz);
        } catch (Exception e) {
            return null;
        }
	}
    
	public static <T> Object getObject(String json, Class<T> clazz) throws JsonParseException, JsonMappingException, Exception{
		return toObject(json, clazz);
	}

	public static <T> Object getObjectSafe(String json, Class<T> clazz) {
		try {
            return toObject(json, clazz);
        } catch (Exception e) {
            return null;
        }
	}

    public static <T> T getObject(String json, TypeReference<T> typeRef) throws IOException, Exception{
        return toObject(json, typeRef);
    }

    public static <T> T getObjectSafe(String json, TypeReference<T> typeRef) {
        try {
            return toObject(json, typeRef);
        } catch (Exception e) {
            return null;
        }
    }
    
    public static <T> List<T> getListObject(String json, Class<T> elementClass) throws IOException, Exception {
        JavaType listType = getObjMapper().getTypeFactory().constructCollectionType(List.class, elementClass);
        return toObject(json, listType);
    }
    
    public static <T> List<T> getListObjectSafe(String json, Class<T> elementClass) {
        try {
            return getListObject(json, elementClass);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
    public static <T> List<T> getListObject(String json, TypeReference<List<T>> typeRef) throws IOException, Exception {
        return toObject(json, typeRef);
    }
    
    public static <T> List<T> getListObjectSafe(String json, TypeReference<List<T>> typeRef) {
        try {
            return toObject(json, typeRef);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

	
	public static String getJsonStr(Object jsonObject) throws JsonParseException, JsonMappingException, Exception {
		return parseJsonString(jsonObject);
	}
    
    public static String getJsonStrSafe(Object jsonObject) {
		try {
            return parseJsonString(jsonObject);
        } catch (Exception e) {
            return "{}";
        }
	}

    public static <T> T getObjectFromMap(Map<?, ?> map, Class<T> valueType) throws IllegalArgumentException, Exception {
        return convertMapToObject(map, valueType);
    }

    public static <T> T getObjectFromMapSafe(Map<?, ?> map, Class<T> valueType) {
        try {
            return convertMapToObject(map, valueType);
        } catch (Exception e) {
            return null;
        }
    }

    public static <T> T getObjectFromMap(Map<?, ?> map, TypeReference<T> typeRef) throws IllegalArgumentException, Exception {
        return convertMapToObject(map, typeRef);
    }

    public static <T> T getObjectFromMapSafe(Map<?, ?> map, TypeReference<T> typeRef) {
        try {
            return convertMapToObject(map, typeRef);
        } catch (Exception e) {            
            return null;
        }
    }

    public static <T> T getObjectFromMap(Map<?, ?> map, JavaType javaType) throws IllegalArgumentException, Exception {
        return convertMapToObject(map, javaType);
    }

    public static <T> T getObjectFromMapSafe(Map<?, ?> map, JavaType javaType) {
        try {
            return convertMapToObject(map, javaType);
        } catch (Exception e) {
           return null;
        }
    }

    public static Map<String, Object> getMapFromNode(JsonNode node) throws IllegalArgumentException, Exception {
    	return convertNodeToMap(node);
    }
    
    public static Map<String, Object> getMapFromNodeSafe(JsonNode node) {
        try {
            return convertNodeToMap(node);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private static <T> T toObject(InputStream is, Class<T> valueType) throws JsonParseException, JsonMappingException, Exception{
        return getObjMapper().readValue(is, valueType);
    }
    
    private static <T> T toObject(String jsonString, Class<T> valueType) throws JsonParseException, JsonMappingException, Exception{
        return getObjMapper().readValue(jsonString, valueType);
    }
    
    private static <T> T toObject(String jsonString, JavaType valueType) throws JsonParseException, JsonMappingException, Exception{
        return getObjMapper().readValue(jsonString, valueType);
    }

	private static <T> T toObject(String jsonString, TypeReference<T> valueType) throws JsonParseException, JsonMappingException, Exception{
        return getObjMapper().readValue(jsonString, valueType);
    }
    
    private static String parseJsonString(Object obj) throws JsonParseException, JsonMappingException, Exception {
        return getObjMapper().writeValueAsString(obj);
    }

    private static <T> T convertMapToObject(Map<?, ?> map, Class<T> valueType) throws IllegalArgumentException, Exception {
        return getObjMapper().convertValue(map, valueType);
    }

    private static <T> T convertMapToObject(Map<?, ?> map, TypeReference<T> typeRef) throws IllegalArgumentException, Exception {
        return getObjMapper().convertValue(map, typeRef);
    }

    private static <T> T convertMapToObject(Map<?, ?> map, JavaType javaType) throws IllegalArgumentException, Exception {
        return getObjMapper().convertValue(map, javaType);
    }

    private static Map<String, Object> convertNodeToMap(JsonNode node) throws IllegalArgumentException, Exception{
    	return getObjMapper().convertValue(node, new TypeReference<Map<String, Object>>() {});
    }
}