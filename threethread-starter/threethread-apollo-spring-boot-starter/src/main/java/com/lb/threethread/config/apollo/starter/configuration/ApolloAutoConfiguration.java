package com.lb.threethread.config.apollo.starter.configuration;

import com.lb.threethread.config.apollo.starter.refresher.ApolloRefresherHandler;
import com.lb.threethread.spring.base.configuration.BootstrapConfigProperties;
import com.lb.threethread.spring.base.configuration.OneThreadBaseConfiguration;
import com.lb.threethread.spring.base.enable.MarkerConfiguration;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@Configurable
@ConditionalOnBean(MarkerConfiguration.Marker.class)
@Import(OneThreadBaseConfiguration.class)
@AutoConfigureAfter(OneThreadBaseConfiguration.class)
@ConditionalOnProperty(prefix = BootstrapConfigProperties.PREFIX, value = "enable", matchIfMissing = true, havingValue = "true")
public class ApolloAutoConfiguration {

    @Bean
    public ApolloRefresherHandler apolloRefresherHandler(BootstrapConfigProperties properties) {
        return new ApolloRefresherHandler(properties);
    }
}
