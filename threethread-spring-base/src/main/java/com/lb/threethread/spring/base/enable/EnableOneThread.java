package com.lb.threethread.spring.base.enable;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用动态线程池功能的注解
 * <p>
 * 通过在Spring Boot应用的启动类或配置类上添加此注解，
 * 可以启用动态线程池框架的所有功能
 * </p>
 * <p>
 * 设计目的：
 * 1. 提供一种简便的方式启用动态线程池功能
 * 2. 通过@Import注解导入标记配置类，触发框架的自动配置机制
 * 3. 遵循Spring Boot的设计模式，提供类似@EnableXxx的使用体验
 * </p>
 * <p>
 * 使用示例：
 * <pre>
 * &#064;SpringBootApplication
 * &#064;EnableOneThread
 * public class Application {
 *     public static void main(String[] args) {
 *         SpringApplication.run(Application.class, args);
 *     }
 * }
 * </pre>
 * </p>
 *
 * @see MarkerConfiguration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(MarkerConfiguration.class)
public @interface EnableOneThread {
}