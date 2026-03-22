package com.ike.sb4camunda8.listener;

import com.ike.sb4camunda8.dto.DeployReq;
import com.ike.sb4camunda8.service.RouteRegisterService;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 17/3/2026
 */
@Component
public class RoutesEventListener implements MessageListener {
    private static final Logger log = LoggerFactory.getLogger(RoutesEventListener.class);

    private final ObjectMapper objectMapper;
    private final RouteRegisterService routeRegisterService;

    public RoutesEventListener(ObjectMapper objectMapper, RouteRegisterService routeRegisterService) {
        this.objectMapper = objectMapper;
        this.routeRegisterService = routeRegisterService;
    }


    /**
     * 从redis中接收广播的消息用于注册新路由, 目前的缺点是首个节点会被执行两次
     * TODO: 暂时无影响, 后续可能需要优化
     *
     * @param message message must not be {@literal null}.
     * @param pattern pattern matching the channel (if specified) - can be {@literal null}.
     */
    @Override
    public void onMessage(Message message, byte @Nullable [] pattern) {
        String body = new String(message.getBody());
        log.debug("接收到的JSON: {}", body);
        DeployReq req = objectMapper.readValue(body, DeployReq.class);
        routeRegisterService.register(req);
    }
}
