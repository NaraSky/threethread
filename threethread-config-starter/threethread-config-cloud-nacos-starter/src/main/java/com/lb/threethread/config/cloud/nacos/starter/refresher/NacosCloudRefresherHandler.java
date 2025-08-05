package com.lb.threethread.config.cloud.nacos.starter.refresher;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.nacos.NacosConfigProperties;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.lb.threethread.core.executor.OneThreadRegistry;
import com.lb.threethread.core.executor.ThreadPoolExecutorHolder;
import com.lb.threethread.core.executor.ThreadPoolExecutorProperties;
import com.lb.threethread.core.executor.support.BlockingQueueTypeEnum;
import com.lb.threethread.core.executor.support.RejectedPolicyTypeEnum;
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
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
@Slf4j
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
    public static final String CHANGE_THREAD_POOL_TEXT = "[{}] \uD83E\uDDF5 Dynamic thread pool parameter changed:"
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
     * @param remateProperties 远程配置属性
     * @return 配置是否发生变化
     */
    private boolean hasThreadPoolConfigChanged(ThreadPoolExecutorProperties remateProperties) {
        String threadPoolId = remateProperties.getThreadPoolId();
        ThreadPoolExecutorHolder holder = OneThreadRegistry.getHolder(threadPoolId);
        if (holder == null) {
            log.warn("No thread pool found for thread pool id: {}", threadPoolId);
            return false;
        }
        ThreadPoolExecutor executor = holder.getExecutor();
        ThreadPoolExecutorProperties originalProperties = holder.getExecutorProperties();

        return hasDifference(originalProperties, remateProperties, executor);
    }

    /**
     * 根据远程配置更新线程池参数
     * <p>
     * 将远程配置中的参数应用到线程池执行器中
     * </p>
     *
     * @param remoteProperties 远程配置属性
     */
    private void updateThreadPoolFromRemoteConfig(ThreadPoolExecutorProperties remoteProperties) {
        String threadPoolId = remoteProperties.getThreadPoolId();
        ThreadPoolExecutorHolder holder = OneThreadRegistry.getHolder(threadPoolId);
        ThreadPoolExecutor executor = holder.getExecutor();
        ThreadPoolExecutorProperties originalProperties = holder.getExecutorProperties();

        Integer remoteCorePoolSize = remoteProperties.getCorePoolSize();
        Integer remoteMaximumPoolSize = remoteProperties.getMaximumPoolSize();
        if (remoteCorePoolSize != null && remoteMaximumPoolSize != null) {
            int originalMaximumPoolSize = executor.getMaximumPoolSize();
            if (remoteCorePoolSize > originalMaximumPoolSize) {
                executor.setMaximumPoolSize(remoteMaximumPoolSize);
                executor.setCorePoolSize(remoteCorePoolSize);
            } else {
                executor.setCorePoolSize(remoteCorePoolSize);
                executor.setMaximumPoolSize(remoteMaximumPoolSize);
            }
        } else {
            if (remoteMaximumPoolSize != null) {
                executor.setMaximumPoolSize(remoteMaximumPoolSize);
            }
            if (remoteCorePoolSize != null) {
                executor.setCorePoolSize(remoteCorePoolSize);
            }
        }

        if (remoteProperties.getAllowCoreThreadTimeOut() != null &&
                !Objects.equals(remoteProperties.getAllowCoreThreadTimeOut(), originalProperties.getAllowCoreThreadTimeOut())) {
            executor.allowCoreThreadTimeOut(remoteProperties.getAllowCoreThreadTimeOut());
        }

        if (remoteProperties.getRejectedHandler() != null &&
                !Objects.equals(remoteProperties.getRejectedHandler(), originalProperties.getRejectedHandler())) {
            RejectedExecutionHandler handler = RejectedPolicyTypeEnum.createPolicy(remoteProperties.getRejectedHandler());
            executor.setRejectedExecutionHandler(handler);
        }

        if (remoteProperties.getKeepAliveTime() != null &&
                !Objects.equals(remoteProperties.getKeepAliveTime(), originalProperties.getKeepAliveTime())) {
            executor.setKeepAliveTime(remoteProperties.getKeepAliveTime(), TimeUnit.SECONDS);
        }
    }

    /**
     * 检查线程池配置是否存在差异
     * <p>
     * 对比原始配置和远程配置的各项参数，判断是否存在差异
     * </p>
     *
     * @param originalProperties 原始配置属性
     * @param remateProperties   远程配置属性
     * @param executor           线程池执行器
     * @return 是否存在配置差异
     */
    private boolean hasDifference(ThreadPoolExecutorProperties originalProperties, ThreadPoolExecutorProperties remateProperties, ThreadPoolExecutor executor) {
        return isChanged(originalProperties.getCorePoolSize(), remateProperties.getCorePoolSize())
                || isChanged(originalProperties.getMaximumPoolSize(), remateProperties.getMaximumPoolSize())
                || isChanged(originalProperties.getAllowCoreThreadTimeOut(), remateProperties.getAllowCoreThreadTimeOut())
                || isChanged(originalProperties.getKeepAliveTime(), remateProperties.getKeepAliveTime())
                || isChanged(originalProperties.getRejectedHandler(), remateProperties.getRejectedHandler());
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
}