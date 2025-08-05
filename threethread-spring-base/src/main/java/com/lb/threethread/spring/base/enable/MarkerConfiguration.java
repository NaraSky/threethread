package com.lb.threethread.spring.base.enable;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 动态线程池标记配置类
 * <p>
 * 作为@EnableOneThread注解的核心支撑类，通过@Import注解导入，
 * 用于触发动态线程池框架的自动配置机制
 * </p>
 * <p>
 * 设计目的：
 * 1. 作为@EnableOneThread注解的导入配置类
 * 2. 提供标记Bean，用于条件装配判断
 * 3. 触发框架相关的自动配置逻辑
 * </p>
 *
 * @see EnableOneThread
 */
@Configuration
public class MarkerConfiguration {

    /**
     * 创建动态线程池标记Bean
     * <p>
     * 该Bean主要用作标记，可用于条件装配（@ConditionalOnBean等）中作为存在性判断依据，
     * 确保在启用动态线程池功能时相关的配置和组件能够正确加载
     * </p>
     *
     * @return Marker实例
     */
    @Bean
    public Marker dynamicThreadPoolMarkerBean() {
        return new Marker();
    }

    /**
     * 标记类
     * <p>
     * 可用于条件装配（@ConditionalOnBean 等）中作为存在性的判断依据
     * </p>
     * <p>
     * 设计目的：
     * 1. 作为框架是否启用的判断标记
     * 2. 为条件化配置提供判断依据
     * 3. 避免直接依赖具体的业务Bean进行判断
     * </p>
     */
    public static class Marker {

    }
}