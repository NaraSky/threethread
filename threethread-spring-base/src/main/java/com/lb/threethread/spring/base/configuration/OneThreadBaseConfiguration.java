package com.lb.threethread.spring.base.configuration;

import com.lb.threethread.spring.base.support.OneThreadBeanPostProcessor;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;

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

    /**
     * 创建动态线程池Bean后置处理器Bean
     * <p>
     * 用于自动识别和注册Spring容器中的动态线程池实例
     * </p>
     *
     * @return OneThreadBeanPostProcessor实例
     */
    @Bean
    public OneThreadBeanPostProcessor oneThreadBeanPostProcessor() {
        return new OneThreadBeanPostProcessor();
    }
}