package com.lb.threethread.core.parser;

import cn.hutool.core.collection.CollectionUtil;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Properties 类型配置文件解析器
 * <p>
 * 用于解析Properties格式的配置文件，将其转换为键值对Map
 * </p>
 * <p>
 * 设计目的：
 * 1. 提供Properties格式配置文件的解析功能
 * 2. 基于Java标准的Properties类实现解析
 * 3. 支持.properties后缀的配置文件
 * </p>
 */
public class PropertiesConfigParser extends AbstractConfigParser {

    /**
     * 解析Properties格式的配置内容
     * <p>
     * 使用Java标准的Properties类将配置内容解析为Properties对象
     * </p>
     *
     * @param content Properties配置内容字符串
     * @return 解析后的键值对Map
     * @throws IOException 解析异常
     */
    @Override
    public Map<Object, Object> doParse(String content) throws IOException {
        Properties properties = new Properties();
        properties.load(new StringReader(content));
        return properties;
    }

    /**
     * 获取当前解析器支持的配置文件类型列表
     *
     * @return 支持的配置文件类型集合，仅为PROPERTIES类型
     */
    @Override
    public List<ConfigFileTypeEnum> getConfigFileTypes() {
        return CollectionUtil.newArrayList(ConfigFileTypeEnum.PROPERTIES);
    }
}