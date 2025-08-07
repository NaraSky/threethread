package com.lb.threethread.core.notification.service;

import com.lb.threethread.core.notification.dto.ThreadPoolConfigChangeDTO;

public interface NotifierService {

    /**
     * 发送线程池配置变更通知
     *
     * @param configChange 配置变更信息
     */
    void sendChangeMessage(ThreadPoolConfigChangeDTO configChange);
}
