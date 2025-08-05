package com.lb.threethread.spring.base.support;

import com.lb.threethread.core.executor.OneThreadExecutor;
import com.lb.threethread.core.executor.OneThreadRegistry;
import com.lb.threethread.core.executor.ThreadPoolExecutorProperties;
import com.lb.threethread.spring.base.DynamicThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 动态线程池Bean后置处理器
 * <p>
 * 用于处理Spring容器中的动态线程池Bean，在Bean初始化完成后进行注册和管理
 * </p>
 * <p>
 * 设计目的：
 * 1. 自动识别Spring容器中的动态线程池Bean并进行注册
 * 2. 将动态线程池注册到统一管理器中，便于后续监控和参数调整
 * 3. 构建线程池配置属性，为动态调整提供数据支持
 * </p>
 */
@Slf4j
public class OneThreadBeanPostProcessor implements BeanPostProcessor {

    /**
     * Bean初始化后处理方法
     * <p>
     * 在Bean初始化完成后调用，用于识别和处理动态线程池Bean
     * </p>
     *
     * @param bean     Bean实例
     * @param beanName Bean名称
     * @return 处理后的Bean实例
     * @throws BeansException Bean处理异常
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 判断是否为动态线程池执行器实例
        if (bean instanceof OneThreadExecutor) {
            DynamicThreadPool dynamicThreadPool;
            try {
                // 通过 IOC 容器扫描 Bean 是否存在动态线程池注解
                dynamicThreadPool = ApplicationContextHolder.findAnnotationOnBean(beanName, DynamicThreadPool.class);
                if (Objects.isNull(dynamicThreadPool)) {
                    return bean;
                }
            } catch (Exception ex) {
                log.error("Failed to create dynamic thread pool in annotation mode.", ex);
                return bean;
            }

            OneThreadExecutor oneThreadExecutor = (OneThreadExecutor) bean;
            // TODO 需要从配置中心读取动态线程池配置并对线程池进行赋值

            // 注册到动态线程池注册器，后续监控和报警从注册器获取线程池实例。同时，参数动态变更需要依赖 ThreadPoolExecutorProperties 比对是否有边跟
            OneThreadRegistry.put(oneThreadExecutor.getThreadPoolId(), oneThreadExecutor, buildDefaultExecutorProperties(oneThreadExecutor));
        }

        return bean;
    }

    /**
     * 构建默认的线程池执行器属性
     * <p>
     * 从线程池执行器中提取当前配置信息，构建成属性对象用于后续比对和管理
     * </p>
     *
     * @param executor 线程池执行器
     * @return 线程池执行器属性
     */
    private ThreadPoolExecutorProperties buildDefaultExecutorProperties(OneThreadExecutor executor) {
        BlockingQueue<Runnable> blockingQueue = executor.getQueue();
        int queueSize = blockingQueue.size();
        String queueType = blockingQueue.getClass().getSimpleName();
        int remainingCapacity = blockingQueue.remainingCapacity();
        int queueCapacity = queueSize + remainingCapacity;
        ThreadPoolExecutorProperties executorProperties = new ThreadPoolExecutorProperties();
        executorProperties.setCorePoolSize(executor.getCorePoolSize())
                .setMaximumPoolSize(executor.getMaximumPoolSize())
                .setAllowCoreThreadTimeOut(executor.allowsCoreThreadTimeOut())
                .setKeepAliveTime(executor.getKeepAliveTime(TimeUnit.SECONDS))
                .setWorkQueue(queueType)
                .setQueueCapacity(queueCapacity)
                .setRejectedHandler(executor.getRejectedExecutionHandler().getClass().getSimpleName())
                .setThreadPoolId(executor.getThreadPoolId());
        return executorProperties;
    }
}