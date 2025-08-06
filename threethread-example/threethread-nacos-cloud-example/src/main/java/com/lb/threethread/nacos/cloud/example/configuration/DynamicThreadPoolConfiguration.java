package com.lb.threethread.nacos.cloud.example.configuration;

import com.lb.threethread.core.executor.support.BlockingQueueTypeEnum;
import com.lb.threethread.core.toolkit.ThreadPoolExecutorBuilder;
import com.lb.threethread.spring.base.DynamicThreadPool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class DynamicThreadPoolConfiguration {

    @Bean
    @DynamicThreadPool
    public ThreadPoolExecutor onethreadProducer() {
        return ThreadPoolExecutorBuilder.builder()
                .threadPoolId("onethread-producer")
                .corePoolSize(2)
                .maximumPoolSize(4)
                .keepAliveTime(9999L)
                .workQueueType(BlockingQueueTypeEnum.SYNCHRONOUS_QUEUE)
                .threadFactory("onethread-producer_")
                .rejectedHandler(new ThreadPoolExecutor.CallerRunsPolicy())
                .dynamicPool()
                .build();
    }

    @Bean
    @DynamicThreadPool
    public ThreadPoolExecutor onethreadConsumer() {
        return ThreadPoolExecutorBuilder.builder()
                .threadPoolId("onethread-consumer")
                .corePoolSize(4)
                .maximumPoolSize(6)
                .keepAliveTime(9999L)
                .workQueueType(BlockingQueueTypeEnum.SYNCHRONOUS_QUEUE)
                .threadFactory("onethread-consumer_")
                .rejectedHandler(new ThreadPoolExecutor.CallerRunsPolicy())
                .dynamicPool()
                .build();
    }
}
