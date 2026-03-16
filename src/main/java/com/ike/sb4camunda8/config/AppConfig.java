package com.ike.sb4camunda8.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 14/3/2026
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class AppConfig {

    @Bean
    public RouterFunction<ServerResponse> dynamicRouter(DynamicRouteRegistry registry) {
        return request -> registry.find(request.method().name(), request.path());
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
