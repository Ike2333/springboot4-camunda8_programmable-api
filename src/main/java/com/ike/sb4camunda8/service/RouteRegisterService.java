package com.ike.sb4camunda8.service;

import com.ike.sb4camunda8.config.DynamicRouteRegistry;
import com.ike.sb4camunda8.dto.RoutesDto;
import io.camunda.client.CamundaClient;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * 注册流程:
 * 1. 写入DB
 * 2. 发布注册事件
 * 3. 所有节点reload routes
 *
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 16/3/2026
 */
@Service
public class RouteRegisterService {
    private final CamundaClient camundaClient;
    private final DynamicRouteRegistry registry;
    private final WorkflowService workflowService;

    public RouteRegisterService(CamundaClient camundaClient, DynamicRouteRegistry registry, WorkflowService workflowService) {
        this.camundaClient = camundaClient;
        this.registry = registry;
        this.workflowService = workflowService;
    }

    public void register(RoutesDto dto) {
        // 向camunda部署工作流, 并在springboot应用中创建一个可以调用该工作流的自定义路由
        var deploy = camundaClient
                .newDeployResourceCommand()
                .addResourceBytes(dto.bpmnXml().getBytes(), dto.name() + ".bpmn")
                .send()
                .join();
        var processId = deploy
                .getProcesses()
                .getFirst()
                .getBpmnProcessId();

        registry.register(
                dto.method().name(),
                dto.path(),
                r -> workflowService.startWorkflow(r, processId)
        );
    }

    public void cancel(RoutesDto dto) {
        registry.cancel(
                dto.method().name(),
                dto.path(),
                r -> workflowService.cancelWorkflow(dto.id())
        );
    }
}
