package com.lb.threethread.spring.base.parser;

import lombok.Getter;

/**
 * 配置文件类型枚举
 * <p>
 * 定义支持的配置文件类型，用于标识和区分不同格式的配置文件
 * </p>
 * <p>
 * 设计目的：
 * 1. 统一管理支持的配置文件类型
 * 2. 为配置解析器提供类型标识
 * 3. 支持通过类型快速查找对应的解析器
 * </p>
 */
@Getter
public enum ConfigFileTypeEnum {

    /**
     * PROPERTIES类型配置文件
     * <p>
     * Java标准的属性配置文件格式，以.properties为后缀
     * </p>
     */
    PROPERTIES("properties"),

    /**
     * YML类型配置文件
     * <p>
     * YAML格式的配置文件，以.yml为后缀
     * </p>
     */
    YML("yml"),

    /**
     * YAML类型配置文件
     * <p>
     * YAML格式的配置文件，以.yaml为后缀
     * </p>
     */
    YAML("yaml");

    /**
     * 配置文件类型值
     * <p>
     * 对应配置文件的后缀名
     * </p>
     */
    private final String value;

    /**
     * 构造配置文件类型枚举
     *
     * @param value 配置文件类型值
     */
    ConfigFileTypeEnum(String value) {
        this.value = value;
    }

    /**
     * 根据值获取对应的配置文件类型枚举
     *
     * @param value 配置文件类型值
     * @return 对应的枚举实例，如果未找到则返回PROPERTIES
     */
    public static ConfigFileTypeEnum of(String value) {
        for (ConfigFileTypeEnum typeEnum : ConfigFileTypeEnum.values()) {
            if (typeEnum.value.equals(value)) {
                return typeEnum;
            }
        }
        return PROPERTIES;
    }
}