package com.lb.threethread.core.executor;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 动态线程池注册中心
 * <p>
 * 用于统一管理和维护所有动态线程池实例，提供线程池的注册和获取功能
 * </p>
 * <p>
 * 设计目的：
 * 1. 提供全局的线程池管理容器，统一管理所有动态线程池实例
 * 2. 支持通过线程池ID快速获取对应的线程池实例
 * 3. 为动态线程池的监控和参数调整提供基础支持
 * </p>
 */
public class OneThreadRegistry {

    /**
     * 线程池持有者缓存，key 为线程池唯一标识，value 为线程池包装类
     * <p>
     * 使用线程安全的ConcurrentHashMap实现，支持并发访问
     * 存储结构为[threadPoolId -> ThreadPoolExecutorHolder]
     * </p>
     */
    private static final Map<String, ThreadPoolExecutorHolder> HOLDER_MAP = new ConcurrentHashMap<>();

    /**
     * 注册线程池到管理器
     * <p>
     * 将线程池实例和其配置信息包装后存储到注册中心，
     * 便于后续通过线程池ID获取和管理
     * </p>
     *
     * @param threadPoolId 线程池唯一标识
     * @param executor     线程池执行器实例
     * @param properties   线程池参数配置
     */
    public static void put(String threadPoolId, ThreadPoolExecutor executor, ThreadPoolExecutorProperties properties) {
        ThreadPoolExecutorHolder executorHolder = new ThreadPoolExecutorHolder(threadPoolId, executor, properties);
        HOLDER_MAP.put(threadPoolId, executorHolder);
    }

    /**
     * 根据线程池 ID 获取对应的线程池包装对象
     * <p>
     * 通过线程池唯一标识获取对应的线程池包装对象，
     * 如果未找到则抛出运行时异常
     * </p>
     *
     * @param threadPoolId 线程池唯一标识
     * @return 线程池持有者对象
     * @throws RuntimeException 如果未找到对应线程池
     */
    public static ThreadPoolExecutorHolder getHolder(String threadPoolId) {
        return Optional.ofNullable(HOLDER_MAP.get(threadPoolId))
                .orElseThrow(() -> new RuntimeException("No thread pool executor found for id: " + threadPoolId));
    }
}