package com.ike.sb4camunda8.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 13/3/2026
 */
@Component
public class DynamicRouteRegistry {

    private final Map<String, HandlerFunction<ServerResponse>> routes = new ConcurrentHashMap<>();

    public void register(String method, String path, HandlerFunction<ServerResponse> handler) {
        routes.put(method + ":" + path, handler);
    }

    public void cancel(String method, String path, HandlerFunction<ServerResponse> handler) {
        routes.remove(method + ":" + path, handler);
    }

    public Optional<HandlerFunction<ServerResponse>> find(String method, String path) {
        return Optional.ofNullable(routes.get(method + ":" + path));
    }
}