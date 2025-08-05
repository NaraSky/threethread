package com.lb.threethread.spring.base.support;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Spring应用上下文持有者
 * <p>
 * 用于在非Spring管理的类中获取Spring容器中的Bean实例，
 * 提供静态方法访问Spring容器功能
 * </p>
 * <p>
 * 设计目的：
 * 1. 提供全局访问Spring ApplicationContext的途径
 * 2. 支持在静态方法或工具类中获取Spring管理的Bean
 * 3. 封装Spring容器访问细节，提供简洁的API
 * </p>
 */
public class ApplicationContextHolder implements ApplicationContextAware {

    /**
     * Spring应用上下文实例
     * <p>
     * 通过实现ApplicationContextAware接口，在Spring容器初始化时自动注入
     * </p>
     */
    private static ApplicationContext CONTEXT;

    /**
     * 设置应用上下文实例
     * <p>
     * 由Spring容器在初始化时自动调用，注入ApplicationContext实例
     * </p>
     *
     * @param applicationContext Spring应用上下文
     * @throws BeansException Bean异常
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApplicationContextHolder.CONTEXT = applicationContext;
    }

    /**
     * 根据类型从容器中获取 Bean
     *
     * @param clazz Bean 类型
     * @param <T>   泛型
     * @return 对应类型的 Bean 实例
     */
    public static <T> T getBean(Class<T> clazz) {
        return CONTEXT.getBean(clazz);
    }

    /**
     * 根据名称和类型从容器中获取 Bean
     *
     * @param name  Bean 名称
     * @param clazz Bean 类型
     * @param <T>   泛型
     * @return 对应的 Bean 实例
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        return CONTEXT.getBean(name, clazz);
    }

    /**
     * 根据类型获取容器中所有匹配的 Bean
     *
     * @param clazz Bean 类型
     * @param <T>   泛型
     * @return 包含所有匹配 Bean 的 Map，key 为 Bean 名称，value 为实例
     */
    public static <T> Map<String, T> getBeansOfType(Class<T> clazz) {
        return CONTEXT.getBeansOfType(clazz);
    }

    /**
     * 查找指定 Bean 上是否存在特定注解
     *
     * @param beanName       Bean 名称
     * @param annotationType 注解类型
     * @param <A>            注解泛型
     * @return 注解实例，若不存在则返回 null
     */
    public static <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType) {
        return CONTEXT.findAnnotationOnBean(beanName, annotationType);
    }

    /**
     * 获取当前的 ApplicationContext 实例
     *
     * @return Spring 应用上下文
     */
    public static ApplicationContext getInstance() {
        return CONTEXT;
    }
}