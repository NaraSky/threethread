package com.lb.threethread.config.common.starter.refresher;

import cn.hutool.core.collection.CollUtil;
import com.lb.threethread.core.executor.OneThreadRegistry;
import com.lb.threethread.core.executor.ThreadPoolExecutorHolder;
import com.lb.threethread.core.executor.ThreadPoolExecutorProperties;
import com.lb.threethread.core.executor.support.BlockingQueueTypeEnum;
import com.lb.threethread.core.executor.support.RejectedPolicyTypeEnum;
import com.lb.threethread.core.executor.support.ResizableCapacityLinkedBlockingQueue;
import com.lb.threethread.spring.base.configuration.BootstrapConfigProperties;
import com.lb.threethread.spring.base.parser.ConfigParserHandler;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractDynamicThreadPoolRefresher implements ApplicationRunner {

    protected final BootstrapConfigProperties properties;

    /**
     * 注册配置变更监听器，由子类实现具体逻辑
     *
     * @throws Exception
     */
    protected abstract void registerListener() throws Exception;

    /**
     * 默认空实现，子类可以按需覆盖
     */
    protected void beforeRegister() {
    }

    /**
     * 默认空实现，子类可以按需覆盖
     */
    protected void afterRegister() {
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        beforeRegister();
        registerListener();
        afterRegister();
    }

    public static final String CHANGE_THREAD_POOL_TEXT = "[{}] Dynamic thread pool parameter changed:"
            + "\n    corePoolSize: {}"
            + "\n    maximumPoolSize: {}"
            + "\n    capacity: {}"
            + "\n    keepAliveTime: {}"
            + "\n    rejectedType: {}"
            + "\n    allowCoreThreadTimeOut: {}";
    public static final String CHANGE_DELIMITER = "%s => %s";

    @SneakyThrows
    public void refreshThreadPoolProperties(String configInfo) {
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

        // 更新队列容量（仅对 ResizableCapacityLinkedBlockingQueue 生效）
        if (isQueueCapacityChanged(originalProperties, remoteProperties, executor)) {
            BlockingQueue<Runnable> queue = executor.getQueue();
            ResizableCapacityLinkedBlockingQueue<?> resizableQueue = (ResizableCapacityLinkedBlockingQueue<?>) queue;
            resizableQueue.setCapacity(remoteProperties.getQueueCapacity());
        }
    }

    private boolean hasDifference(ThreadPoolExecutorProperties originalProperties, ThreadPoolExecutorProperties remoteProperties, ThreadPoolExecutor executor) {
        return isChanged(originalProperties.getCorePoolSize(), remoteProperties.getCorePoolSize())
                || isChanged(originalProperties.getMaximumPoolSize(), remoteProperties.getMaximumPoolSize())
                || isChanged(originalProperties.getAllowCoreThreadTimeOut(), remoteProperties.getAllowCoreThreadTimeOut())
                || isChanged(originalProperties.getKeepAliveTime(), remoteProperties.getKeepAliveTime())
                || isChanged(originalProperties.getRejectedHandler(), remoteProperties.getRejectedHandler())
                || isQueueCapacityChanged(originalProperties, remoteProperties, executor);
    }

    private <T> boolean isChanged(T before, T after) {
        return after != null && !Objects.equals(before, after);
    }

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
