package com.lb.threethread.config.nacos.cloud.starter.configuration;

import com.lb.threethread.config.nacos.cloud.starter.refresher.NacosCloudRefresherHandler;
import com.lb.threethread.core.notification.service.DingTalkMessageService;
import com.lb.threethread.core.notification.service.NotifierDispatcher;
import com.lb.threethread.spring.base.configuration.BootstrapConfigProperties;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;

/**
 * Nacos云配置自动配置类
 * <p>
 * 用于自动配置Nacos云配置相关的组件，包括配置监听器等
 * </p>
 * <p>
 * 设计目的：
 * 1. 自动配置Nacos云配置相关的组件
 * 2. 通过条件注解控制配置的加载条件
 * 3. 与Spring Boot的自动配置机制集成
 * </p>
 */
@Configurable
public class NacosCloudAutoConfiguration {

    /**
     * 创建Nacos云配置刷新处理器Bean
     * <p>
     * 用于监听Nacos配置变化并刷新动态线程池配置
     * </p>
     *
     * @param properties 启动配置属性
     * @return NacosCloudRefresherHandler实例
     */
    @Bean
    public NacosCloudRefresherHandler nacosCloudRefresherHandler(BootstrapConfigProperties properties,
                                                                 NotifierDispatcher notifierDispatcher) {
        return new NacosCloudRefresherHandler(properties, notifierDispatcher);
    }
}