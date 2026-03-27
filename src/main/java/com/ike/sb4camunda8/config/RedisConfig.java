package com.ike.sb4camunda8.config;

import com.ike.sb4camunda8.listener.RoutesEventListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 17/3/2026
 */
@Configuration
public class RedisConfig {


    // 注册redis pub/sub
    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory factory, RoutesEventListener listener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(listener, new ChannelTopic("route-event-update"));
        return container;
    }

}
