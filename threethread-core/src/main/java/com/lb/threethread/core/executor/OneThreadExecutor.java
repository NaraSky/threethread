package com.lb.threethread.core.executor;

import lombok.Getter;
import lombok.NonNull;

import java.util.concurrent.*;

/**
 * 动态线程池执行器
 * <p>
 * 扩展自{@link ThreadPoolExecutor}，增加了线程池唯一标识功能，
 * 用于支持动态线程池的参数调整和监控功能
 * </p>
 * <p>
 * 设计目的：
 * 1. 为线程池增加唯一标识，便于统一管理和动态调整参数
 * 2. 作为动态线程池的核心执行器，支持运行时监控和调整
 * </p>
 */
public class OneThreadExecutor extends ThreadPoolExecutor {

    /**
     * 线程池唯一标识，用来动态变更参数等
     * <p>
     * 通过该标识可以在运行时定位到具体的线程池实例，
     * 实现动态调整线程池参数（如核心线程数、最大线程数等）
     * </p>
     */
    @Getter
    private final String threadPoolId;

    /**
     * 构造一个动态线程池执行器
     *
     * @param threadPoolId      线程池唯一标识
     * @param corePoolSize      核心线程数
     * @param maximumPoolSize   最大线程数
     * @param keepAliveTime     空闲线程存活时间
     * @param unit              时间单位
     * @param workQueue         工作队列
     * @param threadFactory     线程工厂
     * @param handler           拒绝策略
     */
    public OneThreadExecutor(
            @NonNull String threadPoolId,
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            @NonNull TimeUnit unit,
            @NonNull BlockingQueue<Runnable> workQueue,
            @NonNull ThreadFactory threadFactory,
            @NonNull RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);

        // 设置动态线程池扩展属性：线程池 ID 标识
        this.threadPoolId = threadPoolId;
    }
}