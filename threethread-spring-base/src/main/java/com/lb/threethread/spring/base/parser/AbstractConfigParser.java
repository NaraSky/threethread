package com.lb.threethread.spring.base.parser;

/**
 * 配置解析器抽象类
 * <p>
 * 为具体配置解析器提供基础实现，简化配置解析器的开发
 * </p>
 * <p>
 * 设计目的：
 * 1. 提供配置解析器的通用实现
 * 2. 通过模板方法模式减少重复代码
 * 3. 统一配置解析器的实现规范
 * </p>
 */
public abstract class AbstractConfigParser implements ConfigParser {

    /**
     * 判断是否支持指定类型的配置文件解析
     * <p>
     * 通过检查当前解析器支持的配置文件类型列表来判断是否支持指定类型
     * </p>
     *
     * @param type 配置文件类型枚举
     * @return 是否支持该类型
     */
    @Override
    public boolean supports(ConfigFileTypeEnum type) {
        return getConfigFileTypes().contains(type);
    }
}