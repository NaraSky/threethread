package com.lb.threethread.config.apollo.starter.refresher;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.lb.threethread.config.common.starter.refresher.AbstractDynamicThreadPoolRefresher;
import com.lb.threethread.core.notification.service.DingTalkMessageService;
import com.lb.threethread.spring.base.configuration.BootstrapConfigProperties;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Apollo配置中心线程池刷新处理器
 * <p>
 * 主要功能：
 * 1. 监听Apollo配置中心的配置变化
 * 2. 当配置发生变化时，动态更新线程池参数
 * 3. 实现配置热更新，无需重启应用即可调整线程池配置
 * </p>
 * <p>
 * 设计特点：
 * 1. 继承自AbstractDynamicThreadPoolRefresher，实现通用的线程池刷新逻辑
 * 2. 使用Apollo原生API监听配置变化
 * 3. 支持多种配置文件格式（如properties、yaml等）
 * </p>
 */
@Slf4j(topic = "OneThreadConfigRefresher")
public class ApolloRefresherHandler extends AbstractDynamicThreadPoolRefresher {

    public ApolloRefresherHandler(BootstrapConfigProperties properties, NotifierDispatcher notifierDispatcher) {
        super(properties, notifierDispatcher);
    }

    /**
     * 注册Apollo配置监听器
     * <p>
     * 实现原理：
     * 1. 从配置中获取Apollo的namespace
     * 2. 获取对应namespace的配置对象
     * 3. 创建配置变更监听器
     * 4. 注册监听器监听配置变化
     * 5. 当配置变化时，调用refreshThreadPoolProperties方法刷新线程池配置
     * </p>
     * 
     * @throws Exception 配置监听器注册异常
     */
    @SneakyThrows
    public void registerListener() {
        // 获取Apollo配置信息
        BootstrapConfigProperties.ApolloConfig apolloConfig = properties.getApollo();
        // 解析Apollo命名空间，支持多个命名空间用逗号分隔
        String[] apolloNamespaces = apolloConfig.getNamespace().split(",");

        // 当前只处理第一个命名空间
        String namespace = apolloNamespaces[0];
        // 获取配置文件类型（如properties、yaml等）
        String configFileType = properties.getConfigFileType().getValue();
        // 获取Apollo配置对象
        Config config = ConfigService.getConfig(String.format("%s.%s", namespace, properties.getConfigFileType().getValue()));

        // 创建配置变更监听器
        ConfigChangeListener configChangeListener = createConfigChangeListener(namespace, configFileType);
        // 注册配置变更监听器
        config.addChangeListener(configChangeListener);

        log.info("Dynamic thread pool refresher, add apollo listener success. namespace: {}", namespace);
    }

    /**
     * 创建Apollo配置变更监听器
     * <p>
     * 当Apollo配置发生变化时，会触发此监听器
     * 实现原理：
     * 1. 获取变更后的配置文件内容
     * 2. 调用父类的refreshThreadPoolProperties方法处理配置更新
     * </p>
     * 
     * @param namespace 命名空间
     * @param configFileType 配置文件类型
     * @return Apollo配置变更监听器
     */
    private ConfigChangeListener createConfigChangeListener(String namespace, String configFileType) {
        return configChangeEvent -> {
            // 处理命名空间名称，去除文件类型后缀
            String namespaceItem = namespace.replace("." + configFileType, "");
            // 根据文件类型字符串获取对应的枚举值
            ConfigFileFormat configFileFormat = ConfigFileFormat.fromString(configFileType);
            // 获取配置文件对象
            ConfigFile configFile = ConfigService.getConfigFile(namespaceItem, configFileFormat);
            // 刷新线程池配置
            refreshThreadPoolProperties(configFile.getContent());
        };
    }
}