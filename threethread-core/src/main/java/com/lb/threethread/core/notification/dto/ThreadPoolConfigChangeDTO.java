package com.lb.threethread.core.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreadPoolConfigChangeDTO {

    /**
     * 线程池唯一标识
     */
    private String threadPoolId;

    /**
     * 环境标识
     */
    private String active;

    /**
     * 应用名称
     */
    private String applicationName;

    /**
     * 应用节点唯一标识
     */
    private String identify;

    /**
     * 通知接收人
     */
    private String receives;

    /**
     * 阻塞队列类型
     */
    private String workQueue;

    /**
     * 配置项集合
     * Key: 配置项名称 (如corePoolSize)
     * Value: 变更前后值对
     */
    private Map<String, ChangePair<?>> changes;

    /**
     * 变更时间
     */
    private String updateTime;

    @Data
    @AllArgsConstructor
    public static class ChangePair<T> {
        private T before;
        private T after;
    }
}
