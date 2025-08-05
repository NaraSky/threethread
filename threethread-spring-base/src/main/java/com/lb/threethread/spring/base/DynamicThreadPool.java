package com.lb.threethread.spring.base;

import java.lang.annotation.*;


/**
 * 动态线程池注解
 * <p>
 * 用于标记线程池Bean，标识其为动态线程池，需要进行动态管理和监控
 * </p>
 * <p>
 * 设计目的：
 * 1. 通过注解方式标识动态线程池，便于框架自动识别和管理
 * 2. 支持在类级别或方法级别使用，提供灵活的配置方式
 * 3. 作为AOP切入点，为动态线程池功能提供基础支持
 * </p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DynamicThreadPool {
}