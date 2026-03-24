package com.ike.sb4camunda8.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 23/3/2026
 */
public record ChangeVersionReq(
        @Schema(description = "路由的ID")
        Long id,
        @Schema(description = "流程版本号")
        Integer version
) {
}
