package com.ike.sb4camunda8.service;

import com.ike.sb4camunda8.config.DynamicRouteRegistry;
import com.ike.sb4camunda8.dto.CamundaDeployResp;
import com.ike.sb4camunda8.dto.DeployReq;
import com.ike.sb4camunda8.dto.RoutesDto;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.Process;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    public CamundaDeployResp register(DeployReq dto) {
        // 向camunda部署工作流, 并在springboot应用中创建一个可以调用该工作流的自定义路由
        var deploy = camundaClient
                .newDeployResourceCommand()
                .addResourceBytes(dto.bpmnXml().getBytes(), dto.name() + ".bpmn")
                .send()
                .join();

        Process process = deploy.getProcesses().getFirst();
        var processId = process.getBpmnProcessId();
        int version = process.getVersion();
        long processDefinitionKey = process.getProcessDefinitionKey();

        registry.register(
                dto.method().name(),
                dto.path(),
                r -> workflowService.startWorkflow(r, processId)
        );

        return new CamundaDeployResp(processId, version, processDefinitionKey);
    }

    public String findByProcessDefinitionKey(Long processDefinitionKey){
        String xml = camundaClient.newProcessDefinitionGetXmlRequest(processDefinitionKey).send().join();
        return StringUtils.hasText(xml) ? xml : null;
    }

    public void cancel(RoutesDto dto) {
        registry.cancel(
                dto.method().name(),
                dto.path(),
                r -> workflowService.cancelWorkflow(dto.id())
        );
    }
}
