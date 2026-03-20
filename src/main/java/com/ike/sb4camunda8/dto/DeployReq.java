package com.ike.sb4camunda8.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeployReq(
        @NotBlank String sourceName,
        @NotNull SuppHttpMethod method,
        @NotBlank String path,
        @NotBlank String bpmnXml,
        @NotNull Boolean enable
) {
}
