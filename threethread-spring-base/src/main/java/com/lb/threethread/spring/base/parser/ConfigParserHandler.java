package com.lb.threethread.spring.base.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 配置解析器处理器
 * <p>
 * 作为配置解析器的统一入口，负责根据配置文件类型选择合适的解析器进行解析
 * </p>
 * <p>
 * 设计目的：
 * 1. 提供统一的配置解析入口
 * 2. 通过策略模式动态选择合适的解析器
 * 3. 遵循单例模式，确保全局只有一个解析器处理器实例
 * </p>
 */
public final class ConfigParserHandler {

    /**
     * 配置解析器列表
     * <p>
     * 存储所有支持的配置解析器实例
     * </p>
     */
    private static final List<ConfigParser> PARSERS = new ArrayList<>();

    /**
     * 私有构造方法
     * <p>
     * 初始化所有支持的配置解析器
     * </p>
     */
    private ConfigParserHandler() {
        PARSERS.add(new YamlConfigParser());
        PARSERS.add(new PropertiesConfigParser());
    }

    /**
     * 解析配置内容
     * <p>
     * 根据指定的配置文件类型，选择合适的解析器解析配置内容
     * </p>
     *
     * @param content 配置内容字符串
     * @param type    配置文件类型
     * @return 解析后的键值对Map
     * @throws IOException 解析异常
     */
    public Map<Object, Object> parseConfig(String content, ConfigFileTypeEnum type) throws IOException {
        for (ConfigParser parser : PARSERS) {
            if (parser.supports(type)) {
                return parser.doParse(content);
            }
        }
        return Collections.emptyMap();
    }

    /**
     * 获取配置解析器处理器单例实例
     *
     * @return ConfigParserHandler单例实例
     */
    public static ConfigParserHandler getInstance() {
        return ConfigParserHandlerHolder.INSTANCE;
    }

    /**
     * 配置解析器处理器持有者
     * <p>
     * 通过静态内部类实现线程安全的单例模式
     * </p>
     */
    private static class ConfigParserHandlerHolder {

        /**
         * 配置解析器处理器单例实例
         */
        private static final ConfigParserHandler INSTANCE = new ConfigParserHandler();
    }
}