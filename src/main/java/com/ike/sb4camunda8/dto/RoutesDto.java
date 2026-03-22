package com.ike.sb4camunda8.dto;

import java.time.Instant;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 16/3/2026
 */
public record RoutesDto(
        Long id,
        String name,
        SuppHttpMethod method,
        String path,
        String bpmnProcessId,
        Long processDefinitionKey,
        Integer version,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
