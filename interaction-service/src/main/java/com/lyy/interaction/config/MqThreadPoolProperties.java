package com.lyy.interaction.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "thread-pool.mq")
public class MqThreadPoolProperties {
    private int corePoolSize = 10;
    private int maxPoolSize = 20;
    private int queueCapacity = 100;
    private int keepAliveSeconds = 60;
}
