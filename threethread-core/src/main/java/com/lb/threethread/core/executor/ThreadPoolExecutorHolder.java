package com.lb.threethread.core.executor;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池执行器持有者
 * <p>
 * 用于包装线程池执行器及其相关属性配置，
 * 提供统一的线程池信息访问接口
 * </p>
 * <p>
 * 设计目的：
 * 1. 将线程池实例与其配置信息进行封装，便于统一管理
 * 2. 为线程池监控和动态调整提供数据支持
 * 3. 通过不可变设计保证线程安全性
 * </p>
 */
@Data
@RequiredArgsConstructor
public class ThreadPoolExecutorHolder {

    /**
     * 线程池唯一标识
     * <p>
     * 用于唯一标识一个线程池实例，在整个系统中保持唯一性
     * </p>
     */
    private final String threadPoolId;

    /**
     * 线程池执行器实例
     * <p>
     * 实际的线程池执行器，负责任务的调度和执行
     * </p>
     */
    private final ThreadPoolExecutor executor;

    /**
     * 线程池属性参数
     * <p>
     * 存储线程池的配置信息，用于监控和动态调整参数
     * </p>
     */
    private final ThreadPoolExecutorProperties executorProperties;
}