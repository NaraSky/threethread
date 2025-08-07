package com.lb.threethread.config.apollo.starter.configuration;

import com.lb.threethread.config.apollo.starter.refresher.ApolloRefresherHandler;
import com.lb.threethread.core.notification.service.DingTalkMessageService;
import com.lb.threethread.spring.base.configuration.BootstrapConfigProperties;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;

/**
 * Apollo配置中心自动配置类
 * <p>
 * 主要功能：
 * 1. 自动配置Apollo配置中心相关的组件
 * 2. 创建ApolloRefresherHandler实例用于监听配置变化
 * 3. 通过条件注解控制配置的加载条件
 * </p>
 * <p>
 * 设计目的：
 * 1. 与Spring Boot的自动配置机制集成
 * 2. 实现配置中心组件的自动装配
 * 3. 提供开箱即用的Apollo配置监听功能
 * </p>
 */
@Configurable
public class ApolloAutoConfiguration {

    /**
     * 创建Apollo配置刷新处理器Bean
     * <p>
     * 该Bean用于监听Apollo配置变化并刷新动态线程池配置
     * 实现原理：
     * 1. 在Spring容器中注册ApolloRefresherHandler实例
     * 2. ApolloRefresherHandler会自动注册配置监听器
     * 3. 当Apollo配置发生变化时，自动更新线程池参数
     * </p>
     *
     * @param properties 启动配置属性
     * @return ApolloRefresherHandler实例
     */
    @Bean
    public ApolloRefresherHandler apolloRefresherHandler(BootstrapConfigProperties properties,
                                                         NotifierDispatcher notifierDispatcher) {
        return new ApolloRefresherHandler(properties, notifierDispatcher);
    }
}