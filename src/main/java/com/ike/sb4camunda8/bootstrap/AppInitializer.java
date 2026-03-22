package com.ike.sb4camunda8.bootstrap;

import com.ike.sb4camunda8.dto.CamundaDeployResp;
import com.ike.sb4camunda8.dto.DeployReq;
import com.ike.sb4camunda8.entity.Routes;
import com.ike.sb4camunda8.repository.RoutesRepository;
import com.ike.sb4camunda8.service.RouteRegisterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 16/3/2026
 */
@Component
public class AppInitializer {
    private static final Logger log = LoggerFactory.getLogger(AppInitializer.class);
    private final RoutesRepository routesRepository;
    private final RouteRegisterService routeRegisterService;

    public AppInitializer(RoutesRepository routesRepository, RouteRegisterService routeRegisterService) {
        this.routesRepository = routesRepository;
        this.routeRegisterService = routeRegisterService;
    }


    /**
     * 程序运行时读取数据库中的配置初始化路由
     */
    @EventListener(ApplicationReadyEvent.class)
    public void runAfterStartup() {
        List<Routes> allEnabled = routesRepository.findByActive(true);
        log.debug("################## 流程初始化开始 ##################");

        allEnabled.forEach(r -> {
            try {
                // 从数据库读取XML, 向camunda注册, 然后更新数据库中的processDefinitionKey和version
                Long processDefinitionKey = r.getProcessDefinitionKey();
                var optXml = routeRegisterService.findByProcessDefinitionKey(processDefinitionKey);
                if (StringUtils.hasText(optXml)) {
                    var req = new DeployReq(r.getName(), r.getMethod(), r.getPath(), optXml);
                    CamundaDeployResp resp = routeRegisterService.register(req);
                    Long newDefinitionKey = resp.processDefinitionKey();
                    Integer version = resp.version();
                    routesRepository.updateProcessDefinitionKeyAndVersionById(newDefinitionKey, version, r.getId());
                }
            } catch (Exception e) {
                log.error("流程 [name={}, method={}, path={}] 初始化失败", r.getName(), r.getMethod(), r.getPath(), e);
            }
        });
        log.debug("################## 流程初始化结束 ##################");
    }

}
