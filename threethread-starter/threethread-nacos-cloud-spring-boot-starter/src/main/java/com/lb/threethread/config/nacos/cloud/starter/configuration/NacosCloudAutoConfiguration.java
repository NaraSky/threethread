package com.lb.threethread.config.nacos.cloud.starter.configuration;

import com.lb.threethread.config.nacos.cloud.starter.refresher.NacosCloudRefresherHandler;
import com.lb.threethread.spring.base.configuration.BootstrapConfigProperties;
import com.lb.threethread.spring.base.configuration.OneThreadBaseConfiguration;
import com.lb.threethread.spring.base.enable.MarkerConfiguration;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

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
@ConditionalOnBean(MarkerConfiguration.Marker.class)
@Import(OneThreadBaseConfiguration.class)
@AutoConfigureAfter(OneThreadBaseConfiguration.class)
@ConditionalOnProperty(prefix = BootstrapConfigProperties.PREFIX, value = "enable", matchIfMissing = true, havingValue = "true")
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
    public NacosCloudRefresherHandler nacosCloudRefresherHandler(BootstrapConfigProperties properties) {
        return new NacosCloudRefresherHandler(properties);
    }
}