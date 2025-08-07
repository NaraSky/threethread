package com.lb.threethread.config.common.starter.refresher;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import com.lb.threethread.core.executor.OneThreadRegistry;
import com.lb.threethread.core.executor.ThreadPoolExecutorHolder;
import com.lb.threethread.core.executor.ThreadPoolExecutorProperties;
import com.lb.threethread.core.executor.support.BlockingQueueTypeEnum;
import com.lb.threethread.core.executor.support.RejectedPolicyTypeEnum;
import com.lb.threethread.core.executor.support.ResizableCapacityLinkedBlockingQueue;
import com.lb.threethread.core.notification.dto.ThreadPoolConfigChangeDTO;
import com.lb.threethread.core.notification.service.DingTalkMessageService;
import com.lb.threethread.core.notification.service.NotifierDispatcher;
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
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.lb.threethread.core.constant.Constants.CHANGE_DELIMITER;
import static com.lb.threethread.core.constant.Constants.CHANGE_THREAD_POOL_TEXT;

/**
 * 动态线程池刷新器抽象类
 * <p>
 * 主要功能：
 * 1. 提供统一的线程池动态刷新框架
 * 2. 解析配置文件并转换为线程池配置对象
 * 3. 比较配置差异并更新线程池参数
 * 4. 记录线程池参数变更日志
 * </p>
 * <p>
 * 设计特点：
 * 1. 实现ApplicationRunner接口，在Spring Boot应用启动完成后执行注册监听器
 * 2. 使用模板方法模式，将具体的监听器注册逻辑交给子类实现
 * 3. 提供beforeRegister和afterRegister钩子方法，供子类扩展
 * 4. 集成Spring Boot Configuration Processor，支持配置绑定
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractDynamicThreadPoolRefresher implements ApplicationRunner {

    /**
     * 启动配置属性，包含各种配置中心的配置信息
     */
    protected final BootstrapConfigProperties properties;
    protected final NotifierDispatcher notifierDispatcher;

    /**
     * 注册配置变更监听器，由子类实现具体逻辑
     * <p>
     * 不同配置中心（如Nacos、Apollo）有不同的监听机制
     * 子类需要根据具体的配置中心实现监听器注册逻辑
     * </p>
     *
     * @throws Exception 监听器注册异常
     */
    protected abstract void registerListener() throws Exception;

    /**
     * 注册监听器前的处理方法，默认空实现，子类可以按需覆盖
     * <p>
     * 可用于执行注册前的准备工作，如初始化资源、校验配置等
     * </p>
     */
    protected void beforeRegister() {
    }

    /**
     * 注册监听器后的处理方法，默认空实现，子类可以按需覆盖
     * <p>
     * 可用于执行注册后的清理工作或通知逻辑
     * </p>
     */
    protected void afterRegister() {
    }

    /**
     * Spring Boot应用启动完成后执行的方法
     * <p>
     * 执行顺序：
     * 1. 调用beforeRegister方法
     * 2. 调用registerListener方法注册监听器
     * 3. 调用afterRegister方法
     * </p>
     * 
     * @param args 应用启动参数
     * @throws Exception 启动过程中的异常
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        beforeRegister();
        registerListener();
        afterRegister();
    }

    /**
     * 刷新线程池配置
     * <p>
     * 处理流程：
     * 1. 解析配置文件内容为Map对象
     * 2. 将Map对象绑定到BootstrapConfigProperties配置对象
     * 3. 遍历线程池配置列表
     * 4. 检查配置是否发生变化
     * 5. 如果发生变化则更新线程池参数
     * 6. 记录参数变更日志
     * </p>
     *
     * @param configInfo 配置文件内容
     */
    @SneakyThrows
    public void refreshThreadPoolProperties(String configInfo) {
        // 解析配置文件内容为Map对象
        Map<Object, Object> configInfoMap = ConfigParserHandler.getInstance().parseConfig(configInfo, properties.getConfigFileType());
        // 创建配置属性源
        ConfigurationPropertySource sources = new MapConfigurationPropertySource(configInfoMap);
        // 创建配置绑定器
        Binder binder = new Binder(sources);
        // 将配置绑定到BootstrapConfigProperties对象
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

            // 发送线程池配置变更消息通知
            sendThreadPoolConfigChangeMessage(properties, originalProperties, remoteProperties);

            // 打印线程池配置变更日志
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

    /**
     * 检查线程池配置是否发生变化
     * <p>
     * 实现原理：
     * 1. 从线程池注册中心获取当前线程池持有者
     * 2. 获取当前线程池执行器和原始配置
     * 3. 比较各项配置是否发生变化
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
     * 更新顺序和注意事项：
     * 1. 核心线程数和最大线程数需要按特定顺序更新，避免出现临时的非法状态
     * 2. 先更新拒绝策略、存活时间等简单参数
     * 3. 最后更新队列容量（仅对可调整容量队列生效）
     * </p>
     * 
     * @param remoteProperties 远程配置属性
     */
    private void updateThreadPoolFromRemoteConfig(ThreadPoolExecutorProperties remoteProperties) {
        String threadPoolId = remoteProperties.getThreadPoolId();
        ThreadPoolExecutorHolder holder = OneThreadRegistry.getHolder(threadPoolId);
        ThreadPoolExecutor executor = holder.getExecutor();
        ThreadPoolExecutorProperties originalProperties = holder.getExecutorProperties();

        // 更新核心线程数和最大线程数
        Integer remoteCorePoolSize = remoteProperties.getCorePoolSize();
        Integer remoteMaximumPoolSize = remoteProperties.getMaximumPoolSize();
        if (remoteCorePoolSize != null && remoteMaximumPoolSize != null) {
            int originalMaximumPoolSize = executor.getMaximumPoolSize();
            // 如果新的核心线程数大于原始最大线程数，需要先调整最大线程数
            if (remoteCorePoolSize > originalMaximumPoolSize) {
                executor.setMaximumPoolSize(remoteMaximumPoolSize);
                executor.setCorePoolSize(remoteCorePoolSize);
            } else {
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

        // 更新核心线程是否允许超时设置
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

        // 更新线程空闲时间
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
     * 检查线程池各项配置是否发生变化
     * 
     * @param originalProperties 原始配置属性
     * @param remoteProperties 远程配置属性
     * @param executor 线程池执行器
     * @return 是否存在配置差异
     */
    private boolean hasDifference(ThreadPoolExecutorProperties originalProperties,
                                  ThreadPoolExecutorProperties remoteProperties,
                                  ThreadPoolExecutor executor) {
        return isChanged(originalProperties.getCorePoolSize(), remoteProperties.getCorePoolSize())
                || isChanged(originalProperties.getMaximumPoolSize(), remoteProperties.getMaximumPoolSize())
                || isChanged(originalProperties.getAllowCoreThreadTimeOut(), remoteProperties.getAllowCoreThreadTimeOut())
                || isChanged(originalProperties.getKeepAliveTime(), remoteProperties.getKeepAliveTime())
                || isChanged(originalProperties.getRejectedHandler(), remoteProperties.getRejectedHandler())
                || isQueueCapacityChanged(originalProperties, remoteProperties, executor);
    }

    /**
     * 检查单个配置项是否发生变化
     * 
     * @param before 变更前的值
     * @param after 变更后的值
     * @param <T> 配置项类型
     * @return 是否发生变化
     */
    private <T> boolean isChanged(T before, T after) {
        return after != null && !Objects.equals(before, after);
    }

    /**
     * 检查队列容量是否发生变化
     * <p>
     * 注意：仅对ResizableCapacityLinkedBlockingQueue类型的队列生效
     * </p>
     * 
     * @param originalProperties 原始配置属性
     * @param remoteProperties 远程配置属性
     * @param executor 线程池执行器
     * @return 队列容量是否发生变化
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

    /**
     * 发送线程池配置变更消息
     *
     * @param originalProperties 原始线程池配置属性
     * @param remoteProperties 远程线程池配置属性
     */
    @SneakyThrows
    private void sendThreadPoolConfigChangeMessage(BootstrapConfigProperties properties,
                                                   ThreadPoolExecutorProperties originalProperties,
                                                   ThreadPoolExecutorProperties remoteProperties) {
        // 获取环境配置信息
        Environment environment = ApplicationContextHolder.getBean(Environment.class);
        String active = environment.getProperty("spring.profiles.active", "dev");
        String applicationName = environment.getProperty("spring.application.name");

        // 构建配置变更映射表
        Map<String, ThreadPoolConfigChangeDTO.ChangePair<?>> changes = new HashMap<>();
        changes.put("corePoolSize", new ThreadPoolConfigChangeDTO.ChangePair<>(originalProperties.getCorePoolSize(), remoteProperties.getCorePoolSize()));
        changes.put("maximumPoolSize", new ThreadPoolConfigChangeDTO.ChangePair<>(originalProperties.getMaximumPoolSize(), remoteProperties.getMaximumPoolSize()));
        changes.put("queueCapacity", new ThreadPoolConfigChangeDTO.ChangePair<>(originalProperties.getQueueCapacity(), remoteProperties.getQueueCapacity()));
        changes.put("rejectedHandler", new ThreadPoolConfigChangeDTO.ChangePair<>(originalProperties.getRejectedHandler(), remoteProperties.getRejectedHandler()));
        changes.put("keepAliveTime", new ThreadPoolConfigChangeDTO.ChangePair<>(originalProperties.getKeepAliveTime(), remoteProperties.getKeepAliveTime()));

        // 构建线程池配置变更DTO并发送消息
        ThreadPoolConfigChangeDTO configChangeDTO = ThreadPoolConfigChangeDTO.builder()
                .active(active)
                .identify(InetAddress.getLocalHost().getHostAddress())
                .applicationName(applicationName)
                .threadPoolId(originalProperties.getThreadPoolId())
                .receives(remoteProperties.getNotify().getReceives())
                .workQueue(originalProperties.getWorkQueue())
                .changes(changes)
                .updateTime(DateUtil.now())
                .notifyPlatforms(BeanUtil.toBean(properties.getNotifyPlatforms(), ThreadPoolConfigChangeDTO.NotifyPlatformsConfig.class))
                .build();
        notifierDispatcher.sendChangeMessage(configChangeDTO);
    }

}