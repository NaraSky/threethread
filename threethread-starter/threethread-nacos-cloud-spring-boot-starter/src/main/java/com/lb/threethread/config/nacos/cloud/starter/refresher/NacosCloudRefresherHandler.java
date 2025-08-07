package com.lb.threethread.config.nacos.cloud.starter.refresher;

import com.alibaba.cloud.nacos.NacosConfigProperties;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.lb.threethread.config.common.starter.refresher.AbstractDynamicThreadPoolRefresher;
import com.lb.threethread.core.executor.support.BlockingQueueTypeEnum;
import com.lb.threethread.core.notification.service.DingTalkMessageService;
import com.lb.threethread.core.notification.service.NotifierDispatcher;
import com.lb.threethread.core.toolkit.ThreadPoolExecutorBuilder;
import com.lb.threethread.spring.base.configuration.BootstrapConfigProperties;
import com.lb.threethread.spring.base.support.ApplicationContextHolder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Nacos云配置线程池刷新处理器
 * <p>
 * 主要功能：
 * 1. 监听Nacos配置中心的配置变化
 * 2. 当配置发生变化时，动态更新线程池参数
 * 3. 实现配置热更新，无需重启应用即可调整线程池配置
 * </p>
 * <p>
 * 设计特点：
 * 1. 继承自AbstractDynamicThreadPoolRefresher，实现通用的线程池刷新逻辑
 * 2. 使用Nacos原生API监听配置变化
 * 3. 为配置监听创建独立的线程池，避免与业务线程池冲突
 * </p>
 */
@Slf4j(topic = "OneThreadConfigRefresher")
public class NacosCloudRefresherHandler extends AbstractDynamicThreadPoolRefresher {

    /**
     * Nacos配置服务实例，用于与Nacos配置中心进行交互
     */
    private ConfigService configService;

    public NacosCloudRefresherHandler(BootstrapConfigProperties properties, NotifierDispatcher notifierDispatcher) {
        super(properties, notifierDispatcher);
        configService = ApplicationContextHolder.getBean(NacosConfigProperties.class).configServiceInstance();
    }

    /**
     * 注册Nacos配置监听器
     * <p>
     * 实现原理：
     * 1. 从配置中获取Nacos的dataId和group
     * 2. 注册监听器监听配置变化
     * 3. 为监听器创建专用的线程池执行器
     * 4. 当配置变化时，调用refreshThreadPoolProperties方法刷新线程池配置
     * </p>
     * 
     * @throws NacosException Nacos配置服务异常
     */
    public void registerListener() throws NacosException {
        // 获取Nacos配置信息
        BootstrapConfigProperties.NacosConfig nacosConfig = properties.getNacos();
        
        // 添加配置监听器
        configService.addListener(
                nacosConfig.getDataId(),           // 配置的dataId
                nacosConfig.getGroup(),            // 配置的group
                new Listener() {

                    /**
                     * 获取监听器执行器
                     * <p>
                     * 为配置监听器创建专用的线程池，避免与业务线程池冲突
                     * 使用同步队列，核心线程数和最大线程数都为1，确保配置更新的串行执行
                     * </p>
                     * 
                     * @return 用于执行配置变更回调的线程池执行器
                     */
                    @Override
                    public Executor getExecutor() {
                        return ThreadPoolExecutorBuilder.builder()
                                .corePoolSize(1)                                    // 核心线程数为1
                                .maximumPoolSize(1)                                 // 最大线程数为1
                                .keepAliveTime(9999L)                               // 线程空闲时间
                                .workQueueType(BlockingQueueTypeEnum.SYNCHRONOUS_QUEUE) // 使用同步队列
                                .threadFactory("clod-nacos-refresher-thread_")      // 线程工厂，指定线程名称前缀
                                .rejectedHandler(new ThreadPoolExecutor.CallerRunsPolicy()) // 拒绝策略为调用者运行策略
                                .build();
                    }

                    /**
                     * 接收配置信息变更回调
                     * <p>
                     * 当Nacos配置发生变化时，会触发此方法
                     * 调用父类的refreshThreadPoolProperties方法处理配置更新
                     * </p>
                     * 
                     * @param configInfo 变更后的配置信息内容
                     */
                    @Override
                    public void receiveConfigInfo(String configInfo) {
                        refreshThreadPoolProperties(configInfo);
                    }
                });

        log.info("Dynamic thread pool refresher, add nacos cloud listener success. data-id: {}, group: {}", nacosConfig.getDataId(), nacosConfig.getGroup());
    }
}