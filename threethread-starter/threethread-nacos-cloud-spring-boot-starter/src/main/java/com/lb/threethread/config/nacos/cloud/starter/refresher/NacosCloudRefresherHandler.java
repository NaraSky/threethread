package com.lb.threethread.config.nacos.cloud.starter.refresher;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.nacos.NacosConfigProperties;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.lb.threethread.core.executor.OneThreadRegistry;
import com.lb.threethread.core.executor.ThreadPoolExecutorHolder;
import com.lb.threethread.core.executor.ThreadPoolExecutorProperties;
import com.lb.threethread.core.executor.support.BlockingQueueTypeEnum;
import com.lb.threethread.core.executor.support.RejectedPolicyTypeEnum;
import com.lb.threethread.core.executor.support.ResizableCapacityLinkedBlockingQueue;
import com.lb.threethread.core.toolkit.ThreadPoolExecutorBuilder;
import com.lb.threethread.spring.base.configuration.BootstrapConfigProperties;
import com.lb.threethread.spring.base.parser.ConfigParserHandler;
import com.lb.threethread.spring.base.support.ApplicationContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * Nacos云配置刷新处理器
 * <p>
 * 用于监听Nacos配置中心的配置变化，并根据变化刷新动态线程池的配置
 * </p>
 * <p>
 * 设计目的：
 * 1. 监听Nacos配置中心的配置变化
 * 2. 解析配置变化并更新动态线程池参数
 * 3. 实现动态线程池的远程配置管理功能
 * </p>
 */
@Slf4j(topic = "OneThreadConfigRefresher")
@RequiredArgsConstructor
public class NacosCloudRefresherHandler implements ApplicationRunner {

    /**
     * 启动配置属性
     * <p>
     * 包含Nacos配置信息和其他启动参数
     * </p>
     */
    private final BootstrapConfigProperties properties;

    /**
     * Nacos配置服务实例
     * <p>
     * 用于与Nacos配置中心进行交互
     * </p>
     */
    private ConfigService configService;

    /**
     * 线程池参数变更日志模板
     * <p>
     * 用于格式化输出线程池参数变更信息
     * </p>
     */
    public static final String CHANGE_THREAD_POOL_TEXT = "[{}] Dynamic thread pool parameter changed:"
            + "\n    corePoolSize: {}"
            + "\n    maximumPoolSize: {}"
            + "\n    capacity: {}"
            + "\n    keepAliveTime: {}"
            + "\n    rejectedType: {}"
            + "\n    allowCoreThreadTimeOut: {}";
    
    /**
     * 参数变更分隔符模板
     * <p>
     * 用于格式化显示参数变更前后的值
     * </p>
     */
    public static final String CHANGE_DELIMITER = "%s => %s";


    /**
     * 应用启动后执行的方法
     * <p>
     * 获取Nacos配置服务实例并注册配置监听器
     * </p>
     *
     * @param args 应用启动参数
     * @throws Exception 初始化异常
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        configService = ApplicationContextHolder.getBean(NacosConfigProperties.class).configServiceInstance();
        registerListener();
    }

    /**
     * 注册Nacos配置监听器
     * <p>
     * 监听指定dataId和group的配置变化，当配置发生变化时触发回调处理
     * </p>
     *
     * @throws Exception 注册异常
     */
    @SneakyThrows
    public void registerListener() {
        BootstrapConfigProperties.NacosConfig nacosConfig = properties.getNacos();
        configService.addListener(
                nacosConfig.getDataId(),
                nacosConfig.getGroup(),
                new Listener() {
                    /**
                     * 获取监听器执行器
                     * <p>
                     * 为监听器回调方法创建专用的线程池执行器
                     * </p>
                     *
                     * @return 线程池执行器
                     */
                    @Override
                    public Executor getExecutor() {
                        return ThreadPoolExecutorBuilder.builder()
                                .corePoolSize(1)
                                .maximumPoolSize(1)
                                .keepAliveTime(9999L)
                                .workQueueType(BlockingQueueTypeEnum.SYNCHRONOUS_QUEUE)
                                .threadFactory("clod-nacos-refresher-thread_")
                                .rejectedHandler(new ThreadPoolExecutor.CallerRunsPolicy())
                                .build();
                    }

                    /**
                     * 接收配置信息变化回调方法
                     * <p>
                     * 当Nacos配置发生变化时，此方法会被调用，用于解析新配置并更新线程池参数
                     * </p>
                     *
                     * @param configInfo 配置信息内容
                     */
                    @SneakyThrows
                    @Override
                    public void receiveConfigInfo(String configInfo) {
                        Map<Object, Object> configInfoMap = ConfigParserHandler.getInstance().parseConfig(configInfo, properties.getConfigFileType());
                        ConfigurationPropertySource sources = new MapConfigurationPropertySource(configInfoMap);
                        Binder binder = new Binder(sources);
                        BootstrapConfigProperties refresherProperties = binder.bind(BootstrapConfigProperties.PREFIX, Bindable.ofInstance(properties)).get();

                        // 检查远程配置文件是否包含线程池配置
                        if (CollUtil.isEmpty(refresherProperties.getExecutors())) {
                            return;
                        }

                        // 刷新动态线程池对象核心参数
                        for (ThreadPoolExecutorProperties remoteProperties : refresherProperties.getExecutors()) {
                            // 检查线程池配置是否发生变化（与当前内存中的配置对比）
                            boolean changed = hasThreadPoolConfigChanged(remoteProperties);
                            if (!changed) {
                                continue;
                            }

                            // 将远程配置应用到线程池，更新相关参数
                            updateThreadPoolFromRemoteConfig(remoteProperties);

                            // 线程池参数变更后进行日志打印
                            String threadPoolId = remoteProperties.getThreadPoolId();
                            ThreadPoolExecutorHolder holder = OneThreadRegistry.getHolder(threadPoolId);
                            ThreadPoolExecutorProperties originalProperties = holder.getExecutorProperties();
                            holder.setExecutorProperties(remoteProperties);
                            log.info(CHANGE_THREAD_POOL_TEXT,
                                    threadPoolId,
                                    String.format(CHANGE_DELIMITER, originalProperties.getCorePoolSize(), remoteProperties.getCorePoolSize()),
                                    String.format(CHANGE_DELIMITER, originalProperties.getMaximumPoolSize(), remoteProperties.getMaximumPoolSize()),
                                    String.format(CHANGE_DELIMITER, originalProperties.getQueueCapacity(), remoteProperties.getQueueCapacity()),
                                    String.format(CHANGE_DELIMITER, originalProperties.getKeepAliveTime(), remoteProperties.getKeepAliveTime()),
                                    String.format(CHANGE_DELIMITER, originalProperties.getRejectedHandler(), remoteProperties.getRejectedHandler()),
                                    String.format(CHANGE_DELIMITER, originalProperties.getAllowCoreThreadTimeOut(), remoteProperties.getAllowCoreThreadTimeOut()));
                        }
                    }
                });

        log.info("Dynamic thread pool refresher, add nacos cloud listener success. data-id: {}, group: {}", nacosConfig.getDataId(), nacosConfig.getGroup());
    }

    /**
     * 检查线程池配置是否发生变化
     * <p>
     * 对比远程配置与当前内存中的配置，判断是否需要更新线程池参数
     * </p>
     *
     * @param remoteProperties 远程配置属性
     * @return 配置是否发生变化
     */
    private boolean hasThreadPoolConfigChanged(ThreadPoolExecutorProperties remoteProperties) {
        String threadPoolId = remoteProperties.getThreadPoolId();
        ThreadPoolExecutorHolder holder = OneThreadRegistry.getHolder(threadPoolId);
        if (holder == null) {
            log.warn("No thread pool found for thread pool id: {}", threadPoolId);
            return false;
        }
        ThreadPoolExecutor executor = holder.getExecutor();
        ThreadPoolExecutorProperties originalProperties = holder.getExecutorProperties();

        return hasDifference(originalProperties, remoteProperties, executor);
    }

    /**
     * 根据远程配置更新线程池参数
     * <p>
     * 将远程配置中的参数应用到线程池执行器中，支持动态调整以下参数：
     * 1. 核心线程数和最大线程数（考虑设置顺序避免异常）
     * 2. 是否允许核心线程超时
     * 3. 拒绝策略
     * 4. 线程空闲存活时间
     * 5. 队列容量（仅支持ResizableCapacityLinkedBlockingQueue）
     * </p>
     *
     * @param remoteProperties 远程配置属性，包含需要更新的线程池参数
     */
    private void updateThreadPoolFromRemoteConfig(ThreadPoolExecutorProperties remoteProperties) {
        String threadPoolId = remoteProperties.getThreadPoolId();
        ThreadPoolExecutorHolder holder = OneThreadRegistry.getHolder(threadPoolId);
        ThreadPoolExecutor executor = holder.getExecutor();
        ThreadPoolExecutorProperties originalProperties = holder.getExecutorProperties();

        // 更新核心线程数和最大线程数，根据大小关系确定设置顺序避免IllegalArgumentException
        Integer remoteCorePoolSize = remoteProperties.getCorePoolSize();
        Integer remoteMaximumPoolSize = remoteProperties.getMaximumPoolSize();
        if (remoteCorePoolSize != null && remoteMaximumPoolSize != null) {
            int originalMaximumPoolSize = executor.getMaximumPoolSize();
            if (remoteCorePoolSize > originalMaximumPoolSize) {
                // 如果新的核心线程数大于原来的最大线程数，先设置最大线程数再设置核心线程数
                executor.setMaximumPoolSize(remoteMaximumPoolSize);
                executor.setCorePoolSize(remoteCorePoolSize);
            } else {
                // 否则先设置核心线程数再设置最大线程数
                executor.setCorePoolSize(remoteCorePoolSize);
                executor.setMaximumPoolSize(remoteMaximumPoolSize);
            }
        } else {
            // 单独更新最大线程数或核心线程数
            if (remoteMaximumPoolSize != null) {
                executor.setMaximumPoolSize(remoteMaximumPoolSize);
            }
            if (remoteCorePoolSize != null) {
                executor.setCorePoolSize(remoteCorePoolSize);
            }
        }

        // 更新是否允许核心线程超时设置
        if (remoteProperties.getAllowCoreThreadTimeOut() != null &&
                !Objects.equals(remoteProperties.getAllowCoreThreadTimeOut(), originalProperties.getAllowCoreThreadTimeOut())) {
            executor.allowCoreThreadTimeOut(remoteProperties.getAllowCoreThreadTimeOut());
        }

        // 更新拒绝策略
        if (remoteProperties.getRejectedHandler() != null &&
                !Objects.equals(remoteProperties.getRejectedHandler(), originalProperties.getRejectedHandler())) {
            RejectedExecutionHandler handler = RejectedPolicyTypeEnum.createPolicy(remoteProperties.getRejectedHandler());
            executor.setRejectedExecutionHandler(handler);
        }

        // 更新线程空闲存活时间
        if (remoteProperties.getKeepAliveTime() != null &&
                !Objects.equals(remoteProperties.getKeepAliveTime(), originalProperties.getKeepAliveTime())) {
            executor.setKeepAliveTime(remoteProperties.getKeepAliveTime(), TimeUnit.SECONDS);
        }

        // 更新队列容量（仅对 ResizableCapacityLinkedBlockingQueue 生效）
        if (isQueueCapacityChanged(originalProperties, remoteProperties, executor)) {
            BlockingQueue<Runnable> queue = executor.getQueue();
            ResizableCapacityLinkedBlockingQueue<?> resizableQueue = (ResizableCapacityLinkedBlockingQueue<?>) queue;
            resizableQueue.setCapacity(remoteProperties.getQueueCapacity());
        }
    }

    /**
     * 检查线程池配置是否存在差异
     * <p>
     * 对比原始配置和远程配置的各项参数，判断是否存在差异
     * </p>
     *
     * @param originalProperties 原始配置属性
     * @param remoteProperties   远程配置属性
     * @param executor           线程池执行器
     * @return 是否存在配置差异
     */
    private boolean hasDifference(ThreadPoolExecutorProperties originalProperties, ThreadPoolExecutorProperties remoteProperties, ThreadPoolExecutor executor) {
        return isChanged(originalProperties.getCorePoolSize(), remoteProperties.getCorePoolSize())
                || isChanged(originalProperties.getMaximumPoolSize(), remoteProperties.getMaximumPoolSize())
                || isChanged(originalProperties.getAllowCoreThreadTimeOut(), remoteProperties.getAllowCoreThreadTimeOut())
                || isChanged(originalProperties.getKeepAliveTime(), remoteProperties.getKeepAliveTime())
                || isChanged(originalProperties.getRejectedHandler(), remoteProperties.getRejectedHandler())
                || isQueueCapacityChanged(originalProperties, remoteProperties, executor);
    }

    /**
     * 检查单个参数是否发生变化
     * <p>
     * 对比参数的前后值，判断是否发生变化
     * </p>
     *
     * @param before 变更前的值
     * @param after  变更后的值
     * @param <T>    参数类型
     * @return 参数是否发生变化
     */
    private <T> boolean isChanged(T before, T after) {
        return after != null && !Objects.equals(before, after);
    }

    /**
     * 检查队列容量是否发生变化
     * <p>
     * 特殊处理队列容量变更检查，只对ResizableCapacityLinkedBlockingQueue类型生效
     * 因为只有这种队列类型支持运行时动态调整容量
     * </p>
     *
     * @param originalProperties 原始配置属性，包含变更前的队列容量配置
     * @param remoteProperties   远程配置属性，包含变更后的队列容量配置
     * @param executor           线程池执行器，用于获取当前实际使用的队列实例
     * @return 队列容量是否发生变化且队列类型支持动态调整，true表示可以更新队列容量，false表示不能更新
     */
    private boolean isQueueCapacityChanged(ThreadPoolExecutorProperties originalProperties,
                                           ThreadPoolExecutorProperties remoteProperties,
                                           ThreadPoolExecutor executor) {
        Integer remoteCapacity = remoteProperties.getQueueCapacity();
        Integer originalCapacity = originalProperties.getQueueCapacity();
        BlockingQueue<?> queue = executor.getQueue();

        return remoteCapacity != null
                && !Objects.equals(remoteCapacity, originalCapacity)
                && Objects.equals(BlockingQueueTypeEnum.RESIZABLE_CAPACITY_LINKED_BLOCKING_QUEUE.getName(), queue.getClass().getSimpleName());
    }
}