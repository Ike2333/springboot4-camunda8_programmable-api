package com.ike.sb4camunda8.service;

import io.camunda.client.CamundaClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 13/3/2026
 */
@Service
public class WorkflowServiceImpl implements WorkflowService {
    private final CamundaClient camundaClient;

    public WorkflowServiceImpl(CamundaClient camundaClient) {
        this.camundaClient = camundaClient;
    }

    @Override
    public ServerResponse startWorkflow(ServerRequest request, String processId) throws Exception {
        Map<String, Object> vars = extractVariables(request);

        var instance = camundaClient
                .newCreateInstanceCommand()
                .bpmnProcessId(processId)
                .latestVersion()
                .variables(vars)
                .withResult()
                .send()
                .join();

        return ServerResponse.ok().body(instance.getVariablesAsMap());
    }

    private Map<String, Object> extractVariables(ServerRequest request) {
        Map<String, Object> vars = new HashMap<>();

        // path variables
        vars.putAll(request.pathVariables());

        // query params
        vars.putAll(request.params().toSingleValueMap());

        // body (if exists)
        try {
            Map<String, Object> body =
                    request.body(new ParameterizedTypeReference<>() {
                    });
            if (!body.isEmpty()) {
                vars.putAll(body);
            }
        } catch (Exception ignore) {
            // todo: 暂时忽略body解析错误
        }

        return vars;
    }
}
