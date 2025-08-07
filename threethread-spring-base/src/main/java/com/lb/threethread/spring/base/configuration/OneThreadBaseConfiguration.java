package com.lb.threethread.spring.base.configuration;

import com.lb.threethread.core.config.BootstrapConfigProperties;
import com.lb.threethread.core.notification.service.DingTalkMessageService;
import com.lb.threethread.core.notification.service.NotifierDispatcher;
import com.lb.threethread.spring.base.support.ApplicationContextHolder;
import com.lb.threethread.spring.base.support.OneThreadBeanPostProcessor;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;

/**
 * 动态线程池基础配置类
 * <p>
 * 用于配置动态线程池框架所需的基础Bean，
 * 通过Spring的组件扫描自动加载
 * </p>
 * <p>
 * 设计目的：
 * 1. 自动配置动态线程池框架所需的核心组件
 * 2. 通过Spring的IOC容器管理框架组件的生命周期
 * 3. 提供开箱即用的配置方式，简化框架集成
 * </p>
 */
@Configurable
public class OneThreadBaseConfiguration {

    @Bean
    public ApplicationContextHolder applicationContextHolder() {
        return new ApplicationContextHolder();
    }

    @Bean
    @DependsOn("applicationContextHolder")
    public OneThreadBeanPostProcessor oneThreadBeanPostProcessor(BootstrapConfigProperties properties) {
        return new OneThreadBeanPostProcessor(properties);
    }

    @Bean
    public DingTalkMessageService dingTalkMessageService() {
        return new DingTalkMessageService();
    }

    @Bean
    public NotifierDispatcher notifierDispatcher() {
        return new NotifierDispatcher();
    }
}