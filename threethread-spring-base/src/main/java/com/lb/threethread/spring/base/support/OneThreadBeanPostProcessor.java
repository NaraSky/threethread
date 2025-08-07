package com.lb.threethread.spring.base.support;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ReflectUtil;
import com.lb.threethread.core.executor.OneThreadExecutor;
import com.lb.threethread.core.executor.OneThreadRegistry;
import com.lb.threethread.core.executor.ThreadPoolExecutorProperties;
import com.lb.threethread.core.executor.support.BlockingQueueTypeEnum;
import com.lb.threethread.core.executor.support.RejectedPolicyTypeEnum;
import com.lb.threethread.spring.base.DynamicThreadPool;
import com.lb.threethread.core.config.BootstrapConfigProperties;
import lombok.RequiredArgsConstructor;
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
 * 4. 从配置中心读取配置并覆盖本地线程池配置，实现动态配置功能
 * </p>
 * <p>
 * 工作流程：
 * 1. 监听Spring容器中所有Bean的初始化完成事件
 * 2. 识别出OneThreadExecutor类型的Bean并检查是否带有@DynamicThreadPool注解
 * 3. 从配置中心获取对应的线程池配置并覆盖本地配置
 * 4. 将线程池注册到统一管理器中
 * 5. 构建线程池属性信息用于后续比对和管理
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class OneThreadBeanPostProcessor implements BeanPostProcessor {

    /**
     * 启动配置属性
     * <p>
     * 包含从配置文件或配置中心获取的线程池配置信息
     * </p>
     */
    private final BootstrapConfigProperties properties;

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
            // 从配置中心读取动态线程池配置并对线程池进行赋值
            overrideLocalThreadPoolConfig(properties, oneThreadExecutor);

            // 注册到动态线程池注册器，后续监控和报警从注册器获取线程池实例。同时，参数动态变更需要依赖 ThreadPoolExecutorProperties 比对是否有边跟
            OneThreadRegistry.put(oneThreadExecutor.getThreadPoolId(), oneThreadExecutor, buildDefaultExecutorProperties(oneThreadExecutor));
        }

        return bean;
    }

    /**
     * 覆盖本地线程池配置
     * <p>
     * 从配置中心获取线程池配置并覆盖本地线程池配置，实现动态配置功能
     * </p>
     *
     * @param properties         启动配置属性
     * @param oneThreadExecutor  动态线程池执行器
     */
    private void overrideLocalThreadPoolConfig(BootstrapConfigProperties properties, OneThreadExecutor oneThreadExecutor) {
        // 从配置属性中查找当前线程池ID对应的配置
        ThreadPoolExecutorProperties executorProperties = properties.getExecutors()
                .stream()
                .filter(each -> Objects.equals(oneThreadExecutor.getThreadPoolId(), each.getThreadPoolId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("The thread pool id does not exist in the configuration."));

        // 获取远程配置的核心线程数和最大线程数
        Integer remoteCorePoolSize = executorProperties.getCorePoolSize();
        Integer remoteMaximumPoolSize = executorProperties.getMaximumPoolSize();
        
        // 校验核心线程数不能大于最大线程数
        Assert.isTrue(remoteCorePoolSize <= remoteMaximumPoolSize, "remoteCorePoolSize must be smaller than remoteMaximumPoolSize.");

        // 如果不清楚为什么有这段逻辑，可以参考 Hippo4j Issue https://github.com/opengoofy/hippo4j/issues/1063
        // 处理核心线程数和最大线程数的设置顺序问题，避免在某些情况下设置失败
        int originalMaximumPoolSize = oneThreadExecutor.getMaximumPoolSize();
        if (remoteCorePoolSize > originalMaximumPoolSize) {
            // 当新的核心线程数大于原始最大线程数时，先设置最大线程数再设置核心线程数
            oneThreadExecutor.setMaximumPoolSize(remoteMaximumPoolSize);
            oneThreadExecutor.setCorePoolSize(remoteCorePoolSize);
        } else {
            // 正常情况下先设置核心线程数再设置最大线程数
            oneThreadExecutor.setCorePoolSize(remoteCorePoolSize);
            oneThreadExecutor.setMaximumPoolSize(remoteMaximumPoolSize);
        }

        // 阻塞队列没有常规 set 方法，所以使用反射赋值
        BlockingQueue workQueue = BlockingQueueTypeEnum.createBlockingQueue(executorProperties.getWorkQueue(), executorProperties.getQueueCapacity());
        // Java 9+ 的模块系统（JPMS）默认禁止通过反射访问 JDK 内部 API 的私有字段，所以需要配置开放反射权限
        // 在启动命令中增加以下参数，显式开放 java.util.concurrent 包
        // IDE 中通过在 VM options 中添加参数：--add-opens=java.base/java.util.concurrent=ALL-UNNAMED
        // 部署的时候，在启动脚本（如 java -jar 命令）中加入该参数：java -jar --add-opens=java.base/java.util.concurrent=ALL-UNNAMED your-app.jar
        ReflectUtil.setFieldValue(oneThreadExecutor, "workQueue", workQueue);

        // 赋值动态线程池其他核心参数
        // 设置线程空闲存活时间
        oneThreadExecutor.setKeepAliveTime(executorProperties.getKeepAliveTime(), TimeUnit.SECONDS);
        // 设置是否允许核心线程超时
        oneThreadExecutor.allowCoreThreadTimeOut(executorProperties.getAllowCoreThreadTimeOut());
        // 设置拒绝策略
        oneThreadExecutor.setRejectedExecutionHandler(RejectedPolicyTypeEnum.createPolicy(executorProperties.getRejectedHandler()));
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
        // 获取线程池的阻塞队列信息
        BlockingQueue<Runnable> blockingQueue = executor.getQueue();
        int queueSize = blockingQueue.size();
        String queueType = blockingQueue.getClass().getSimpleName();
        int remainingCapacity = blockingQueue.remainingCapacity();
        int queueCapacity = queueSize + remainingCapacity;
        
        // 构建线程池属性对象
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