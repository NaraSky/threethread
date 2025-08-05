package com.lb.threethread.spring.base.parser;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ByteArrayResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * YAML 类型配置文件解析器
 * <p>
 * 用于解析YAML格式的配置文件，将其转换为键值对Map
 * </p>
 * <p>
 * 设计目的：
 * 1. 提供YAML格式配置文件的解析功能
 * 2. 基于Spring的YamlPropertiesFactoryBean实现解析
 * 3. 支持.yml和.yaml两种后缀的YAML文件
 * </p>
 */
public class YamlConfigParser extends AbstractConfigParser {

    /**
     * 解析YAML格式的配置内容
     * <p>
     * 使用Spring的YamlPropertiesFactoryBean将YAML内容解析为Properties对象
     * </p>
     *
     * @param content YAML配置内容字符串
     * @return 解析后的键值对Map
     */
    @Override
    public Map<Object, Object> doParse(String content) {
        if (StrUtil.isEmpty(content)) {
            return new HashMap<>(1);
        }

        YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
        yamlPropertiesFactoryBean.setResources(new ByteArrayResource(content.getBytes()));
        return yamlPropertiesFactoryBean.getObject();
    }

    /**
     * 获取当前解析器支持的配置文件类型列表
     *
     * @return 支持的配置文件类型集合，包括YML和YAML
     */
    @Override
    public List<ConfigFileTypeEnum> getConfigFileTypes() {
        return CollectionUtil.newArrayList(ConfigFileTypeEnum.YML, ConfigFileTypeEnum.YAML);
    }
}