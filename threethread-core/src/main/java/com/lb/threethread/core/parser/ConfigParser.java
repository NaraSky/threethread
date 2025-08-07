package com.lb.threethread.core.parser;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 配置解析器接口
 * <p>
 * 定义配置解析器的标准接口，用于解析不同格式的配置文件内容
 * </p>
 * <p>
 * 设计目的：
 * 1. 提供统一的配置解析接口，支持多种配置文件格式
 * 2. 通过策略模式实现不同类型配置文件的解析
 * 3. 支持扩展新的配置文件格式解析器
 * </p>
 *
 * @author 马丁
 */
public interface ConfigParser {

    /**
     * 判断是否支持指定类型的配置文件解析
     *
     * @param type 配置文件类型枚举
     * @return 是否支持该类型
     */
    boolean supports(ConfigFileTypeEnum type);

    /**
     * 解析配置内容字符串为键值对 Map
     *
     * @param content 配置文件内容字符串
     * @return 解析后的键值对 Map
     * @throws IOException 解析失败时抛出
     */
    Map<Object, Object> doParse(String content) throws IOException;

    /**
     * 获取当前解析器支持的配置文件类型列表
     *
     * @return 支持的配置文件类型集合
     */
    List<ConfigFileTypeEnum> getConfigFileTypes();
}