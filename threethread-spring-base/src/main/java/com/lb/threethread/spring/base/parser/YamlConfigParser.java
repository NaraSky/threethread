package com.lb.threethread.spring.base.parser;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.lb.threethread.core.parser.AbstractConfigParser;
import com.lb.threethread.core.parser.ConfigFileTypeEnum;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ByteArrayResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * YAML 类型配置文件解析器
 */
public class YamlConfigParser extends AbstractConfigParser {

    @Override
    public Map<Object, Object> doParse(String content) {
        if (StrUtil.isEmpty(content)) {
            return new HashMap<>(1);
        }

        YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
        yamlPropertiesFactoryBean.setResources(new ByteArrayResource(content.getBytes()));
        return yamlPropertiesFactoryBean.getObject();
    }

    @Override
    public List<ConfigFileTypeEnum> getConfigFileTypes() {
        return CollectionUtil.newArrayList(ConfigFileTypeEnum.YML, ConfigFileTypeEnum.YAML);
    }
}