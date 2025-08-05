package com.lb.threethread.core.executor.support;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 拒绝策略类型枚举
 */
public enum RejectedPolicyTypeEnum {

    CALLER_RUNS_POLICY("CallerRunsPolicy", new ThreadPoolExecutor.CallerRunsPolicy()),

    ABORT_POLICY("AbortPolicy", new ThreadPoolExecutor.AbortPolicy()),

    DISCARD_POLICY("DiscardPolicy", new ThreadPoolExecutor.DiscardPolicy()),

    DISCARD_OLDEST_POLICY("DiscardOldestPolicy", new ThreadPoolExecutor.DiscardOldestPolicy());

    @Getter
    private String name;

    @Getter
    private RejectedExecutionHandler rejectedHandler;

    RejectedPolicyTypeEnum(String rejectedPolicyName, RejectedExecutionHandler rejectedHandler) {
        this.name = rejectedPolicyName;
        this.rejectedHandler = rejectedHandler;
    }

    private static final Map<String, RejectedPolicyTypeEnum> NAME_TO_ENUM_MAP;

    static {
        final RejectedPolicyTypeEnum[] values = RejectedPolicyTypeEnum.values();
        NAME_TO_ENUM_MAP = new HashMap<>(values.length);
        for (RejectedPolicyTypeEnum value : values) {
            NAME_TO_ENUM_MAP.put(value.name, value);
        }
    }
    
    public static RejectedExecutionHandler createPolicy(String rejectedPolicyName) {
        RejectedPolicyTypeEnum rejectedPolicyTypeEnum = NAME_TO_ENUM_MAP.get(rejectedPolicyName);
        if (rejectedPolicyTypeEnum != null) {
            return rejectedPolicyTypeEnum.rejectedHandler;
        }

        throw new IllegalArgumentException("No matching type of rejected execution was found: " + rejectedPolicyName);
    }
}
