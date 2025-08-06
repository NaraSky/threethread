package com.lb.threethread.config.common.starter.configuration;

import com.lb.threethread.spring.base.configuration.BootstrapConfigProperties;
import com.lb.threethread.spring.base.configuration.OneThreadBaseConfiguration;
import com.lb.threethread.spring.base.enable.MarkerConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@ConditionalOnBean(MarkerConfiguration.Marker.class)
@Import(OneThreadBaseConfiguration.class)
@AutoConfigureAfter(OneThreadBaseConfiguration.class)
@ConditionalOnProperty(prefix = BootstrapConfigProperties.PREFIX, value = "enable", matchIfMissing = true, havingValue = "true")
public class CommonAutoConfiguration {
}
