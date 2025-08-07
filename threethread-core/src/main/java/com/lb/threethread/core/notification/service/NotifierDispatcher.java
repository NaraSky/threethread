package com.lb.threethread.core.notification.service;

import com.lb.threethread.core.config.BootstrapConfigProperties;
import com.lb.threethread.core.notification.dto.ThreadPoolConfigChangeDTO;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class NotifierDispatcher implements NotifierService {

    private static final Map<String, NotifierService> NOTIFIER_SERVICE_MAP = new HashMap<>();

    static {
        NOTIFIER_SERVICE_MAP.put("DING", new DingTalkMessageService());
    }

    @Override
    public void sendChangeMessage(ThreadPoolConfigChangeDTO configChange) {
        Optional<NotifierService> notifierService = Optional.ofNullable(BootstrapConfigProperties.getInstance().getNotifyPlatforms())
                .map(BootstrapConfigProperties.NotifyPlatformsConfig::getPlatform)
                .map(each -> NOTIFIER_SERVICE_MAP.get(each));
        if (notifierService.isPresent()) {
            notifierService.get().sendChangeMessage(configChange);
        }
    }
}
