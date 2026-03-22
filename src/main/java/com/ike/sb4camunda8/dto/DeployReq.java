package com.ike.sb4camunda8.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeployReq(
        // 流程名称
        @NotBlank String name,
        // 请求方法: post/get/put/delete
        @NotNull SuppHttpMethod method,
        // 请求路径
        @NotBlank String path,
        // BPMN XML
        @NotBlank String bpmnXml
) {
}
