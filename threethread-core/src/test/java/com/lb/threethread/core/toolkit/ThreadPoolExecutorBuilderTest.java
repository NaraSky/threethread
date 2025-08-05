package com.lb.threethread.core.toolkit;

import com.lb.threethread.core.executor.OneThreadExecutor;
import com.lb.threethread.core.executor.support.BlockingQueueTypeEnum;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ThreadPoolExecutorBuilderTest {

    @Test
    void testBuildWithAllParameters() {
        ThreadPoolExecutor executor = new ThreadPoolExecutorBuilder()
                .corePoolSize(2)
                .maximumPoolSize(4)
                .workQueueType(BlockingQueueTypeEnum.ARRAY_BLOCKING_QUEUE)
                .workQueueCapacity(10)
                .keepAliveTime(30L)
                .rejectedHandler(new ThreadPoolExecutor.AbortPolicy())
                .threadFactory(Executors.defaultThreadFactory())
                .allowCoreThreadTimeOut(true)
                .build();

        assertThat(executor.getCorePoolSize()).isEqualTo(2);
        assertThat(executor.getMaximumPoolSize()).isEqualTo(4);
        assertThat(executor.getKeepAliveTime(TimeUnit.SECONDS)).isEqualTo(30L);
        assertThat(executor.getQueue()).isInstanceOf(ArrayBlockingQueue.class);
        assertThat(executor.getRejectedExecutionHandler()).isInstanceOf(ThreadPoolExecutor.AbortPolicy.class);
        assertThat(executor.allowsCoreThreadTimeOut()).isTrue();
    }

    @Test
    void testDefaultKeepAliveTimeAndAllowTimeout() {
        ThreadPoolExecutor executor = new ThreadPoolExecutorBuilder()
                .corePoolSize(1)
                .maximumPoolSize(1)
                .keepAliveTime(60L)
                .workQueueType(BlockingQueueTypeEnum.LINKED_BLOCKING_QUEUE)
                .workQueueCapacity(100)
                .threadFactory(Executors.defaultThreadFactory())
                .rejectedHandler(new ThreadPoolExecutor.AbortPolicy())
                .build();

        assertThat(executor.getKeepAliveTime(TimeUnit.SECONDS)).isEqualTo(60L); // 默认值
        assertThat(executor.allowsCoreThreadTimeOut()).isFalse();               // 默认值
    }

    @Test
    void testQueueCapacityIsRespected() {
        ThreadPoolExecutor executor = new ThreadPoolExecutorBuilder()
                .corePoolSize(1)
                .maximumPoolSize(1)
                .keepAliveTime(9999L)
                .workQueueType(BlockingQueueTypeEnum.SYNCHRONOUS_QUEUE)
                .threadFactory(Executors.defaultThreadFactory())
                .allowCoreThreadTimeOut(true)
                .build();

        assertThat(executor.getQueue()).isInstanceOf(SynchronousQueue.class);
    }

    @Test
    void testRejectsMissingQueueType() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> {
            new ThreadPoolExecutorBuilder()
                    .corePoolSize(1)
                    .maximumPoolSize(1)
                    .workQueueType(null)
                    .keepAliveTime(9999L)
                    .workQueueCapacity(100)
                    .threadFactory(Executors.defaultThreadFactory())
                    .allowCoreThreadTimeOut(true)
                    .build();
        });

        assertThat(ex.getMessage()).contains("\"this.workQueueType\" is null");
    }

    @Test
    void testDynamicThreadPoolFlag() {
        ThreadPoolExecutorBuilder builder = new ThreadPoolExecutorBuilder().dynamicPool();
        assertThat(builder.isDynamicPool()).isEqualTo(Boolean.TRUE);
    }

    @Test
    void testDynamicThreadPoolId() {
        ThreadPoolExecutorBuilder builder = new ThreadPoolExecutorBuilder()
                .threadPoolId("test-thread-id");
        assertNotNull(builder.getThreadPoolId());
    }

    @Test
    void testDynamicThreadPool() {
        ThreadPoolExecutor executor = ThreadPoolExecutorBuilder.builder()
                .threadPoolId("test-thread-id")
                .corePoolSize(1)
                .maximumPoolSize(1)
                .keepAliveTime(60L)
                .workQueueType(BlockingQueueTypeEnum.LINKED_BLOCKING_QUEUE)
                .workQueueCapacity(100)
                .threadFactory("one-thread_")
                .rejectedHandler(new ThreadPoolExecutor.AbortPolicy())
                .dynamicPool()
                .build();
        assertNotNull(((OneThreadExecutor) executor).getThreadPoolId());
    }
}
