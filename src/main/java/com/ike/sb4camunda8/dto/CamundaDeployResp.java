package com.ike.sb4camunda8.dto;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 22/3/2026
 */
public record CamundaDeployResp(
        String processId,
        Integer version,
        Long processDefinitionKey
) {
}
