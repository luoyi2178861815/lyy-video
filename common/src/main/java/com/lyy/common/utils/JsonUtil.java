package com.lyy.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * JSON 通用序列化/反序列化工具类
 * 基于Jackson封装，统一全局配置，提供常用转换方法
 * 特性：
 * 1. Long类型序列化自动转字符串，避免前端精度丢失
 * 2. 反序列化忽略未知字段，不抛异常
 * 3. 统一异常捕获，对外返回友好结果
 * 4. 提供对象、集合、Map互转全套方法
 * @author xxx
 * @date 2026-06-24
 */
@Slf4j
public class JsonUtil {

    /**
     * 全局单例ObjectMapper，全局复用，避免频繁创建损耗性能
     * static final 保证线程安全，全局唯一实例
     */
    private static final ObjectMapper OBJECT_MAPPER;

    static {
        // 静态代码块初始化全局配置，项目启动仅执行一次
        OBJECT_MAPPER = new ObjectMapper();
        // 反序列化时，JSON存在实体类不存在的字段不报错
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 解决Long、Long类型ID返回前端丢失精度问题（19位长数字js丢失精度）
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        OBJECT_MAPPER.registerModule(module);
    }

    /**
     * 获取全局ObjectMapper实例
     * 供特殊场景自定义拓展使用
     * @return ObjectMapper
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    // ====================== 对象转JSON字符串 ======================

    /**
     * Java对象转为JSON字符串
     * @param obj 待序列化对象（POJO/Map/List均可）
     * @return JSON字符串，对象为null返回null
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("对象转JSON字符串失败，对象信息:{}", obj, e);
            throw new RuntimeException("JSON序列化异常", e);
        }
    }

    /**
     * Java对象转为格式化、带换行的美观JSON字符串（打印日志调试用）
     * @param obj 待序列化对象
     * @return 格式化JSON字符串
     */
    public static String toPrettyJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("对象转格式化JSON失败，对象信息:{}", obj, e);
            throw new RuntimeException("格式化JSON序列化异常", e);
        }
    }

    // ====================== JSON字符串转实体对象 ======================

    /**
     * JSON字符串转为指定POJO实体对象
     * @param json JSON字符串
     * @param clazz 目标实体Class
     * @param <T> 泛型实体类型
     * @return 实体对象，json为空返回null
     */
    public static <T> T parseObj(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            log.error("JSON转实体对象失败，JSON文本:{}", json, e);
            throw new RuntimeException("JSON反序列化实体异常", e);
        }
    }

    // ====================== JSON字符串转List集合 ======================

    /**
     * JSON数组字符串转为List集合
     * 示例：List<User> list = JsonUtil.parseList(jsonStr, User.class);
     * @param json JSON数组字符串
     * @param clazz 集合内元素实体Class
     * @param <T> 元素泛型
     * @return List<T>
     */
    public static <T> List<T> parseList(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (IOException e) {
            log.error("JSON转List集合失败，JSON文本:{}", json, e);
            throw new RuntimeException("JSON反序列化List异常", e);
        }
    }

    // ====================== JSON字符串转Map ======================

    /**
     * JSON对象字符串转为Map<String, Object>
     * @param json JSON对象字符串
     * @return Map
     */
    public static Map<String, Object> parseMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            log.error("JSON转Map失败，JSON文本:{}", json, e);
            throw new RuntimeException("JSON反序列化Map异常", e);
        }
    }

    /**
     * JSON字符串转自定义泛型Map（指定key、value类型）
     * @param json JSON字符串
     * @param keyClazz key类型
     * @param valueClazz value类型
     * @param <K> key泛型
     * @param <V> value泛型
     * @return Map<K,V>
     */
    public static <K, V> Map<K, V> parseMap(String json, Class<K> keyClazz, Class<V> valueClazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, OBJECT_MAPPER.getTypeFactory().constructMapType(Map.class, keyClazz, valueClazz));
        } catch (IOException e) {
            log.error("JSON转泛型Map失败，JSON文本:{}", json, e);
            throw new RuntimeException("JSON反序列化泛型Map异常", e);
        }
    }

    // ====================== 泛型复杂类型转换（高阶通用） ======================

    /**
     * 复杂泛型反序列化（如 List<Map<String,User>>、Page<User> 等复杂结构）
     * 使用TypeReference处理泛型擦除问题
     * 示例：List<Map<String, User>> data = JsonUtil.parseComplex(json, new TypeReference<>() {});
     * @param json JSON字符串
     * @param typeReference 泛型类型引用
     * @param <T> 目标泛型类型
     * @return 转换后的泛型对象
     */
    public static <T> T parseComplex(String json, TypeReference<T> typeReference) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (IOException e) {
            log.error("复杂泛型JSON转换失败，JSON文本:{}", json, e);
            throw new RuntimeException("复杂泛型JSON反序列化异常", e);
        }
    }

    // ====================== 对象之间互转（拷贝属性） ======================

    /**
     * 对象属性拷贝：将源对象属性转为目标对象
     * 适用场景：DTO、VO、PO互转
     * @param source 源对象
     * @param targetClazz 目标对象Class
     * @param <S> 源类型
     * @param <T> 目标类型
     * @return 目标实体
     */
    public static <S, T> T convertObj(S source, Class<T> targetClazz) {
        if (source == null) {
            return null;
        }
        return OBJECT_MAPPER.convertValue(source, targetClazz);
    }
}