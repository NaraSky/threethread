package com.lb.threethread.core.executor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 线程池执行器配置属性
 * <p>
 * 用于定义线程池的各项配置参数，支持通过建造者模式进行构建
 * </p>
 * <p>
 * 设计目的：
 * 1. 统一管理线程池的配置参数
 * 2. 支持通过配置文件或代码方式初始化线程池
 * 3. 为线程池的动态调整和监控提供数据支持
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ThreadPoolExecutorProperties {

    /**
     * 线程池唯一标识
     */
    private String threadPoolId;

    /**
     * 核心线程数
     * <p>
     * 线程池中保持的最小线程数，即使这些线程处于空闲状态也不会被销毁，
     * 除非设置了allowCoreThreadTimeOut
     * </p>
     */
    private Integer corePoolSize;

    /**
     * 最大线程数
     * <p>
     * 线程池中允许的最大线程数，当任务队列满且当前线程数小于该值时会创建新线程
     * </p>
     */
    private Integer maximumPoolSize;

    /**
     * 队列容量
     * <p>
     * 工作队列的容量，用于存放等待执行的任务
     * </p>
     */
    private Integer queueCapacity;

    /**
     * 阻塞队列类型
     * <p>
     * 指定线程池使用的工作队列类型，如ArrayBlockingQueue、LinkedBlockingQueue等
     * </p>
     */
    private String workQueue;

    /**
     * 拒绝策略类型
     * <p>
     * 当线程池和工作队列都满时采用的任务拒绝策略，
     * 如AbortPolicy、CallerRunsPolicy等
     * </p>
     */
    private String rejectedHandler;

    /**
     * 线程空闲存活时间（单位：秒）
     * <p>
     * 当线程数超过核心线程数时，多余的空闲线程在终止前等待新任务的最长时间
     * </p>
     */
    private Long keepAliveTime;

    /**
     * 是否允许核心线程超时
     * <p>
     * 如果设置为true，则核心线程也会在空闲时超时并终止
     * </p>
     */
    private Boolean allowCoreThreadTimeOut;

    /**
     * 通知配置
     */
    private NotifyConfig notify;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotifyConfig {

        /**
         * 接收人集合
         */
        private String receives;
    }
}