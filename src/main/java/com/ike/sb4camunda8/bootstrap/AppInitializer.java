package com.ike.sb4camunda8.bootstrap;

import com.ike.sb4camunda8.entity.Routes;
import com.ike.sb4camunda8.mappers.EntityDtoMapper;
import com.ike.sb4camunda8.repository.RoutesRepository;
import com.ike.sb4camunda8.service.RouteRegisterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 16/3/2026
 */
@Component
public class AppInitializer {
    private static final Logger log = LoggerFactory.getLogger(AppInitializer.class);
    private final RoutesRepository routesRepository;
    private final RouteRegisterService routeRegisterService;
    private final EntityDtoMapper entityDtoMapper;

    public AppInitializer(RoutesRepository routesRepository, RouteRegisterService routeRegisterService, EntityDtoMapper entityDtoMapper) {
        this.routesRepository = routesRepository;
        this.routeRegisterService = routeRegisterService;
        this.entityDtoMapper = entityDtoMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runAfterStartup() {
        List<Routes> allEnabled = routesRepository.findByEnable(true);
        log.debug("################## 流程初始化开始 ##################");
        // 基于数据库初始化流程, 极端情况下可能存在节点状态不一致, 后续使用mq处理
        entityDtoMapper.convertRoutesListToDtoList(allEnabled)
                .forEach(r -> {
                    try {
                        routeRegisterService.register(r);
                    } catch (Exception e) {
                        log.error("流程 [name={}, method={}, path={}] 初始化失败", r.name(), r.method(), r.path(), e);
                    }
                });
        log.debug("################## 流程初始化结束 ##################");
    }

}
