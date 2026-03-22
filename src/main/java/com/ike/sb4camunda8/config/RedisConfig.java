package com.ike.sb4camunda8.config;

import com.ike.sb4camunda8.listener.RoutesEventListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 17/3/2026
 */
@Configuration
public class RedisConfig {
    private final ObjectMapper objectMapper;

    public RedisConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    // 注册redis pub/sub
    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory factory, RoutesEventListener listener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(listener, new ChannelTopic("route-event-update"));
        return container;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        var r = new StringRedisTemplate();
        r.setConnectionFactory(factory);
        StringRedisSerializer ks = new StringRedisSerializer();
        GenericJacksonJsonRedisSerializer vs = new GenericJacksonJsonRedisSerializer(objectMapper);
        r.setKeySerializer(ks);
        r.setHashKeySerializer(ks);
        r.setValueSerializer(vs);
        r.setHashValueSerializer(vs);
        return r;
    }

}
